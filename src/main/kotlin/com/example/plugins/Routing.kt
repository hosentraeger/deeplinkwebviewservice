package com.example.plugins

import com.example.models.DeviceData

import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

fun Application.configureRouting() {
    routing {
        post("/appstart") {
            val deviceData = call.receive<DeviceData>()
            Database.saveDeviceData(deviceData)
            call.respond(deviceData)
        }
        get("/sendpush") {
            call.respondText("Hello World!")
        }
    }
}
