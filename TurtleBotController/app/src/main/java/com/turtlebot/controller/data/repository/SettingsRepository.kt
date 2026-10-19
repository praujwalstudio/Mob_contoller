package com.turtlebot.controller.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.turtlebot.controller.data.model.ControlMode
import com.turtlebot.controller.data.model.NetworkTopologyPreset

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("turtlebot_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_HOST = "pref_robot_host"
        private const val KEY_PORT = "pref_robot_port"
        private const val KEY_LINEAR_SPEED = "pref_max_linear_speed"
        private const val KEY_ANGULAR_SPEED = "pref_max_angular_speed"
        private const val KEY_CONTROL_MODE = "pref_control_mode"

        const val DEFAULT_HOST = "192.168.4.1"
        const val DEFAULT_PORT = 8765
        const val DEFAULT_LINEAR_SPEED = 0.08f   // 0.08 m/s (safe crawl)
        const val DEFAULT_ANGULAR_SPEED = 0.50f  // 0.50 rad/s (gentle turn)

        // Absolute hardware maximums for safety enforcement
        const val HARD_MAX_LINEAR = 0.22f
        const val HARD_MAX_ANGULAR = 2.84f
    }

    fun getHost(): String = prefs.getString(KEY_HOST, DEFAULT_HOST) ?: DEFAULT_HOST

    fun setHost(host: String) {
        prefs.edit().putString(KEY_HOST, host.trim()).apply()
    }

    fun getPort(): Int = prefs.getInt(KEY_PORT, DEFAULT_PORT)

    fun setPort(port: Int) {
        prefs.edit().putInt(KEY_PORT, port).apply()
    }

    fun getLinearSpeed(): Float = prefs.getFloat(KEY_LINEAR_SPEED, DEFAULT_LINEAR_SPEED)

    fun setLinearSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.02f, HARD_MAX_LINEAR)
        prefs.edit().putFloat(KEY_LINEAR_SPEED, clamped).apply()
    }

    fun getAngularSpeed(): Float = prefs.getFloat(KEY_ANGULAR_SPEED, DEFAULT_ANGULAR_SPEED)

    fun setAngularSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.10f, HARD_MAX_ANGULAR)
        prefs.edit().putFloat(KEY_ANGULAR_SPEED, clamped).apply()
    }

    fun getControlMode(): ControlMode {
        val modeStr = prefs.getString(KEY_CONTROL_MODE, ControlMode.ANALOG_JOYSTICK.name)
        return try {
            ControlMode.valueOf(modeStr ?: ControlMode.ANALOG_JOYSTICK.name)
        } catch (e: Exception) {
            ControlMode.ANALOG_JOYSTICK
        }
    }

    fun setControlMode(mode: ControlMode) {
        prefs.edit().putString(KEY_CONTROL_MODE, mode.name).apply()
    }

    fun applyPreset(preset: NetworkTopologyPreset) {
        setHost(preset.defaultIp)
        setPort(preset.defaultPort)
    }
}
