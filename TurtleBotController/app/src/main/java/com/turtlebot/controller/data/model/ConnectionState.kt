package com.turtlebot.controller.data.model

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data class Connecting(val host: String, val port: Int) : ConnectionState
    data class Connected(val host: String, val port: Int) : ConnectionState
    data class Error(val message: String) : ConnectionState
}
