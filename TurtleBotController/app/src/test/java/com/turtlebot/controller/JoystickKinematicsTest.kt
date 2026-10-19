package com.turtlebot.controller

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class JoystickKinematicsTest {

    private val maxLinear = 0.12f   // m/s
    private val maxAngular = 1.00f  // rad/s
    private val deadzone = 0.08f

    @Test
    fun testDeadzoneSuppression() {
        // Values within deadzone (magnitude < 0.08) must result in exactly 0.0 velocity
        val normX = 0.03f
        val normY = 0.04f
        val magnitude = sqrt(normX * normX + normY * normY)

        val (linear, angular) = computeVelocities(normX, normY, magnitude, deadzone, maxLinear, maxAngular)
        assertEquals(0.0f, linear, 0.0001f)
        assertEquals(0.0f, angular, 0.0001f)
    }

    @Test
    fun testPureForwardMotion() {
        // Full forward: normY = 1.0, normX = 0.0
        val (linear, angular) = computeVelocities(0.0f, 1.0f, 1.0f, deadzone, maxLinear, maxAngular)
        assertEquals(maxLinear, linear, 0.001f)
        assertEquals(0.0f, angular, 0.001f)
    }

    @Test
    fun testPureBackwardMotion() {
        // Full backward: normY = -1.0, normX = 0.0
        val (linear, angular) = computeVelocities(0.0f, -1.0f, 1.0f, deadzone, maxLinear, maxAngular)
        assertEquals(-maxLinear, linear, 0.001f)
        assertEquals(0.0f, angular, 0.001f)
    }

    @Test
    fun testTurnLeftInPlace() {
        // Left turn: normX = -1.0 (left), normY = 0.0 -> positive angular_z
        val (linear, angular) = computeVelocities(-1.0f, 0.0f, 1.0f, deadzone, maxLinear, maxAngular)
        assertEquals(0.0f, linear, 0.001f)
        assertEquals(maxAngular, angular, 0.001f)
    }

    @Test
    fun testTurnRightInPlace() {
        // Right turn: normX = 1.0 (right), normY = 0.0 -> negative angular_z
        val (linear, angular) = computeVelocities(1.0f, 0.0f, 1.0f, deadzone, maxLinear, maxAngular)
        assertEquals(0.0f, linear, 0.001f)
        assertEquals(-maxAngular, angular, 0.001f)
    }

    @Test
    fun testSimultaneousDrivingArc() {
        // Driving forward while curving right: normY = 0.707, normX = 0.707
        val normX = 0.707f
        val normY = 0.707f
        val (linear, angular) = computeVelocities(normX, normY, 1.0f, deadzone, maxLinear, maxAngular)

        assertTrue(linear > 0.0f) // Positive forward speed
        assertTrue(angular < 0.0f) // Negative clockwise rotation
        assertTrue(linear <= maxLinear)
        assertTrue(angular >= -maxAngular)
    }

    private fun computeVelocities(
        normX: Float,
        normY: Float,
        magnitude: Float,
        deadzone: Float,
        maxLinear: Float,
        maxAngular: Float
    ): Pair<Float, Float> {
        if (magnitude < deadzone) {
            return Pair(0.0f, 0.0f)
        }
        val linear = normY.coerceIn(-1.0f, 1.0f) * maxLinear
        val angular = -normX.coerceIn(-1.0f, 1.0f) * maxAngular
        return Pair(linear, angular)
    }
}
