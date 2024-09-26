package com.example.services

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters

import io.ktor.client.plugins.contentnegotiation.*

import io.ktor.client.engine.cio.*
import kotlinx.serialization.Serializable
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

import java.time.Instant
import java.util.Base64

import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec


class JwtTokenProvider(private val serviceAccountFilePath: String) {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    private val googleOAuth2TokenUrl = "https://oauth2.googleapis.com/token"
    private val jwtGrantType = "urn:ietf:params:oauth:grant-type:jwt-bearer"
    private val firebaseMessagingScope = "https://www.googleapis.com/auth/firebase.messaging"
    private val tokenLifetime = 3600L // 1 Stunde

    @Serializable
    data class ServiceAccountInfo(
        val client_email: String,
        val private_key: String
    )

    @Serializable
    data class TokenResponse(
        val access_token: String,
        val expires_in: Int
    )

    private fun loadServiceAccountInfo(): ServiceAccountInfo {
        val fileContent = java.io.File(serviceAccountFilePath).readText()
        return Json {
          ignoreUnknownKeys = true // Ignoriere unbekannte JSON-Schlüssel
        }.decodeFromString(ServiceAccountInfo.serializer(), fileContent)
    }

    private fun createJwt(serviceAccountInfo: ServiceAccountInfo): String {
        val currentTime = Instant.now().epochSecond
        val header = """{"alg":"RS256","typ":"JWT"}"""
        val payload = """{
            "iss":"${serviceAccountInfo.client_email}",
            "scope":"$firebaseMessagingScope",
            "aud":"$googleOAuth2TokenUrl",
            "iat":$currentTime,
            "exp":${currentTime + tokenLifetime}
        }"""

        val headerBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(header.toByteArray())
        val payloadBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        val unsignedJwt = "$headerBase64.$payloadBase64"

        // Signiere das JWT mit dem privaten Schlüssel
        val signature = signWithPrivateKey(unsignedJwt, serviceAccountInfo.private_key)
        val jwt = "$unsignedJwt.$signature"

        return jwt
    }

    // Methode, um den privaten Schlüssel zu extrahieren und die JWT-Signatur zu erstellen
    fun signWithPrivateKey(data: String, privateKeyPem: String): String {
        // Entferne den Header und Footer des PEM-Private-Key-Formats
        val privateKeyPEM = privateKeyPem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")

        // Dekodiere den Base64-codierten Private Key
        val encoded = Base64.getDecoder().decode(privateKeyPEM)

        // Konvertiere den dekodierten Schlüssel in ein PrivateKey-Objekt
        val keySpec = PKCS8EncodedKeySpec(encoded)
        val keyFactory = KeyFactory.getInstance("RSA")
        val privateKey: PrivateKey = keyFactory.generatePrivate(keySpec)

        // Erstelle die Signatur mit SHA256 und RSA
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(privateKey)
        signature.update(data.toByteArray())

        // Signiere die Daten und encodiere das Ergebnis in Base64
        val signedData = signature.sign()
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signedData)
    }

    suspend fun requestAccessToken(jwtToken: String): String {
        val response: HttpResponse = client.submitForm(
            url = googleOAuth2TokenUrl,
            formParameters = Parameters.build {
                append("grant_type", jwtGrantType)
                append("assertion", jwtToken)
            }
        )

        return if (response.status == HttpStatusCode.OK) {
            val tokenResponse: TokenResponse = response.body()
            tokenResponse.access_token
        } else {
            throw Exception("Failed to get access token: ${response.status}")
        }
    }

    suspend fun getAccessToken(): String {
        val serviceAccountInfo = loadServiceAccountInfo()
        val jwt = createJwt(serviceAccountInfo)
        return requestAccessToken(jwt)
    }
}
