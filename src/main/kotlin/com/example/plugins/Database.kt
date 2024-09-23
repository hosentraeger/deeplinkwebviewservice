package com.example.plugins

import com.example.models.DeviceData

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Database {
    private const val jdbcUrl = "jdbc:mariadb://localhost:3306/deeplinkwebviewservice"
    private const val dbUser = "root" // Ersetze mit deinem DB-Benutzernamen
    private const val dbPassword = "Start21Fsi" // Ersetze mit deinem DB-Passwort
    private var connection: Connection? = null

    fun init() {
        val logger: Logger = LoggerFactory.getLogger("Application")
        logger.info("Database init")
        connection = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword)
    }

    fun saveDeviceData(deviceData: DeviceData) {
        val logger: Logger = LoggerFactory.getLogger("Application")
        logger.info("device_data: $deviceData")

        if ( null == connection ) {
          logger.info("db connection is null")
          connection = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword)
        }
        if ( null == connection ) {
          logger.info("db connection is still null")
        }
        val query = """
            INSERT INTO device_data (device_id, push_id, login_id, last_login) 
            VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE 
            push_id = VALUES(push_id), 
            login_id = VALUES(login_id), 
            last_login = VALUES(last_login)
        """.trimIndent()

        val formattedTimestamp = LocalDateTime.parse(deviceData.last_login, DateTimeFormatter.ISO_DATE_TIME)
        logger.info("formattedTimestamp: $formattedTimestamp")

        connection?.prepareStatement(query)?.apply {
            setString(1, deviceData.device_id)
            setString(2, deviceData.push_id)
            setString(3, deviceData.login_id)

            // Umwandlung des last_login in das richtige Format
            val formattedTimestamp = LocalDateTime.parse(deviceData.last_login, DateTimeFormatter.ISO_DATE_TIME)
            setTimestamp(4, java.sql.Timestamp.valueOf(formattedTimestamp))

            executeUpdate()
        }
    }
}
