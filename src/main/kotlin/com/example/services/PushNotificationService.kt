package com.example.services

import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.TimeUnit

@Serializable
data class Message(
    val message: MessageContent
)

@Serializable
data class MessageContent(
    val token: String,
    val notification: Notification,
    val data: Data
)

@Serializable
data class Notification(
    val title: String,
    val body: String
)

@Serializable
data class Data(
    val showAlert: String,
    val customKey1: String,
    val customKey2: String
)

object PushNotificationService {
    private val logger = LoggerFactory.getLogger(PushNotificationService::class.java)
    private var cachedToken: String? = null
    private var tokenExpiry: Instant? = null
    private val jwtTokenProvider = JwtTokenProvider("/etc/jwt/pushtoapp-c3caa-dd8457b08124.json")
    private val httpClient = HttpClient()

    // Simulierter Token-Abruf 
    private fun fetchBearerToken(): String {
        logger.info("Fetching new bearer token...")
        return runBlocking {
            jwtTokenProvider.getAccessToken()
        }
    }

    private fun getBearerToken(): String {
        val currentTime = Instant.now()
        return if (cachedToken == null || tokenExpiry == null || currentTime.isAfter(tokenExpiry)) {
            val newToken = fetchBearerToken()
            cachedToken = newToken
            tokenExpiry = currentTime.plus(1, TimeUnit.HOURS.toChronoUnit()) // Token für eine Stunde cachen
            logger.info("New token cached")
            newToken
        } else {
            logger.info("Using cached token")
            cachedToken!!
        }
    }

    suspend fun sendPushNotification(deviceId: String, topic: String, body: String, k1: String, k2: String, showAlert: Boolean): Boolean {
        val token = getBearerToken()
        logger.info("Sending push notification")

        val messageContent = Message(
            message = MessageContent(
                token = deviceId,
                notification = Notification(
                    title = topic,
                    body = body
                ),
                data = Data(
                    showAlert = if ( showAlert ) "true" else "false",
                    customKey1 = k1,
                    customKey2= k2
                )
            )
        )

        val messageContentString = Json.encodeToString(messageContent)
	logger.info("message:$messageContentString")
        return try {
            val response: HttpResponse = httpClient.post("https://fcm.googleapis.com/v1/projects/pushtoapp-c3caa/messages:send") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(messageContentString)
            }
            val responseBody = response.bodyAsText()
            logger.info("Response: $responseBody")
            response.status.isSuccess()
        } catch (e: Exception) {
            logger.error("Error sending push notification: ${e.message}", e)
            false
        }
    }
}
