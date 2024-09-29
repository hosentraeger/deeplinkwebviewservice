package com.example.plugins

import com.example.models.DeviceData
import com.example.services.PushNotificationService

import io.ktor.http.*
import io.ktor.server.application.*
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
                    script {
                        unsafe {
                            raw("""
                            function checkSendButton() {
                                const useTitleAndText = document.querySelector('input[name="useTitleAndText"]').checked;
                                const useAdditionalData = document.querySelector('input[name="useAdditionalData"]').checked;
                                const sendButton = document.querySelector('button[type="submit"]');

                                if (useTitleAndText || useAdditionalData) {
                                    sendButton.disabled = false;
                                } else {
                                    sendButton.disabled = true;
                                }
                            }

                            function randomizeMessage() {
                                console.log("randomizeMessage called");
                                const titleTopicPairs = {
                                  "Wichtige Aktualisierung" : "Es gibt eine neue Version der App. Aktualisiere jetzt, um die neuesten Funktionen zu nutzen.",
                                  "Dein Profil wurde aktualisiert" : "Wir haben dein Profil erfolgreich aktualisiert. Überprüfe die Änderungen in der App.",
                                  "Neuer Kommentar" : "Du hast einen neuen Kommentar zu deinem Beitrag erhalten. Klicke hier, um ihn zu lesen.",
                                  "Angebot des Tages" : "Nur heute: Spare 20 % auf deine nächste Bestellung! Nutze den Code: SPAREN20.",
                                  "Erinnerung" : "Vergiss nicht, deinen wöchentlichen Bericht zu überprüfen.",
                                  "Nachricht von [App-Name]" : "Du hast eine neue Nachricht erhalten. Öffne die App, um sie zu lesen.",
                                  "Wartungsarbeiten" : "Die App wird heute von 02:00 bis 04:00 Uhr aufgrund geplanter Wartungsarbeiten nicht verfügbar sein.",
                                  "Sicherheitswarnung" : "Ungewöhnliche Aktivitäten wurden in deinem Konto festgestellt. Ändere dein Passwort so schnell wie möglich.",
                                  "Ziel erreicht!" : "Glückwunsch! Du hast dein tägliches Ziel erreicht. Weiter so!",
                                  "Neues Feature verfügbar" : "Entdecke unser neuestes Feature! Jetzt in der App verfügbar.",
                                  "Event-Erinnerung" : "Dein Event startet in 30 Minuten. Sei bereit!",
                                  "Freundschaftsanfrage" : "Du hast eine neue Freundschaftsanfrage. Klicke hier, um sie zu akzeptieren.",
                                  "Verpasste Nachricht" : "Du hast eine ungelesene Nachricht. Öffne die App, um sie zu lesen.",
                                  "Täglicher Tipp" : "Erhöhe deine Produktivität mit unserem neuesten Tipp!",
                                  "System-Update erforderlich" : "Bitte installiere das neueste Update für eine reibungslose Nutzung.",
                                  "Dein Konto ist sicher" : "Wir haben einen Sicherheits-Check durchgeführt. Alles ist in Ordnung.",
                                  "Geschenk erhalten!" : "Du hast ein Geschenk erhalten. Öffne die App, um es zu sehen.",
                                  "Top Nachrichten" : "Bleib informiert. Hier sind die wichtigsten Nachrichten des Tages.",
                                  "Wochenendplan" : "Schau dir deine Aktivitäten für das Wochenende an.",
                                  "Neuer Highscore!" : "Jemand hat deinen Highscore übertroffen. Schaffst du es zurück an die Spitze?"
                                }
                                const keys = Object.keys(titleTopicPairs);
                                const randomKey = keys[Math.floor(Math.random() * keys.length)];
                                document.querySelector('input[name="title"]').value = randomKey;
                                document.querySelector('input[name="topic"]').value = titleTopicPairs[randomKey];
                            }
                            function sendPushNotification() {
                                const form = document.querySelector("#push-form");
                                const formData = new FormData(form);

                                fetch("/send-push", {
                                    method: "POST",
                                    body: formData
                                })
                                .then(response => response.json())
                                .then(data => {
                                    if (data.status === "success") {
                                        document.getElementById("overlay-message").innerText = data.message;
                                        document.getElementById("status-overlay").style.display = "block";
                                    } else {
                                        alert("Error: " + data.message);
                                    }
                                })
                                .catch(error => {
                                    console.error("Error:", error);
                                });

                                return false; // Prevent form from submitting the normal way
                            }
                            """.trimIndent())
                        }
                    }
                }
                body {
                    h1 { +"Device Data Overview" }

                    // Overlay-Formular
                    div {
                        id = "overlay"
                        style = "display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background-color: rgba(0,0,0,0.5);"
                        div {
                            style = "background-color: white; padding: 20px; margin: 100px auto; width: 300px;"
                            form {
                                attributes["id"] = "push-form"
                                attributes["onsubmit"] = "return sendPushNotification();"
                                hiddenInput(name = "device_id") { value = "" }  // Wird dynamisch gesetzt
                                textInput(name = "push_id") { placeholder = "Push ID" }  // Befüllt durch JavaScript
                                checkBoxInput(name = "useTitleAndText") {
                                    onClick = """
                                        const title = document.querySelector('input[name="title"]');
                                        const topic = document.querySelector('input[name="topic"]');
                                        const sendButton = document.querySelector('button[type="submit"]');
                                        title.disabled = !this.checked;
                                        topic.disabled = !this.checked;
                                        checkSendButton();
                                    """
                                }
                                textInput(name = "title") { placeholder = "Title"; disabled = true }
                                textInput(name = "topic") { placeholder = "Topic"; disabled = true }

                                checkBoxInput(name = "useAdditionalData") {
                                    onClick = """
                                        const key1 = document.querySelector('input[name="key1"]');
                                        const key2 = document.querySelector('input[name="key2"]');
                                        const sendButton = document.querySelector('button[type="submit"]');
                                        key1.disabled = !this.checked;
                                        key2.disabled = !this.checked;
                                        checkSendButton();
                                    """
                                }
                                textInput(name = "key1" ) { placeholder = "key1"; disabled = true }
                                textInput(name = "key2" ) { placeholder = "key2"; disabled = true }
                                // Senden-Button, der zunächst inaktiv ist
                                button(type = ButtonType.submit) {
                                  style = "margin: 10px;" // Add some spacing around the button
                                    disabled = true
                                    +"Senden"
                                }
                                button {
                                    style = "margin: 10px;" // Add some spacing around the button
                                    type = ButtonType.button
                                    onClick = "document.getElementById('overlay').style.display='none';"
                                    +"Abbrechen"
                                }
                                button(type = ButtonType.button) {
                                    style = "margin: 10px;" // Add some spacing around the button
                                    onClick = """
                                        document.querySelector('input[name="title"]').value = '';
                                        document.querySelector('input[name="topic"]').value = '';
                                        document.querySelector('textarea[name="key1"]').value = '';
                                        document.querySelector('textarea[name="key2"]').value = '';
                                        document.querySelector('input[name="useTitleAndText"]').checked = false;
                                        document.querySelector('input[name="useAdditionalData"]').checked = false;
                                        checkSendButton();
                                    """
                                    +"Inhalte löschen"
                                }
                                button(type = ButtonType.button) {
                                    style = "margin: 10px;"
                                    onClick = """
                                        console.log('Zufällige Ansprache button clicked');
                                        randomizeMessage();
                                    """
                                    +"Zufällige Ansprache"
                                }
                            }
                        }
                    }
                    div {
                        id = "status-overlay"
                        style = "display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background-color: rgba(0,0,0,0.5);"
                        div {
                            style = "background-color: white; padding: 20px; margin: 100px auto; width: 300px; text-align: center;"
                            p {
                                id = "overlay-message"
                                +""
                            }
                            button {
                                type = ButtonType.button
                                onClick = "document.getElementById('status-overlay').style.display='none';"
                                +"Close"
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
                    footer {
                        a(href = "/static/app-debug.apk") {
                            +"Download APK"
                        }
                    }
                }
            }
        }
        // Empfangen des Formulars und Senden der Push-Benachrichtigung
        post("/send-push") {
            val parameters = call.receiveParameters()
            
            val useTitleAndText = parameters["useTitleAndText"] != null
            val useAdditionalData = parameters["useAdditionalData"] != null
            val pushId = parameters["push_id"]
            val title = if (useTitleAndText) parameters["title"] else null
            val topic = if (useTitleAndText) parameters["topic"] else null
            val key1 = if (useAdditionalData) parameters["key1"] else null
            val key2 = if (useAdditionalData) parameters["key2"] else null

            if (title == null && topic == null && key1 == null && key2 == null) {
                call.respond(HttpStatusCode.BadRequest, "At least one field must be filled!")
                return@post
            }
            // Push Notification Logik
            val success = PushNotificationService.sendPushNotification(
                pushId = pushId,
                topic = title,
                body = topic,
                k1 = key1,
                k2 = key2,
                showAlert = true
            )
            if (success) {
                call.respond(HttpStatusCode.OK, mapOf("status" to "success", "message" to "Push notification sent successfully!"))
            } else {
                call.respond(HttpStatusCode.InternalServerError, mapOf("status" to "error", "message" to "Failed to send push notification."))
            }
    }
        post("/delete-device") {
          val deviceId = call.receiveParameters()["device_id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
          Database.deleteDeviceData(deviceId)
          call.respondRedirect("/data-overview")
       }
    }
}
