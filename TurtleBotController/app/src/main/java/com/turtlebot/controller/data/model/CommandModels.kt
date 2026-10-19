package com.turtlebot.controller.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

enum class MovementDirection {
    FORWARD,
    BACKWARD,
    LEFT,
    RIGHT,
    STOP
}

@Serializable
data class MovementCommand(
    @SerialName("version") val version: Int = 1,
    @SerialName("type") val type: String = "cmd_vel",
    @SerialName("linear_x") val linearX: Double,
    @SerialName("angular_z") val angularZ: Double,
    @SerialName("command_id") val commandId: String = UUID.randomUUID().toString(),
    @SerialName("timestamp") val timestamp: Double = System.currentTimeMillis() / 1000.0
)

@Serializable
data class StopCommand(
    @SerialName("version") val version: Int = 1,
    @SerialName("type") val type: String = "stop",
    @SerialName("command_id") val commandId: String = UUID.randomUUID().toString(),
    @SerialName("timestamp") val timestamp: Double = System.currentTimeMillis() / 1000.0
)

@Serializable
data class PingMessage(
    @SerialName("version") val version: Int = 1,
    @SerialName("type") val type: String = "ping",
    @SerialName("timestamp") val timestamp: Double = System.currentTimeMillis() / 1000.0
)
