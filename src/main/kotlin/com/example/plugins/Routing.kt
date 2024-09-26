package com.example.plugins

import com.example.models.DeviceData
import com.example.services.PushNotificationService
import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import kotlinx.html.* // HTML DSL für dynamisches HTML
import kotlinx.coroutines.runBlocking

fun Application.configureRouting() {
    routing {
        post("/appstart") {
            val deviceData = call.receive<DeviceData>()
            Database.saveDeviceData(deviceData)
            call.respond(deviceData)
        }

        get("/data-overview") {
            val deviceDataList: List<DeviceData> = Database.getAllDeviceData()

            // Generiere dynamisches HTML mit eingebundenem Stylesheet
            call.respondHtml(HttpStatusCode.OK) {
                head {
                    title("Device Data Overview")
                    link(rel = "stylesheet", href = "/static/styles.css", type = "text/css")
                }
                body {
                    h1 { +"Device Data Overview" }

                    // Overlay-Formular
                    div {
                        id = "overlay"
                        style = "display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background-color: rgba(0,0,0,0.5);"
                        div {
                            style = "background-color: white; padding: 20px; margin: 100px auto; width: 300px;"
                            form(action = "/send-push", method = FormMethod.post) {
                                hiddenInput(name = "device_id") { value = "" }  // Wird dynamisch gesetzt
                                textInput(name = "push_id") { placeholder = "Push ID" }  // Befüllt durch JavaScript
                                textInput(name = "title") { placeholder = "Title" }
                                textInput(name = "topic") { placeholder = "Topic" }
                                textInput(name = "key1") { placeholder = "Key1" }
                                textInput(name = "key2") { placeholder = "Key2" }
                                button(type = ButtonType.submit) { +"Senden" }
                            }
                            button {
                                type = ButtonType.button
                                onClick = "document.getElementById('overlay').style.display='none';"
                                +"Abbrechen"
                            }
                        }
                    }

                    table {
                        thead {
                            tr {
                                th { +"Device ID" }
                                th { +"Push ID" }
                                th { +"Login ID" }
                                th { +"Last Login" }
                                th { +"Action" }
                                th { +"Löschen" }
                            }
                        }
                        tbody {
                            deviceDataList.forEach { deviceData ->
                                tr {
                                    td { +deviceData.device_id }
                                    td { +deviceData.push_id }
                                    td { +deviceData.login_id }
                                    td { +deviceData.last_login.toString() }
                                    td {
                                        button {
                                            type = ButtonType.button
                                            onClick = """
                                                document.getElementById('overlay').style.display='block';
                                                document.querySelector('input[name="device_id"]').value='${deviceData.device_id}';
                                                document.querySelector('input[name="push_id"]').value='${deviceData.push_id}';
                                            """
                                            +"Send Push"
                                        }
                                    }
                                    td {
                                        form(action = "/delete-device", method = FormMethod.post) {
                                            hiddenInput(name = "device_id") { value = deviceData.device_id }
                                            button(type = ButtonType.submit) { +"Delete" }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Empfangen des Formulars und Senden der Push-Benachrichtigung
        post("/send-push") {
            val parameters = call.receiveParameters()  // Der Request-Body wird nur hier konsumiert
            val pushId = parameters["push_id"] ?: return@post call.respondText("Missing push_id", status = HttpStatusCode.BadRequest)
            val title = parameters["title"] ?: return@post call.respondText("Missing title", status = HttpStatusCode.BadRequest)
            val topic = parameters["topic"] ?: return@post call.respondText("Missing topic", status = HttpStatusCode.BadRequest)
            val key1 = parameters["key1"] ?: return@post call.respondText("Missing key1", status = HttpStatusCode.BadRequest)
            val key2 = parameters["key2"] ?: return@post call.respondText("Missing key2", status = HttpStatusCode.BadRequest)


            // Logik zum Abrufen des Bearer-Tokens und Senden der Push-Benachrichtigung
            val success = PushNotificationService.sendPushNotification(pushId, title, topic, key1, key2, true )

            if (success) {
                call.respondText("Push notification sent successfully!", status = HttpStatusCode.OK)
            } else {
                call.respondText("Failed to send push notification.", status = HttpStatusCode.InternalServerError)
            }
        }
        post("/delete-device") {
          val deviceId = call.receiveParameters()["device_id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
          Database.deleteDeviceData(deviceId)
          call.respondRedirect("/data-overview")
       }
    }
}
