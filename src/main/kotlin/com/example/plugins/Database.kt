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

    private val logger: Logger = LoggerFactory.getLogger("Application")

    private fun getConnection(): Connection {
        if (connection == null || connection!!.isClosed) {
            logger.error("Database connection null, re-creating")
            connection = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword)
        }
        return connection!!
    }
    
    fun init() {
      val logger: Logger = LoggerFactory.getLogger("Application")
    }
    
    // Speichere oder aktualisiere DeviceData in der Datenbank
    fun saveDeviceData(deviceData: DeviceData) {
        logger.info("Saving device data: $deviceData")

        val conn = getConnection()

        val query = """
            INSERT INTO device_data (device_id, push_id, login_id, last_login)
            VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE 
            push_id = VALUES(push_id),
            login_id = VALUES(login_id),
            last_login = VALUES(last_login)
        """.trimIndent()

        val formattedTimestamp = LocalDateTime.parse(deviceData.last_login, DateTimeFormatter.ISO_DATE_TIME)
        logger.info("Formatted Timestamp: $formattedTimestamp")

        conn?.prepareStatement(query)?.apply {
            setString(1, deviceData.device_id)
            setString(2, deviceData.push_id)
            setString(3, deviceData.login_id)
            setTimestamp(4, java.sql.Timestamp.valueOf(formattedTimestamp))
            executeUpdate()
        } ?: logger.error("Failed to save device data due to null conn")
    }

    // Hole alle DeviceData-Einträge aus der Datenbank
    fun getAllDeviceData(): List<DeviceData> {
        logger.info("Fetching all device data")
        val query = "SELECT device_id, push_id, login_id, last_login FROM device_data"
        val deviceDataList = mutableListOf<DeviceData>()

        val conn = getConnection()

        conn?.prepareStatement(query)?.use { statement ->
            val resultSet = statement.executeQuery()
            while (resultSet.next()) {
                val deviceData = DeviceData(
                    device_id = resultSet.getString("device_id"),
                    push_id = resultSet.getString("push_id"),
                    login_id = resultSet.getString("login_id"),
                    last_login = resultSet.getString("last_login")
                )
                deviceDataList.add(deviceData)
            }
        } ?: logger.error("Failed to fetch device data due to null conn")

        return deviceDataList
    }

    // Hole ein spezifisches DeviceData basierend auf der device_id
    fun getDeviceDataById(deviceId: String): DeviceData? {
        logger.info("Fetching device data for device_id: $deviceId")
        val query = "SELECT device_id, push_id, login_id, last_login FROM device_data WHERE device_id = ?"
        val conn = getConnection()
        conn?.prepareStatement(query)?.use { statement ->
            statement.setString(1, deviceId)
            val resultSet = statement.executeQuery()
            if (resultSet.next()) {
                return DeviceData(
                    device_id = resultSet.getString("device_id"),
                    push_id = resultSet.getString("push_id"),
                    login_id = resultSet.getString("login_id"),
                    last_login = resultSet.getString("last_login")
                )
            }
        } ?: logger.error("Failed to fetch device data for $deviceId due to null conn")

        return null
    }
    fun deleteDeviceData(deviceId: String): Boolean {
        logger.info("Deleting device data for device_id: $deviceId")
        val query = "DELETE FROM device_data WHERE device_id = ?"
        val conn = getConnection()
        return try {
            conn?.prepareStatement(query)?.use { statement ->
                statement.setString(1, deviceId)
                val rowsAffected = statement.executeUpdate()
                if (rowsAffected > 0) {
                    logger.info("Successfully deleted device data for device_id: $deviceId")
                    true
                } else {
                    logger.warn("No device data found to delete for device_id: $deviceId")
                    false
                }
            } ?: run {
                logger.error("Failed to delete device data for $deviceId due to null conn")
                false
            }
        } catch (e: SQLException) {
            logger.error("SQL error while deleting device data for device_id: $deviceId", e)
            false
        }
    }
}
