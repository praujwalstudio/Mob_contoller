package com.turtlebot.controller.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.turtlebot.controller.data.model.ConnectionState
import com.turtlebot.controller.data.model.NetworkTopologyPreset
import com.turtlebot.controller.ui.components.ConnectionBadge
import com.turtlebot.controller.ui.theme.CyanPrimary
import com.turtlebot.controller.ui.theme.DarkBackground
import com.turtlebot.controller.ui.theme.DarkBorder
import com.turtlebot.controller.ui.theme.DarkSurface
import com.turtlebot.controller.ui.theme.DarkSurfaceHighlight
import com.turtlebot.controller.ui.theme.DarkSurfaceVariant
import com.turtlebot.controller.ui.theme.EmergencyRed
import com.turtlebot.controller.ui.theme.SuccessGreen
import com.turtlebot.controller.ui.theme.TextMuted
import com.turtlebot.controller.ui.theme.TextPrimary
import com.turtlebot.controller.ui.theme.TextSecondary
import com.turtlebot.controller.ui.theme.WarningAmber
import com.turtlebot.controller.viewmodel.MainViewModel

@Composable
fun ConnectionSettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val settings = viewModel.settingsRepository

    var hostInput by remember { mutableStateOf(settings.getHost()) }
    var portInput by remember { mutableStateOf(settings.getPort().toString()) }
    var saveStatus by remember { mutableStateOf("") }
    var pingStatus by remember { mutableStateOf<String?>(null) }
    var isPinging by remember { mutableStateOf(false) }
    var selectedGuideTab by remember { mutableIntStateOf(0) }

    val isConnected = connectionState is ConnectionState.Connected
    val isConnecting = connectionState is ConnectionState.Connecting

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "NETWORK & ROBOT CONNECTION",
            color = CyanPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Text(
            text = "Configure Wi-Fi Router, Phone Hotspot, or Robot AP Network Links",
            color = TextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Connection Badge
        ConnectionBadge(connectionState = connectionState)

        Spacer(modifier = Modifier.height(14.dp))

        // ONE-TAP NETWORK TOPOLOGY PRESETS
        Text(
            text = "ONE-TAP CONNECTION PRESETS",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PresetButton(
                title = "ROUTER LAN",
                icon = Icons.Default.Router,
                isActive = hostInput == "192.168.1.50",
                onClick = {
                    viewModel.applyPreset(NetworkTopologyPreset.ROUTER_LAN)
                    hostInput = settings.getHost()
                    portInput = settings.getPort().toString()
                    saveStatus = "Switched to Router LAN preset"
                },
                modifier = Modifier.weight(1f)
            )

            PresetButton(
                title = "PHONE HOTSPOT",
                icon = Icons.Default.WifiTethering,
                isActive = hostInput == "192.168.43.1",
                onClick = {
                    viewModel.applyPreset(NetworkTopologyPreset.PHONE_HOTSPOT)
                    hostInput = settings.getHost()
                    portInput = settings.getPort().toString()
                    saveStatus = "Switched to Phone Hotspot preset"
                },
                modifier = Modifier.weight(1.1f)
            )

            PresetButton(
                title = "ROBOT AP",
                icon = Icons.Default.Wifi,
                isActive = hostInput == "192.168.4.1",
                onClick = {
                    viewModel.applyPreset(NetworkTopologyPreset.ROBOT_ACCESS_POINT)
                    hostInput = settings.getHost()
                    portInput = settings.getPort().toString()
                    saveStatus = "Switched to Robot AP preset"
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Manual Configuration Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(12.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            // IP Input
            OutlinedTextField(
                value = hostInput,
                onValueChange = {
                    hostInput = it
                    settings.setHost(it)
                    saveStatus = "Saved automatically"
                },
                label = { Text("Raspberry Pi IP Address") },
                placeholder = { Text("e.g. 192.168.1.50 or 192.168.4.1") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanPrimary,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = CyanPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Port Input
            OutlinedTextField(
                value = portInput,
                onValueChange = {
                    portInput = it
                    val portNum = it.toIntOrNull()
                    if (portNum != null && portNum in 1..65535) {
                        settings.setPort(portNum)
                        saveStatus = "Saved automatically"
                    }
                },
                label = { Text("WebSocket Port (Default: 8765)") },
                placeholder = { Text("8765") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanPrimary,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = CyanPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (saveStatus.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = saveStatus,
                    color = SuccessGreen,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (isConnected) {
                            viewModel.disconnect()
                        } else {
                            viewModel.connect()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) EmergencyRed else CyanPrimary,
                        contentColor = if (isConnected) TextPrimary else DarkBackground
                    ),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isConnecting,
                    modifier = Modifier.weight(1.2f)
                ) {
                    Text(
                        text = if (isConnected) "DISCONNECT" else if (isConnecting) "CONNECTING..." else "CONNECT",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = {
                        isPinging = true
                        pingStatus = "Testing link..."
                        val portNum = portInput.toIntOrNull() ?: 8765
                        viewModel.testReachability(hostInput, portNum) { success, msg ->
                            isPinging = false
                            pingStatus = msg
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurfaceHighlight,
                        contentColor = CyanPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isPinging,
                    modifier = Modifier
                        .weight(1.1f)
                        .border(1.dp, CyanPrimary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                ) {
                    Text(
                        text = if (isPinging) "PINGING..." else "TEST LINK",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = {
                        viewModel.disconnect()
                        viewModel.connect()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurfaceVariant,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isConnecting,
                    modifier = Modifier.weight(0.7f)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(text = "RETRY", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
            }

            if (pingStatus != null) {
                Spacer(modifier = Modifier.height(10.dp))
                val isSuccess = pingStatus!!.startsWith("Reachable")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSuccess) SuccessGreen.copy(alpha = 0.15f) else EmergencyRed.copy(alpha = 0.15f),
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            if (isSuccess) SuccessGreen.copy(alpha = 0.6f) else EmergencyRed.copy(alpha = 0.6f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = pingStatus!!,
                        color = if (isSuccess) SuccessGreen else EmergencyRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // INTERACTIVE WI-FI GUIDANCE TABS
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(12.dp))
                .border(1.dp, CyanPrimary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GuideTabPill("OPTION A: ROUTER LAN", selected = selectedGuideTab == 0) { selectedGuideTab = 0 }
                GuideTabPill("OPTION B: HOTSPOT", selected = selectedGuideTab == 1) { selectedGuideTab = 1 }
                GuideTabPill("ANDROID TIPS", selected = selectedGuideTab == 2) { selectedGuideTab = 2 }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedGuideTab) {
                0 -> {
                    Text(
                        text = "OPTION A: SHARED WI-FI ROUTER (RECOMMENDED FOR LAB/DEV)\n\n" +
                                "1. Connect your Raspberry Pi 3 to your local Wi-Fi router.\n" +
                                "2. Connect your smartphone to the exact same Wi-Fi router.\n" +
                                "3. On the Raspberry Pi terminal, run 'hostname -I' to find its IP address (e.g., 192.168.1.50).\n" +
                                "4. Enter that IP address above and tap CONNECT.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
                1 -> {
                    Text(
                        text = "OPTION B: PHONE HOTSPOT (FOR OUTDOOR / FIELD USE)\n\n" +
                                "1. On your smartphone, turn ON Personal Hotspot.\n" +
                                "2. On your Raspberry Pi, connect to your phone's Wi-Fi hotspot.\n" +
                                "3. Check the phone hotspot settings to see the connected Pi's IP, or tap 'PHONE HOTSPOT' preset (192.168.43.1).\n" +
                                "4. Tap CONNECT to link the robot directly to your phone.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
                2 -> {
                    Text(
                        text = "IMPORTANT ANDROID SMARTPHONE TIPS:\n\n" +
                                "• 'Wi-Fi Has No Internet' Prompt: Modern Android phones show a notification when connecting to a robot without internet. Tap 'Always Stay Connected' or 'Don't Ask Again'.\n" +
                                "• Battery Saver / Task Killer: Ensure battery saver is disabled or set to 'Unrestricted' for this app so the OS does not close background Wi-Fi sockets during motion.",
                        color = WarningAmber,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetButton(
    title: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(if (isActive) CyanPrimary.copy(alpha = 0.2f) else DarkSurfaceVariant, RoundedCornerShape(8.dp))
            .border(1.dp, if (isActive) CyanPrimary else DarkBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isActive) CyanPrimary else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                color = if (isActive) CyanPrimary else TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun GuideTabPill(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(if (selected) CyanPrimary else DarkSurfaceVariant, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            color = if (selected) DarkBackground else TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
