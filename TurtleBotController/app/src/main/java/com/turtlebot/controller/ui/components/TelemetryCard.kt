package com.turtlebot.controller.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.turtlebot.controller.data.model.RobotTelemetry
import com.turtlebot.controller.ui.theme.CyanGlow
import com.turtlebot.controller.ui.theme.CyanPrimary
import com.turtlebot.controller.ui.theme.DarkBorder
import com.turtlebot.controller.ui.theme.DarkSurface
import com.turtlebot.controller.ui.theme.DarkSurfaceVariant
import com.turtlebot.controller.ui.theme.ElectricBlue
import com.turtlebot.controller.ui.theme.EmergencyRed
import com.turtlebot.controller.ui.theme.SuccessGreen
import com.turtlebot.controller.ui.theme.TextMuted
import com.turtlebot.controller.ui.theme.TextPrimary
import com.turtlebot.controller.ui.theme.TextSecondary
import com.turtlebot.controller.ui.theme.WarningAmber
import java.util.Locale

@Composable
fun TelemetryOverview(
    telemetry: RobotTelemetry?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Linear Velocity Card
        TelemetryCardItem(
            icon = Icons.Default.Speed,
            iconTint = CyanPrimary,
            title = "LINEAR VEL",
            value = if (telemetry != null) String.format(Locale.US, "%.2f", telemetry.linearVelocity) else "--",
            unit = "m/s",
            isLive = telemetry != null,
            accentColor = CyanPrimary,
            modifier = Modifier.weight(1f)
        )

        // 2. Angular Velocity Card
        TelemetryCardItem(
            icon = Icons.AutoMirrored.Filled.RotateRight,
            iconTint = ElectricBlue,
            title = "ANGULAR VEL",
            value = if (telemetry != null) String.format(Locale.US, "%.2f", telemetry.angularVelocity) else "--",
            unit = "rad/s",
            isLive = telemetry != null,
            accentColor = ElectricBlue,
            modifier = Modifier.weight(1f)
        )

        // 3. Battery Card with Color Thresholds
        val batteryColor = when {
            telemetry == null -> TextMuted
            telemetry.batteryVoltage < 11.1 -> EmergencyRed
            telemetry.batteryVoltage < 11.5 -> WarningAmber
            else -> SuccessGreen
        }

        TelemetryCardItem(
            icon = Icons.Default.Bolt,
            iconTint = batteryColor,
            title = "BATTERY",
            value = if (telemetry != null) String.format(Locale.US, "%.1f", telemetry.batteryVoltage) else "--",
            unit = "V",
            isLive = telemetry != null,
            accentColor = batteryColor,
            valueColor = batteryColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TelemetryCardItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    unit: String,
    isLive: Boolean,
    accentColor: Color,
    valueColor: Color = TextPrimary,
    modifier: Modifier = Modifier
) {
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isLive) accentColor.copy(alpha = 0.5f) else DarkBorder,
        label = "card_border"
    )

    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(DarkSurfaceVariant, DarkSurface)
                ),
                RoundedCornerShape(14.dp)
            )
            .border(1.dp, animatedBorderColor, RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header with Icon and Label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    color = TextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Metric Readout
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = value,
                    color = valueColor,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = unit,
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Subtle Status Glow Line
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(2.dp)
                    .background(
                        if (isLive) accentColor.copy(alpha = 0.8f) else DarkBorder,
                        RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}
