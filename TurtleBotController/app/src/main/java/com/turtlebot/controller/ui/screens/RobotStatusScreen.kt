package com.turtlebot.controller.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.turtlebot.controller.data.model.ConnectionState
import com.turtlebot.controller.ui.components.ConnectionBadge
import com.turtlebot.controller.ui.theme.CyanPrimary
import com.turtlebot.controller.ui.theme.DarkBackground
import com.turtlebot.controller.ui.theme.DarkBorder
import com.turtlebot.controller.ui.theme.DarkSurface
import com.turtlebot.controller.ui.theme.EmergencyRed
import com.turtlebot.controller.ui.theme.SuccessGreen
import com.turtlebot.controller.ui.theme.TextMuted
import com.turtlebot.controller.ui.theme.TextPrimary
import com.turtlebot.controller.ui.theme.TextSecondary
import com.turtlebot.controller.ui.theme.WarningAmber
import com.turtlebot.controller.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RobotStatusScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val lastAck by viewModel.lastAck.collectAsState()
    val isEmergencyStopped by viewModel.isEmergencyStopped.collectAsState()
    val isConnected = connectionState is ConnectionState.Connected

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "ROBOT DIAGNOSTICS & TELEMETRY",
            color = CyanPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Text(
            text = "Real-time Telemetry, Motor Controller State, and Safety Logs",
            color = TextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Connection Badge
        ConnectionBadge(connectionState = connectionState)

        Spacer(modifier = Modifier.height(16.dp))

        // Hardware Telemetry Section
        StatusCard(title = "PHYSICAL ROBOT TELEMETRY") {
            if (isConnected && telemetry != null) {
                StatusRow("Telemetry Stream", "ACTIVE (10 Hz)", SuccessGreen)
                StatusRow(
                    "Linear Velocity",
                    String.format(Locale.US, "%.3f m/s", telemetry!!.linearVelocity),
                    TextPrimary
                )
                StatusRow(
                    "Angular Velocity",
                    String.format(Locale.US, "%.3f rad/s", telemetry!!.angularVelocity),
                    TextPrimary
                )
                StatusRow(
                    "Battery Voltage",
                    String.format(Locale.US, "%.2f V", telemetry!!.batteryVoltage),
                    if (telemetry!!.batteryVoltage < 11.1) EmergencyRed else SuccessGreen
                )
                StatusRow(
                    "Motor State",
                    if (telemetry!!.isStopped) "STOPPED / IDLE" else "ACTIVE IN MOTION",
                    if (telemetry!!.isStopped) SuccessGreen else CyanPrimary
                )
                StatusRow(
                    "Command Lease Active",
                    if (telemetry!!.hasActiveLease) "YES (Movement Authorized)" else "NO (Motors Locked)",
                    if (telemetry!!.hasActiveLease) CyanPrimary else WarningAmber
                )
                StatusRow(
                    "Telemetry Timestamp",
                    formatTimestamp(telemetry!!.timestamp),
                    TextSecondary
                )
            } else {
                Text(
                    text = "TELEMETRY UNAVAILABLE\nConnect to TurtleBot Wi-Fi to receive live telemetry from OpenCR.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Last Command & Acknowledgement
        StatusCard(title = "COMMAND ACKNOWLEDGEMENT LOG") {
            if (lastAck != null) {
                StatusRow("Last Command ID", lastAck!!.commandId, TextPrimary)
                StatusRow(
                    "Bridge Status",
                    lastAck!!.status.uppercase(Locale.US),
                    if (lastAck!!.status == "ok") SuccessGreen else EmergencyRed
                )
                if (lastAck!!.reason.isNotEmpty()) {
                    StatusRow("Response Detail", lastAck!!.reason, TextSecondary)
                }
                StatusRow("Ack Timestamp", formatTimestamp(lastAck!!.timestamp), TextSecondary)
            } else {
                Text(
                    text = "No command acknowledgement received yet.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Safety Architecture State
        StatusCard(title = "FAIL-SAFE SYSTEM INTEGRITY") {
            StatusRow(
                "Software E-STOP Latch",
                if (isEmergencyStopped) "ENGAGED" else "DISENGAGED",
                if (isEmergencyStopped) EmergencyRed else SuccessGreen
            )
            StatusRow("Dead-Man Switch Interval", "120 ms continuous pulse", CyanPrimary)
            StatusRow("Bridge Watchdog Timeout", "400 ms lease expiration", CyanPrimary)
            StatusRow("Client Disconnect Action", "Instant 3x Zero-Velocity Stop", SuccessGreen)
            StatusRow("ROS Topic", "/cmd_vel (geometry_msgs/Twist)", TextSecondary)
            StatusRow("Controller Hardware", "OpenCR 1.0 (Dynamixel XL430)", TextSecondary)
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(12.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            color = CyanPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun formatTimestamp(timestampSec: Double): String {
    if (timestampSec <= 0.0) return "N/A"
    val date = Date((timestampSec * 1000).toLong())
    val format = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    return format.format(date)
}
