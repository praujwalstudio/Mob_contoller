package com.turtlebot.controller.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.turtlebot.controller.data.model.ConnectionState
import com.turtlebot.controller.data.model.MovementDirection
import com.turtlebot.controller.ui.theme.CyanGlow
import com.turtlebot.controller.ui.theme.CyanPrimary
import com.turtlebot.controller.ui.theme.DarkBorder
import com.turtlebot.controller.ui.theme.DarkSurface
import com.turtlebot.controller.ui.theme.DarkSurfaceVariant
import com.turtlebot.controller.ui.theme.EmergencyRed
import com.turtlebot.controller.ui.theme.EmergencyRedGlow
import com.turtlebot.controller.ui.theme.SuccessGreen
import com.turtlebot.controller.ui.theme.SuccessGreenGlow
import com.turtlebot.controller.ui.theme.TextMuted
import com.turtlebot.controller.ui.theme.TextPrimary
import com.turtlebot.controller.ui.theme.WarningAmber
import com.turtlebot.controller.ui.theme.WarningAmberGlow

@Composable
fun ConnectionBadge(connectionState: ConnectionState, modifier: Modifier = Modifier) {
    val (statusText, dotColor, glowColor) = when (connectionState) {
        is ConnectionState.Connected -> Triple("ONLINE • ${connectionState.host}", SuccessGreen, SuccessGreenGlow)
        is ConnectionState.Connecting -> Triple("CONNECTING...", WarningAmber, WarningAmberGlow)
        is ConnectionState.Disconnected -> Triple("OFFLINE / DISCONNECTED", TextMuted, Color.Transparent)
        is ConnectionState.Error -> Triple("LINK ERROR", EmergencyRed, EmergencyRedGlow)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_pulse"
    )

    Box(
        modifier = modifier
            .background(
                Brush.horizontalGradient(
                    listOf(DarkSurfaceVariant, DarkSurface)
                ),
                RoundedCornerShape(24.dp)
            )
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(dotColor.copy(alpha = 0.6f), dotColor.copy(alpha = 0.2f))
                ),
                RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(12.dp)) {
                if (connectionState is ConnectionState.Connected || connectionState is ConnectionState.Connecting) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .scale(pulseScale)
                            .background(glowColor, CircleShape)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(dotColor, CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = statusText,
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.8.sp
            )
        }
    }
}

@Composable
fun MovementStateBadge(
    direction: MovementDirection,
    isEmergencyStopped: Boolean,
    modifier: Modifier = Modifier
) {
    val (label, targetColor, glowColor) = when {
        isEmergencyStopped -> Triple("⚠ EMERGENCY HALTED", EmergencyRed, EmergencyRedGlow)
        direction == MovementDirection.FORWARD -> Triple("▲ ADVANCING", CyanPrimary, CyanGlow)
        direction == MovementDirection.BACKWARD -> Triple("▼ REVERSING", CyanPrimary, CyanGlow)
        direction == MovementDirection.LEFT -> Triple("◀ PIVOT LEFT", CyanPrimary, CyanGlow)
        direction == MovementDirection.RIGHT -> Triple("▶ PIVOT RIGHT", CyanPrimary, CyanGlow)
        else -> Triple("■ STANDBY / BRAKED", SuccessGreen, SuccessGreenGlow)
    }

    val animatedColor by animateColorAsState(targetValue = targetColor, label = "badge_color")

    Box(
        modifier = modifier
            .background(
                Brush.horizontalGradient(
                    listOf(glowColor.copy(alpha = 0.25f), DarkSurfaceVariant)
                ),
                RoundedCornerShape(12.dp)
            )
            .border(1.dp, animatedColor.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = animatedColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.2.sp
        )
    }
}
