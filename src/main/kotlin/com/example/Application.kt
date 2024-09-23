package com.example

import com.example.plugins.*

import org.slf4j.Logger
import org.slf4j.LoggerFactory


import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val logger: Logger = LoggerFactory.getLogger("Application")
    
    logger.info("Application started")

    install(ContentNegotiation) {
        json() // Füge die JSON-Serialisierung hinzu
    }
    configureRouting()
}
