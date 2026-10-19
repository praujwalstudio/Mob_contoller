package com.turtlebot.controller.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.turtlebot.controller.data.model.ConnectionState
import com.turtlebot.controller.data.model.ControlMode
import com.turtlebot.controller.data.repository.SettingsRepository
import com.turtlebot.controller.ui.components.ConnectionBadge
import com.turtlebot.controller.ui.components.DirectionalPad
import com.turtlebot.controller.ui.components.EmergencyStopButton
import com.turtlebot.controller.ui.components.MovementStateBadge
import com.turtlebot.controller.ui.components.TelemetryOverview
import com.turtlebot.controller.ui.components.UniversalJoystick
import com.turtlebot.controller.ui.theme.CyanGlow
import com.turtlebot.controller.ui.theme.CyanPrimary
import com.turtlebot.controller.ui.theme.CyanSecondary
import com.turtlebot.controller.ui.theme.DarkBackground
import com.turtlebot.controller.ui.theme.DarkBorder
import com.turtlebot.controller.ui.theme.DarkSurface
import com.turtlebot.controller.ui.theme.DarkSurfaceHighlight
import com.turtlebot.controller.ui.theme.DarkSurfaceVariant
import com.turtlebot.controller.ui.theme.ElectricBlue
import com.turtlebot.controller.ui.theme.EmergencyRed
import com.turtlebot.controller.ui.theme.SuccessGreen
import com.turtlebot.controller.ui.theme.TextMuted
import com.turtlebot.controller.ui.theme.TextPrimary
import com.turtlebot.controller.ui.theme.TextSecondary
import com.turtlebot.controller.ui.theme.WarningAmber
import com.turtlebot.controller.viewmodel.MainViewModel
import java.util.Locale

