package com.turtlebot.controller

import com.turtlebot.controller.data.model.AckResponse
import com.turtlebot.controller.data.model.MovementCommand
import com.turtlebot.controller.data.model.RobotTelemetry
import com.turtlebot.controller.data.model.StopCommand
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Test
    fun testMovementCommandSerialization() {
        val cmd = MovementCommand(
            linearX = 0.15,
            angularZ = -0.50,
            commandId = "test-cmd-001"
        )
        val jsonStr = json.encodeToString(MovementCommand.serializer(), cmd)
        val element = json.parseToJsonElement(jsonStr).jsonObject

        assertEquals("1", element["version"]?.jsonPrimitive?.content)
        assertEquals("cmd_vel", element["type"]?.jsonPrimitive?.content)
        assertEquals("0.15", element["linear_x"]?.jsonPrimitive?.content)
        assertEquals("-0.5", element["angular_z"]?.jsonPrimitive?.content)
        assertEquals("test-cmd-001", element["command_id"]?.jsonPrimitive?.content)
    }

    @Test
    fun testStopCommandSerialization() {
        val stop = StopCommand(commandId = "stop-001")
        val jsonStr = json.encodeToString(StopCommand.serializer(), stop)
        val element = json.parseToJsonElement(jsonStr).jsonObject

        assertEquals("1", element["version"]?.jsonPrimitive?.content)
        assertEquals("stop", element["type"]?.jsonPrimitive?.content)
        assertEquals("stop-001", element["command_id"]?.jsonPrimitive?.content)
    }

    @Test
    fun testTelemetryDeserialization() {
        val rawJson = """
            {
                "version": 1,
                "type": "telemetry",
                "linear_velocity": 0.12,
                "angular_velocity": 0.45,
                "battery_voltage": 12.2,
                "is_stopped": false,
                "has_active_lease": true,
                "timestamp": 1700000000.0
            }
        """.trimIndent()

        val tel = json.decodeFromString(RobotTelemetry.serializer(), rawJson)
        assertEquals(0.12, tel.linearVelocity, 0.001)
        assertEquals(0.45, tel.angularVelocity, 0.001)
        assertEquals(12.2, tel.batteryVoltage, 0.01)
        assertFalse(tel.isStopped)
        assertTrue(tel.hasActiveLease)
    }

    @Test
    fun testAckDeserialization() {
        val rawJson = """
            {
                "version": 1,
                "type": "ack",
                "command_id": "test-ack-1",
                "status": "ok",
                "reason": "Accepted",
                "timestamp": 1700000001.0
            }
        """.trimIndent()

        val ack = json.decodeFromString(AckResponse.serializer(), rawJson)
        assertEquals("test-ack-1", ack.commandId)
        assertEquals("ok", ack.status)
        assertEquals("Accepted", ack.reason)
    }
}
