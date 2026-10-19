package com.turtlebot.controller.data.model

enum class ControlMode {
    ANALOG_JOYSTICK,
    TACTILE_DPAD
}

enum class NetworkTopologyPreset(
    val title: String,
    val defaultIp: String,
    val defaultPort: Int,
    val description: String
) {
    ROBOT_ACCESS_POINT(
        title = "Robot AP (Hotspot)",
        defaultIp = "192.168.4.1",
        defaultPort = 8765,
        description = "TurtleBot creates its own Wi-Fi Hotspot. Phone connects directly to the robot."
    ),
    PHONE_HOTSPOT(
        title = "Phone Hotspot",
        defaultIp = "192.168.43.1",
        defaultPort = 8765,
        description = "Phone enables Wi-Fi Hotspot. Raspberry Pi connects to your phone's network."
    ),
    ROUTER_LAN(
        title = "Shared Wi-Fi Router",
        defaultIp = "192.168.1.50",
        defaultPort = 8765,
        description = "Both phone and Raspberry Pi connect to the same local home/lab Wi-Fi router."
    )
}

data class JoystickState(
    val normX: Float = 0f,
    val normY: Float = 0f,
    val magnitude: Float = 0f,
    val angleDegrees: Float = 0f,
    val isEngaged: Boolean = false
)
