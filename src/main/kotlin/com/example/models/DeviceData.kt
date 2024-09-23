package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class DeviceData(
    val device_id: String,
    val push_id: String,
    val login_id: String,
    val last_login: String // Timestamp als String
)