@Composable
fun MainControllerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val currentDirection by viewModel.currentDirection.collectAsState()
    val isEmergencyStopped by viewModel.isEmergencyStopped.collectAsState()
    val controlMode by viewModel.controlMode.collectAsState()
    val linearSpeed by viewModel.linearSpeed.collectAsState()
    val angularSpeed by viewModel.angularSpeed.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val displayError = errorMessage ?: (connectionState as? ConnectionState.Error)?.message

    val isConnected = connectionState is ConnectionState.Connected
    val isConnecting = connectionState is ConnectionState.Connecting

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        val isWideOrTablet = maxWidth >= 600.dp || (maxWidth > maxHeight && maxWidth >= 480.dp)

        if (isWideOrTablet) {
            // =========================================================================
            // TABLET & LANDSCAPE ADAPTIVE TWO-PANE LAYOUT
            // =========================================================================
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // LEFT PANE: JOYSTICK / D-PAD CONTROLLER & E-STOP
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    ModeToggleRow(
                        controlMode = controlMode,
                        onModeChanged = { viewModel.setControlMode(it) }
                    )

                    MovementStateBadge(
                        direction = currentDirection,
                        isEmergencyStopped = isEmergencyStopped
                    )

                    if (controlMode == ControlMode.ANALOG_JOYSTICK) {
                        UniversalJoystick(
                            onJoystickMoved = { x, y, mag -> viewModel.onJoystickMoved(x, y, mag) },
                            onJoystickReleased = { viewModel.onJoystickReleased() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                        EmergencyStopButton(
                            onStopClicked = { viewModel.triggerEmergencyStop() },
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    } else {
                        DirectionalPad(
                            currentDirection = currentDirection,
                            onDirectionPressed = { viewModel.onDirectionPressed(it) },
                            onDirectionReleased = { viewModel.onDirectionReleased() },
                            onEmergencyStop = { viewModel.triggerEmergencyStop() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // RIGHT PANE: TELEMETRY & SPEED GAUGES
                Column(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HeaderBar(
                        isConnected = isConnected,
                        isConnecting = isConnecting,
                        onConnectClicked = {
                            if (isConnected) viewModel.disconnect() else viewModel.connect()
                        }
                    )

                    ConnectionBadge(connectionState = connectionState)

                    TelemetryOverview(telemetry = telemetry)

                    if (displayError != null) {
                        ErrorAlert(errorMessage = displayError)
                    }

                    SpeedControlSection(
                        linearSpeed = linearSpeed,
                        angularSpeed = angularSpeed,
                        onLinearSpeedChanged = { viewModel.updateLinearSpeed(it) },
                        onAngularSpeedChanged = { viewModel.updateAngularSpeed(it) }
                    )

                    OperatorGuidanceCard()
                }
            }
        } else {
            // =========================================================================
            // SMARTPHONE PORTRAIT LAYOUT
            // =========================================================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Cockpit Bar
                HeaderBar(
                    isConnected = isConnected,
                    isConnecting = isConnecting,
                    onConnectClicked = {
                        if (isConnected) viewModel.disconnect() else viewModel.connect()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                ConnectionBadge(connectionState = connectionState)

                Spacer(modifier = Modifier.height(10.dp))

                TelemetryOverview(telemetry = telemetry)

                if (displayError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ErrorAlert(errorMessage = displayError)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mode Toggle (Joystick vs D-Pad)
                ModeToggleRow(
                    controlMode = controlMode,
                    onModeChanged = { viewModel.setControlMode(it) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                MovementStateBadge(
                    direction = currentDirection,
                    isEmergencyStopped = isEmergencyStopped
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Controller Center
                if (controlMode == ControlMode.ANALOG_JOYSTICK) {
                    UniversalJoystick(
                        onJoystickMoved = { x, y, mag -> viewModel.onJoystickMoved(x, y, mag) },
                        onJoystickReleased = { viewModel.onJoystickReleased() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(255.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Prominent Central Stop Button with Pulsing Safety Halo
                    EmergencyStopButton(
                        onStopClicked = { viewModel.triggerEmergencyStop() }
                    )
                } else {
                    DirectionalPad(
                        currentDirection = currentDirection,
                        onDirectionPressed = { viewModel.onDirectionPressed(it) },
                        onDirectionReleased = { viewModel.onDirectionReleased() },
                        onEmergencyStop = { viewModel.triggerEmergencyStop() },
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                SpeedControlSection(
                    linearSpeed = linearSpeed,
                    angularSpeed = angularSpeed,
                    onLinearSpeedChanged = { viewModel.updateLinearSpeed(it) },
                    onAngularSpeedChanged = { viewModel.updateAngularSpeed(it) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                OperatorGuidanceCard()

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun HeaderBar(
    isConnected: Boolean,
    isConnecting: Boolean,
    onConnectClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(DarkSurfaceVariant, DarkSurface)
                ),
                RoundedCornerShape(16.dp)
            )
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .background(DarkSurfaceHighlight, CircleShape)
                    .border(1.dp, CyanPrimary.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.PrecisionManufacturing,
                    contentDescription = "Robot",
                    tint = CyanPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = "TURTLEBOT 3",
                    color = CyanPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "UNIVERSAL ROBOT CONTROLLER",
                    color = TextSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Button(
            onClick = onConnectClicked,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isConnected) DarkSurfaceHighlight else CyanPrimary,
                contentColor = if (isConnected) EmergencyRed else DarkBackground
            ),
            shape = RoundedCornerShape(12.dp),
            enabled = !isConnecting,
            modifier = Modifier
                .border(
                    1.dp,
                    if (isConnected) EmergencyRed.copy(alpha = 0.6f) else CyanPrimary,
                    RoundedCornerShape(12.dp)
                )
        ) {
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = "Power",
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.size(5.dp))
            Text(
                text = if (isConnected) "DISCONNECT" else if (isConnecting) "WAIT..." else "CONNECT",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun ModeToggleRow(
    controlMode: ControlMode,
    onModeChanged: (ControlMode) -> Unit
) {
    Row(
        modifier = Modifier
            .background(DarkSurface, RoundedCornerShape(24.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(24.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        val isJoystick = controlMode == ControlMode.ANALOG_JOYSTICK

        Button(
            onClick = { onModeChanged(ControlMode.ANALOG_JOYSTICK) },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isJoystick) CyanPrimary else Color.Transparent,
                contentColor = if (isJoystick) DarkBackground else TextMuted
            ),
            shape = RoundedCornerShape(20.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = if (isJoystick) 4.dp else 0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Gamepad,
                contentDescription = "Joystick",
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "360° JOYSTICK",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Button(
            onClick = { onModeChanged(ControlMode.TACTILE_DPAD) },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (!isJoystick) CyanPrimary else Color.Transparent,
                contentColor = if (!isJoystick) DarkBackground else TextMuted
            ),
            shape = RoundedCornerShape(20.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = if (!isJoystick) 4.dp else 0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = "D-Pad",
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "TACTILE D-PAD",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun ErrorAlert(errorMessage: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(EmergencyRed.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .border(1.dp, EmergencyRed.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Alert",
                tint = EmergencyRed,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = errorMessage,
                color = EmergencyRed,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun SpeedControlSection(
    linearSpeed: Float,
    angularSpeed: Float,
    onLinearSpeedChanged: (Float) -> Unit,
    onAngularSpeedChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(DarkSurfaceVariant, DarkSurface)
                ),
                RoundedCornerShape(16.dp)
            )
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "Speed Limits",
                    tint = CyanPrimary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SAFETY SPEED LIMITS",
                    color = CyanPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick-Speed Preset Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SpeedPresetChip(
                label = "ECO",
                sublabel = "0.04 m/s",
                isSelected = (linearSpeed in 0.03f..0.05f),
                onClick = {
                    onLinearSpeedChanged(0.04f)
                    onAngularSpeedChanged(0.30f)
                },
                modifier = Modifier.weight(1f)
            )

            SpeedPresetChip(
                label = "NORMAL",
                sublabel = "0.08 m/s",
                isSelected = (linearSpeed in 0.07f..0.09f),
                onClick = {
                    onLinearSpeedChanged(0.08f)
                    onAngularSpeedChanged(0.50f)
                },
                modifier = Modifier.weight(1f)
            )

            SpeedPresetChip(
                label = "SPORT",
                sublabel = "0.15 m/s",
                isSelected = (linearSpeed in 0.14f..0.16f),
                onClick = {
                    onLinearSpeedChanged(0.15f)
                    onAngularSpeedChanged(1.00f)
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Fine-Tune Linear Speed Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Max Linear Speed",
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = String.format(Locale.US, "%.2f m/s", linearSpeed),
                color = CyanPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }
        Slider(
            value = linearSpeed,
            onValueChange = onLinearSpeedChanged,
            valueRange = 0.02f..SettingsRepository.HARD_MAX_LINEAR,
            colors = SliderDefaults.colors(
                thumbColor = CyanPrimary,
                activeTrackColor = CyanPrimary,
                inactiveTrackColor = DarkBorder
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Fine-Tune Angular Speed Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Max Angular Speed",
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = String.format(Locale.US, "%.2f rad/s", angularSpeed),
                color = ElectricBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }
        Slider(
            value = angularSpeed,
            onValueChange = onAngularSpeedChanged,
            valueRange = 0.10f..SettingsRepository.HARD_MAX_ANGULAR,
            colors = SliderDefaults.colors(
                thumbColor = ElectricBlue,
                activeTrackColor = ElectricBlue,
                inactiveTrackColor = DarkBorder
            )
        )
    }
}

@Composable
private fun SpeedPresetChip(
    label: String,
    sublabel: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) CyanPrimary else DarkBorder,
        label = "chip_border"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) DarkSurfaceHighlight else DarkSurface
            )
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = if (isSelected) CyanPrimary else TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = sublabel,
                color = if (isSelected) CyanSecondary else TextMuted,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun OperatorGuidanceCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Tips",
                tint = CyanSecondary,
                modifier = Modifier
                    .size(16.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "OPERATOR QUICK TIPS",
                    color = CyanSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "• Drag thumbstick in any direction to steer robot smoothly.\n• Release thumb to automatically brake to neutral (0 m/s).\n• Tap E-STOP anytime to trigger immediate zero-velocity halt.",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
