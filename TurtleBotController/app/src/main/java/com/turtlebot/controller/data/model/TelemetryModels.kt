package com.turtlebot.controller.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RobotTelemetry(
    @SerialName("version") val version: Int = 1,
    @SerialName("type") val type: String = "telemetry",
    @SerialName("linear_velocity") val linearVelocity: Double = 0.0,
    @SerialName("angular_velocity") val angularVelocity: Double = 0.0,
    @SerialName("battery_voltage") val batteryVoltage: Double = 12.0,
    @SerialName("is_stopped") val isStopped: Boolean = true,
    @SerialName("has_active_lease") val hasActiveLease: Boolean = false,
    @SerialName("active_client") val activeClient: String? = null,
    @SerialName("timestamp") val timestamp: Double = 0.0
)

@Serializable
data class AckResponse(
    @SerialName("version") val version: Int = 1,
    @SerialName("type") val type: String = "ack",
    @SerialName("command_id") val commandId: String = "",
    @SerialName("status") val status: String = "ok",
    @SerialName("reason") val reason: String = "",
    @SerialName("timestamp") val timestamp: Double = 0.0
)

@Serializable
data class PongResponse(
    @SerialName("version") val version: Int = 1,
    @SerialName("type") val type: String = "pong",
    @SerialName("client_timestamp") val clientTimestamp: Double? = null,
    @SerialName("server_timestamp") val serverTimestamp: Double = 0.0
)

@Serializable
data class ErrorResponse(
    @SerialName("version") val version: Int = 1,
    @SerialName("type") val type: String = "error",
    @SerialName("message") val message: String = "",
    @SerialName("command_id") val commandId: String? = null,
    @SerialName("timestamp") val timestamp: Double = 0.0
)
