package com.turtlebot.controller.safety

import android.util.Log
import com.turtlebot.controller.data.model.MovementDirection
import com.turtlebot.controller.data.repository.RobotRepository
import com.turtlebot.controller.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SafetyManager(
    private val repository: RobotRepository,
    private val scope: CoroutineScope
) {
    private val tag = "SafetyManager"

    // Pulse interval (100ms): safely renews the robot's 400ms lease while moving
    private val commandPulseIntervalMs = 100L

    private var activePulseJob: Job? = null

    private val _currentDirection = MutableStateFlow(MovementDirection.STOP)
    val currentDirection: StateFlow<MovementDirection> = _currentDirection.asStateFlow()

    private val _isEmergencyStopped = MutableStateFlow(false)
    val isEmergencyStopped: StateFlow<Boolean> = _isEmergencyStopped.asStateFlow()

    // Current continuous analog velocities
    private var targetLinearX: Double = 0.0
    private var targetAngularZ: Double = 0.0

    /**
     * Called continuously when the user drags the Universal 360° Analog Joystick.
     * Maps normalized normX [-1..1] and normY [-1..1] to proportional physical velocities.
     */
    @Synchronized
    fun onJoystickMoved(
        normX: Float,
        normY: Float,
        magnitude: Float,
        maxLinearSpeed: Float,
        maxAngularSpeed: Float
    ) {
        if (!repository.isConnected()) {
            return
        }

        _isEmergencyStopped.value = false

        if (magnitude <= 0.01f) {
            onJoystickReleased()
            return
        }

        // Speed calculation: Y maps to linear velocity, X maps to angular velocity (turning)
        val safeLinearMax = maxLinearSpeed.coerceIn(0.01f, SettingsRepository.HARD_MAX_LINEAR)
        val safeAngularMax = maxAngularSpeed.coerceIn(0.05f, SettingsRepository.HARD_MAX_ANGULAR)

        // normY > 0 is Forward, normY < 0 is Backward
        // normX < 0 is Turn Left (positive angular_z), normX > 0 is Turn Right (negative angular_z)
        targetLinearX = (normY * safeLinearMax).toDouble()
        targetAngularZ = (-normX * safeAngularMax).toDouble()

        // Update nominal direction for status displays
        _currentDirection.value = when {
            Math.abs(normY) > Math.abs(normX) -> if (normY > 0) MovementDirection.FORWARD else MovementDirection.BACKWARD
            Math.abs(normX) > 0.1f -> if (normX < 0) MovementDirection.LEFT else MovementDirection.RIGHT
            else -> MovementDirection.STOP
        }

        // Start or maintain the pulse loop
        if (activePulseJob == null || activePulseJob?.isActive == false) {
            startLeasePulseLoop()
        }
    }

    /**
     * Called the instant the user's finger leaves the virtual joystick thumbstick.
     * Snaps velocities to 0.0 immediately and transmits a burst stop.
     */
    @Synchronized
    fun onJoystickReleased() {
        targetLinearX = 0.0
        targetAngularZ = 0.0
        _currentDirection.value = MovementDirection.STOP

        activePulseJob?.cancel()
        activePulseJob = null

        scope.launch(Dispatchers.IO) {
            // Send explicit 3-burst stop to guarantee motor controller halts
            repeat(3) {
                repository.sendStop()
                delay(20)
            }
        }
    }

    /**
     * Called when the user presses and holds a directional D-Pad button.
     */
    @Synchronized
    fun onDirectionPressed(
        direction: MovementDirection,
        linearSpeed: Float,
        angularSpeed: Float
    ) {
        if (!repository.isConnected()) {
            Log.w(tag, "Cannot start motion: robot is not connected.")
            return
        }

        _isEmergencyStopped.value = false
        _currentDirection.value = direction

        val (linearX, angularZ) = calculateVelocities(direction, linearSpeed, angularSpeed)
        targetLinearX = linearX
        targetAngularZ = angularZ

        if (activePulseJob == null || activePulseJob?.isActive == false) {
            startLeasePulseLoop()
        }
    }

    /**
     * Called immediately when a D-Pad button is released.
     */
    @Synchronized
    fun onDirectionReleased() {
        onJoystickReleased()
    }

    /**
     * Central Emergency STOP button pressed. Overrides all active operations.
     */
    @Synchronized
    fun triggerEmergencyStop() {
        Log.w(tag, "EMERGENCY STOP TRIGGERED!")
        targetLinearX = 0.0
        targetAngularZ = 0.0
        activePulseJob?.cancel()
        activePulseJob = null
        _currentDirection.value = MovementDirection.STOP
        _isEmergencyStopped.value = true

        scope.launch(Dispatchers.IO) {
            repeat(3) {
                repository.sendStop()
                delay(20)
            }
        }
    }

    /**
     * Lifecycle hook: invoked when app is paused, minimized, or screen turns off.
     */
    @Synchronized
    fun onAppPaused() {
        Log.w(tag, "App paused or lost focus. Halting all movement commands.")
        onJoystickReleased()
    }

    private fun startLeasePulseLoop() {
        activePulseJob = scope.launch(Dispatchers.IO) {
            try {
                while (isActive) {
                    if (Math.abs(targetLinearX) > 1e-4 || Math.abs(targetAngularZ) > 1e-4) {
                        repository.sendMovement(targetLinearX, targetAngularZ)
                    } else {
                        repository.sendStop()
                    }
                    delay(commandPulseIntervalMs)
                }
            } finally {
                repository.sendStop()
            }
        }
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
