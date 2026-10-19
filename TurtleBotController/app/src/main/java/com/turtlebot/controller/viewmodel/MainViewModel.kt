package com.turtlebot.controller.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.turtlebot.controller.data.model.AckResponse
import com.turtlebot.controller.data.model.ConnectionState
import com.turtlebot.controller.data.model.ControlMode
import com.turtlebot.controller.data.model.MovementDirection
import com.turtlebot.controller.data.model.NetworkTopologyPreset
import com.turtlebot.controller.data.model.RobotTelemetry
import com.turtlebot.controller.data.repository.RobotRepository
import com.turtlebot.controller.data.repository.SettingsRepository
import com.turtlebot.controller.safety.SafetyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val settingsRepository = SettingsRepository(application.applicationContext)

    private val repository: RobotRepository = RobotRepository(
        onEmergencySafetyStopTriggered = {
            safetyManager.onJoystickReleased()
        },
        context = application.applicationContext
    )

    val safetyManager: SafetyManager = SafetyManager(repository, viewModelScope)

    val connectionState: StateFlow<ConnectionState> = repository.connectionState
    val telemetry: StateFlow<RobotTelemetry?> = repository.telemetry
    val lastAck: StateFlow<AckResponse?> = repository.lastAck
    val errorMessage: StateFlow<String?> = repository.errorMessage

    val currentDirection: StateFlow<MovementDirection> = safetyManager.currentDirection
    val isEmergencyStopped: StateFlow<Boolean> = safetyManager.isEmergencyStopped

    private val _controlMode = MutableStateFlow(settingsRepository.getControlMode())
    val controlMode: StateFlow<ControlMode> = _controlMode.asStateFlow()

    private val _linearSpeed = MutableStateFlow(settingsRepository.getLinearSpeed())
    val linearSpeed: StateFlow<Float> = _linearSpeed.asStateFlow()

    private val _angularSpeed = MutableStateFlow(settingsRepository.getAngularSpeed())
    val angularSpeed: StateFlow<Float> = _angularSpeed.asStateFlow()

    fun connect() {
        val host = settingsRepository.getHost()
        val port = settingsRepository.getPort()
        repository.connect(host, port)
    }

    fun disconnect() {
        safetyManager.onJoystickReleased()
        repository.disconnect()
    }

    fun setControlMode(mode: ControlMode) {
        _controlMode.value = mode
        settingsRepository.setControlMode(mode)
        safetyManager.onJoystickReleased()
    }

    fun onJoystickMoved(normX: Float, normY: Float, magnitude: Float) {
        safetyManager.onJoystickMoved(
            normX = normX,
            normY = normY,
            magnitude = magnitude,
            maxLinearSpeed = _linearSpeed.value,
            maxAngularSpeed = _angularSpeed.value
        )
    }

    fun onJoystickReleased() {
        safetyManager.onJoystickReleased()
    }

    fun onDirectionPressed(direction: MovementDirection) {
        safetyManager.onDirectionPressed(
            direction = direction,
            linearSpeed = _linearSpeed.value,
            angularSpeed = _angularSpeed.value
        )
    }

    fun onDirectionReleased() {
        safetyManager.onDirectionReleased()
    }

    fun triggerEmergencyStop() {
        safetyManager.triggerEmergencyStop()
    }

    fun updateLinearSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.02f, SettingsRepository.HARD_MAX_LINEAR)
        _linearSpeed.value = clamped
        settingsRepository.setLinearSpeed(clamped)
    }

    fun updateAngularSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.10f, SettingsRepository.HARD_MAX_ANGULAR)
        _angularSpeed.value = clamped
        settingsRepository.setAngularSpeed(clamped)
    }

    fun applyPreset(preset: NetworkTopologyPreset) {
        settingsRepository.applyPreset(preset)
        disconnect()
    }

    fun onAppPaused() {
        safetyManager.onAppPaused()
    }

    fun testReachability(host: String, port: Int, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.testReachability(host, port)
            onResult(result.first, result.second)
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
