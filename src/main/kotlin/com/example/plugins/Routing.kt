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
import java.util.Base64
import org.slf4j.Logger
import org.slf4j.LoggerFactory


fun formatPushId(pushId: String): String {
  return if (pushId.length > 16) {
    "${pushId.substring(0, 16)}****"
  } else {
    pushId
  }
}

fun Application.configureRouting() {
  val logger: Logger = LoggerFactory.getLogger("Application")

  val contentIds = mapOf(
    "KampagneA-sfMNI" to "9508697563",
    "KampagneA-IronManp" to "9493366586",
    "KampagneA-fsiexp" to "7561469993",
    "KampagneB-BruceW" to "1983068231",
    "KampagneB-AnneW" to "4805970215",
    "KampagneB-SBroDepotPlus1" to "2077139071",
    "KampagneC-larsv" to "6122688630",
    "KampagneC-royb" to "4015714997",
    "KampagneC-carstenb" to "8612016758",
    "KampagneC-DFpush" to "5841929766",
    "KampagneD-S_Broker1SF" to "5695324742",
    "KampagneD-LasttestPeKo" to "7006280225",
    "KampagneD-LasttestBuKo" to "2943700695"
  )
  
  val webviewUrls = mapOf (
    "Card Control" to "https://m164an08-421.if-etaps.de/de/home/service/card-control.webview.html?IF_SILENT_LOGIN=true&n=true&wstart=true",
    "Merkzettel" to "https://m164an08-421.if-etaps.de/de/home/onlinebanking/service/meine-aktivitaeten.webview.html?IF_SILENT_LOGIN=true&n=true&wstart=true",
    "Service Center" to "https://m164an08-421.if-etaps.de/de/home/service.webview.html?IF_SILENT_LOGIN=true&n=true&wstart=true",
    "Limiterhöhung" to "https://m164an08-421.if-etaps.de/de/home/onlinebanking/service/limitaenderung_neo_app.webview.html?IF_SILENT_LOGIN=true&n=true&wstart=true",
    "Finanzplaner" to "https://m164an08-421.if-etaps.de/de/home/onlinebanking/nbf/finanzplaner/dashboard.webview.html?IF_SILENT_LOGIN=true&n=true&wstart=true",
    "Click2Credit" to "https://m164an08-421.if-etaps.de/de/home/privatkunden/kredite-und-finanzierungen/zwei-klick-kredit.webview.html?IF_SILENT_LOGIN=true&n=true&wstart=true",
    "Individuelle Angebote" to "https://m164an08-421.if-etaps.de/de/home/privatkunden/aktuelle-angebote.webview.html?IF_SILENT_LOGIN=true&n=true&wstart=true"
  );

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
                
                const selectBox = document.getElementById('webviewUrlIdSelect');

                for (const key in webviewUrls) {
                  const option = document.createElement('option');
                  option.value = webviewUrls[key];
                  option.textContent = key;
                  selectBox.appendChild(option);
                }

                function updateWebviewPathInput() {
                  const selectElement = document.getElementById('webviewUrlIdSelect');
                  const selectedKey = selectElement.value;
                  // Setze den URL-Wert in das Textfeld
                  const pathInput = document.getElementById('pathInput');
                  pathInput.value = webviewUrls[selectedKey] || '';
                }

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
                  const titleBodyPairs = {
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
                  const keys = Object.keys(titleBodyPairs);
                  const randomKey = keys[Math.floor(Math.random() * keys.length)];
                  document.querySelector('input[name="title"]').value = randomKey;
                  document.querySelector('input[name="body"]').value = titleBodyPairs[randomKey];
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
            style = "display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background-color: rgba(0,0,0,0.5); overflow: auto;"
            div {
              style = "background-color: white; padding: 20px; margin: 50px auto; width: 400px; max-height: 90%; overflow-y: auto; border-radius: 8px;"
              form {
                attributes["id"] = "push-form"
                attributes["onsubmit"] = "return sendPushNotification();"
                hiddenInput(name = "device_id") { value = "" }  // Wird dynamisch gesetzt
                textInput(name = "push_id") { placeholder = "Push ID" }  // Befüllt durch JavaScript

                table {
                  tr {
                    td { label { +"BLZ:" } }
                    td { textInput { name = "blz"; value = "94059421"; style = "margin: 10px; width: calc(100% - 20px);" } }
                  }
                  tr {
                    td { label { +"OBV:" } }
                    td { textInput { name = "obv"; value = "royb"; style = "margin: 10px; width: calc(100% - 20px);" } }
                  }
                  tr {
                    td { label { +"Titel:" } }
                    td { textInput(name = "title") { placeholder = "Title"; style = "margin: 10px; width: calc(100% - 20px);" } }
                  }
                  tr {
                    td { label { +"Nachricht:" } }
                    td { textInput(name = "body") { placeholder = "Body"; style = "margin: 10px; width: calc(100% - 20px);" } }
                  }
                  tr {
                    td { p { +"" } }
                    td {
                      button(type = ButtonType.button) {
                          style = "margin: 10px; width: calc(100% - 20px);"
                          onClick = """
                              console.log('Zufällige Ansprache button clicked');
                              randomizeMessage();
                          """
                          +"Zufällige Ansprache"
                      }
                    }
                  }
                  tr {
                    td { label { +"Select Structure:" } }
                    td {
                      select {
                        name = "structure"
                        id = "structure"
                        option { value = ""; +"None" }
                        option { value = "IAM"; +"IAM" }
                        option { value = "WEBVIEW"; +"WEBVIEW" }
                        option { value = "MAILBOX"; +"MAILBOX" }
                        option { value = "PING"; +"PING" }
                        option { value = "UPDATE"; +"UPDATE" }
                        option { value = "BALANCE"; +"BALANCE" }
                      }
                    }
                  }
                }

                // IAM specific fields
                div {
                  id = "IAMFields"
                  style = "display: none;"
                  table {
                    tr {
                      td { label { +"Content ID:" } }
                      td {
                          select {
                              name = "contentIdSelect"
                              contentIds.forEach { (key, _) ->
                                  option {
                                      value = key
                                      +key
                                  }
                              }
                          }
                      }
                    }
                    tr {
                      td { label { +"Event ID:" } }
                      td { textInput { name = "eventId"; style = "margin: 10px; width: calc(100% - 20px);" } }
                    }
                    tr {
                      td { label { +"URI:" } }
                      td { textInput { name = "uri"; style = "margin: 10px; width: calc(100% - 20px);" } }
                    }
                    tr {
                      td { label { +"Use Banner:" } }
                      td { checkBoxInput { name = "useBanner" } }
                    }
                    tr {
                      td { label { +"Show Disrupter:" } }
                      td { checkBoxInput { name = "showDisrupter" } }
                    }
                  }
                }

                // WEBVIEW specific fields
                div {
                  id = "WEBVIEWFields"
                  style = "display: none;"
                  table {
                    tr {
                      td {
                          label { +"WebView Path:" }
                      }
                      td {
                          select {
                              name = "webviewUrlIdSelect"
                              id = "webviewUrlIdSelect"
                              webviewUrls.forEach { (key, _) ->
                                  option {
                                      value = key
                                      +key
                                  }
                              }
                          }
                      }
                    }
                  }
                }

                // MAILBOX specific fields
                div {
                  id = "MAILBOXFields"
                  style = "display: none;"
                  table {
                    tr {
                      td { label { +"Mail Count:" } }
                      td { textInput { name = "count"; style = "width: 100%;" } }
                    }
                  }
                }

                // PING has no fields
                div {
                  id = "PINGFields"
                  style = "display: none;"
                  p { +"No fields for PING" }
                }

                // UPDATE specific fields
                div {
                  id = "UPDATEFields"
                  style = "display: none;"
                  table {
                    tr {
                      td { label { +"Version:" } }
                      td { textInput { name = "fromVersion"; style = "width: 100%;" } }
                    }
                  }
                }

                // BALANCE specific fields
                div {
                  id = "BALANCEFields"
                  style = "display: none;"
                  table {
                    tr {
                      td { label { +"IBAN:" } }
                      td { textInput { name = "iban"; style = "width: 100%;" } }
                    }
                    tr {
                      td { label { +"Balance:" } }
                      td { textInput { name = "balance"; style = "width: 100%;" } }
                    }
                  }
                }
                // Senden-Button, der zunächst inaktiv ist
                button(type = ButtonType.submit) {
                  style = "margin: 10px; width: calc(100% - 20px);"
                  disabled = false
                  +"Senden"
                }

                // Löschen-Button
                button(type = ButtonType.button) {
                  style = "margin: 10px; width: calc(100% - 20px);"
                  onClick = """
                    document.querySelector('input[name="title"]').value = '';
                    document.querySelector('input[name="body"]').value = '';
                    document.querySelector('input[name="key1"]').value = '';
                    document.querySelector('input[name="key2"]').value = '';
                    document.querySelector('input[name="useTitleAndText"]').checked = false;
                    document.querySelector('input[name="useAdditionalData"]').checked = false;
                    checkSendButton();
                  """
                  +"Inhalte löschen"
                }

                // Abbrechen-Button
                button {
                  style = "margin: 10px; width: calc(100% - 20px);"
                  type = ButtonType.button
                  onClick = "document.getElementById('overlay').style.display='none';"
                  +"Abbrechen"
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
                  td { +formatPushId(deviceData.push_id) } 
                  td { +deviceData.login_id }
                  td { +deviceData.last_login.toString() }
                  td {
                    button {
                      type = ButtonType.button
                      onClick = """
                        // Overlay anzeigen
                        document.getElementById('overlay').style.display='block';
                        
                        // Werte für die input-Felder setzen
                        document.querySelector('input[name="device_id"]').value='${deviceData.device_id}';
                        document.querySelector('input[name="push_id"]').value='${deviceData.push_id}';
                        
                        // Event-Listener für das Select-Element hinzufügen, nachdem das Overlay sichtbar ist
                        const structureElement = document.getElementById('structure');
                        if (structureElement) {
                          console.log('structureElement gefunden');
                          structureElement.addEventListener('change', function() {
                            var selectedStructure = this.value;
                            var sections = ['IAMFields', 'WEBVIEWFields', 'MAILBOXFields', 'PINGFields', 'UPDATEFields', 'BALANCEFields'];
                            sections.forEach(function(section) {
                              document.getElementById(section).style.display = 'none';
                            });
                            if (selectedStructure) {
                              document.getElementById(selectedStructure + 'Fields').style.display = 'block';
                            }
                          });
                        } else {
                          console.log('structureElement nicht gefunden');
                        }
                      """
                      + "Send"
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
            style = "display: flex; align-items: center; justify-content: center; margin-top: 10px;"

            a(href = "/static/app-debug.apk") {
              +"Download APK"
            }

            // Trenner
            span {
              style = "margin: 0 10px; border-left: 1px solid #ccc; height: 20px; align-self: center;"
            }

            a(href = "/deeplinks.html") {
              +"Deeplinks"
            }
          }
        }
      }
    }
    // Empfangen des Formulars und Senden der Push-Benachrichtigung
    post("/send-push") {
      val parameters = call.receiveParameters()

      val pushId = parameters["push_id"]
      // Überprüfe, ob pushId null ist
      if (pushId == null) {
          call.respond(HttpStatusCode.BadRequest, "Push ID cannot be null")
          return@post // Beende die Ausführung, falls pushId null ist
      }

      val blz = parameters["blz"]
      val obv = parameters["obv"]
      val title = parameters["title"]
      val body = parameters["body"]
      val notificationType = parameters["structure"]
      var payload = """
          {
              "blz": "$blz",
              "obv": "$obv"
              "title": "$title",
              "body": "$body",
          }
      """.trimIndent()

      if (notificationType == "IAM") {
          val contentIdSelect = parameters["contentIdSelect"]
          logger.info("contentIdSelect: ${contentIdSelect}")
          val contentId = contentIds[contentIdSelect] ?: "Kein Wert gefunden"
          
          val eventId = parameters["eventId"]
          val uri = parameters["uri"]
          val useBanner = parameters["useBanner"]?.let { it == "on" } ?: false
          val showDisrupter = parameters["showDisrupter"]?.let { it == "on" } ?: false
          payload = """
              {
                  "blz": "$blz",
                  "obv": "$obv",
                  "title": "$title",
                  "body": "$body",
                  "iam": {
                      "contentId": "$contentId",
                      "eventId": "$eventId",
                      "uri": "$uri",
                      "useBanner": $useBanner,
                      "showDisrupter": $showDisrupter
                  }
              }
          """.trimIndent()
      }

      if (notificationType == "WEBVIEW") {
          val webviewUrlIdSelect = parameters["webviewUrlIdSelect"]
          logger.info("webviewUrlIdSelect: ${webviewUrlIdSelect}")
          val webviewUrl = webviewUrls[webviewUrlIdSelect] ?: "Kein Wert gefunden"
          val path = parameters["path"]
          payload = """
              {
                  "blz": "$blz",
                  "obv": "$obv",
                  "title": "$title",
                  "body": "$body",
                  "webview": {
                      "path": "$webviewUrl"
                  }
              }
          """.trimIndent()
      }
      
      if ( notificationType == "MAILBOX" ) {
          val count = parameters["count"]
          payload = """
              {
                  "blz": "$blz",
                  "obv": "$obv",
                  "title": "$title",
                  "body": "$body",
                  "mailbox": {
                      "count": "$count"
                  }
              }
          """.trimIndent()
        }
        
      if ( notificationType == "PING" ) {
        payload = """
            {
                "blz": "$blz",
                "obv": "$obv",
                "title": "$title",
                "body": "$body",
                "ping": {
                }
            }
        """.trimIndent()
      }
      
      if ( notificationType == "UPDATE" ) {
        val fromVersion = parameters["fromVersion"]
        payload = """
            {
                "blz": "$blz",
                "obv": "$obv",
                "title": "$title",
                "body": "$body",
                "update": {
                    "fromVersion": "$fromVersion"
                }
            }
        """.trimIndent()
      }
      
      if ( notificationType == "BALANCE" ) {
        val balance = parameters["balance"]
        val iban = parameters["iban"]
        payload = """
            {
                "blz": "$blz",
                "obv": "$obv",
                "title": "$title",
                "body": "$body",
                "balance": {
                  "iban":"$iban",
                  "balance":"$balance"
                }
            }
        """.trimIndent()
      }
      
      logger.info("${payload}")

      val encodedPayload = Base64.getEncoder().encodeToString(payload.toByteArray()) 
      logger.info("${encodedPayload}")
      
      // Push Notification Logik
      val success = PushNotificationService.sendPushNotification(
        pushId = pushId,
        topic = "",
        body = "",
        k1 = encodedPayload,
        k2 = null
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
