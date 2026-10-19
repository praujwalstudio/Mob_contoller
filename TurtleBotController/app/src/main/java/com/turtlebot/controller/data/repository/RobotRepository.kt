package com.turtlebot.controller.data.repository

import android.content.Context
import android.util.Log
import com.turtlebot.controller.data.model.AckResponse
import com.turtlebot.controller.data.model.ConnectionState
import com.turtlebot.controller.data.model.ErrorResponse
import com.turtlebot.controller.data.model.MovementCommand
import com.turtlebot.controller.data.model.PingMessage
import com.turtlebot.controller.data.model.PongResponse
import com.turtlebot.controller.data.model.RobotTelemetry
import com.turtlebot.controller.data.model.StopCommand
import com.turtlebot.controller.data.network.RobotWebSocketClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class RobotRepository(
    private val onEmergencySafetyStopTriggered: () -> Unit = {},
    private val context: Context? = null
) {
    private val tag = "RobotRepository"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val _telemetry = MutableStateFlow<RobotTelemetry?>(null)
    val telemetry: StateFlow<RobotTelemetry?> = _telemetry.asStateFlow()

    private val _lastAck = MutableStateFlow<AckResponse?>(null)
    val lastAck: StateFlow<AckResponse?> = _lastAck.asStateFlow()

    private val _lastSentCommand = MutableStateFlow<MovementCommand?>(null)
    val lastSentCommand: StateFlow<MovementCommand?> = _lastSentCommand.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val client = RobotWebSocketClient(
        onMessageReceived = { rawText -> handleIncomingMessage(rawText) },
        onConnectionLost = {
            Log.w(tag, "Connection lost! Triggering safety halt.")
            _telemetry.value = null
            onEmergencySafetyStopTriggered()
        },
        context = context
    )

    val connectionState: StateFlow<ConnectionState> = client.connectionState

    suspend fun testReachability(host: String, port: Int): Pair<Boolean, String> {
        return client.testReachability(host, port)
    }

    fun connect(host: String, port: Int) {
        _errorMessage.value = null
        client.connect(host, port)
    }

    fun disconnect() {
        sendStop()
        client.disconnect()
        _telemetry.value = null
    }

    fun isConnected(): Boolean = client.isConnected()

    fun sendMovement(linearX: Double, angularZ: Double): Boolean {
        if (!client.isConnected()) {
            Log.w(tag, "Cannot send movement: not connected.")
            return false
        }

        val cmd = MovementCommand(
            linearX = linearX,
            angularZ = angularZ
        )
        val payload = json.encodeToString(MovementCommand.serializer(), cmd)
        val success = client.send(payload)
        if (success) {
            _lastSentCommand.value = cmd
        }
        return success
    }

    fun sendStop(): Boolean {
        val stopCmd = StopCommand()
        val payload = json.encodeToString(StopCommand.serializer(), stopCmd)
        val sent = client.send(payload)
        _lastSentCommand.value = null
        return sent
    }

    fun sendPing(): Boolean {
        if (!client.isConnected()) return false
        val ping = PingMessage()
        val payload = json.encodeToString(PingMessage.serializer(), ping)
        return client.send(payload)
    }

    private fun handleIncomingMessage(rawText: String) {
        try {
            val jsonElement = json.parseToJsonElement(rawText)
            val type = jsonElement.jsonObject["type"]?.jsonPrimitive?.content

            when (type) {
                "telemetry" -> {
                    val tel = json.decodeFromString(RobotTelemetry.serializer(), rawText)
                    _telemetry.value = tel
                }
                "ack" -> {
                    val ack = json.decodeFromString(AckResponse.serializer(), rawText)
                    _lastAck.value = ack
                }
                "error" -> {
                    val err = json.decodeFromString(ErrorResponse.serializer(), rawText)
                    _errorMessage.value = err.message
                    Log.e(tag, "Robot bridge error: ${err.message}")
                }
                "pong" -> {
                    Log.d(tag, "Pong received from bridge")
                }
                else -> {
                    Log.w(tag, "Unrecognized message type: $type")
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to parse incoming message: $rawText", e)
        }
    }
}
