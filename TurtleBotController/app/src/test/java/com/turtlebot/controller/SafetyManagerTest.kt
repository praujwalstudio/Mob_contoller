package com.turtlebot.controller

import com.turtlebot.controller.data.model.MovementDirection
import com.turtlebot.controller.data.repository.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyManagerTest {

    @Test
    fun testHardVelocityClamping() {
        val repo = SettingsRepository.HARD_MAX_LINEAR
        val angularRepo = SettingsRepository.HARD_MAX_ANGULAR

        val excessiveLinear = 1.5f
        val clampedLinear = excessiveLinear.coerceIn(0.01f, repo)
        assertEquals(0.22f, clampedLinear, 0.001f)

        val excessiveAngular = 5.0f
        val clampedAngular = excessiveAngular.coerceIn(0.05f, angularRepo)
        assertEquals(2.84f, clampedAngular, 0.001f)
    }

    @Test
    fun testDirectionVectors() {
        val linearSpeed = 0.10f
        val angularSpeed = 0.50f

        // Forward
        val (fwdX, fwdZ) = calculateVelocities(MovementDirection.FORWARD, linearSpeed, angularSpeed)
        assertTrue(fwdX > 0.0)
        assertEquals(0.0, fwdZ, 0.001)

        // Backward
        val (bwdX, bwdZ) = calculateVelocities(MovementDirection.BACKWARD, linearSpeed, angularSpeed)
        assertTrue(bwdX < 0.0)
        assertEquals(0.0, bwdZ, 0.001)

        // Turn Left
        val (leftX, leftZ) = calculateVelocities(MovementDirection.LEFT, linearSpeed, angularSpeed)
        assertEquals(0.0, leftX, 0.001)
        assertTrue(leftZ > 0.0)

        // Turn Right
        val (rightX, rightZ) = calculateVelocities(MovementDirection.RIGHT, linearSpeed, angularSpeed)
        assertEquals(0.0, rightX, 0.001)
        assertTrue(rightZ < 0.0)

        // Stop
        val (stopX, stopZ) = calculateVelocities(MovementDirection.STOP, linearSpeed, angularSpeed)
        assertEquals(0.0, stopX, 0.001)
        assertEquals(0.0, stopZ, 0.001)
    }

    private fun calculateVelocities(
        direction: MovementDirection,
        linearSpeed: Float,
        angularSpeed: Float
    ): Pair<Double, Double> {
        val safeLinear = linearSpeed.coerceIn(0.01f, SettingsRepository.HARD_MAX_LINEAR).toDouble()
        val safeAngular = angularSpeed.coerceIn(0.05f, SettingsRepository.HARD_MAX_ANGULAR).toDouble()

        return when (direction) {
            MovementDirection.FORWARD -> Pair(safeLinear, 0.0)
            MovementDirection.BACKWARD -> Pair(-safeLinear, 0.0)
            MovementDirection.LEFT -> Pair(0.0, safeAngular)
            MovementDirection.RIGHT -> Pair(0.0, -safeAngular)
            MovementDirection.STOP -> Pair(0.0, 0.0)
        }
    }
}
