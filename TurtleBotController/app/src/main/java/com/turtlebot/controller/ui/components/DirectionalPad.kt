package com.turtlebot.controller.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.turtlebot.controller.data.model.MovementDirection
import com.turtlebot.controller.ui.theme.CyanGlow
import com.turtlebot.controller.ui.theme.CyanPrimary
import com.turtlebot.controller.ui.theme.DarkBorder
import com.turtlebot.controller.ui.theme.DarkSurface
import com.turtlebot.controller.ui.theme.DarkSurfaceHighlight
import com.turtlebot.controller.ui.theme.DarkSurfaceVariant
import com.turtlebot.controller.ui.theme.TextPrimary

@Composable
fun DirectionalPad(
    currentDirection: MovementDirection,
    onDirectionPressed: (MovementDirection) -> Unit,
    onDirectionReleased: () -> Unit,
    onEmergencyStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        // TOP: FORWARD
        DirectionTouchButton(
            label = "FORWARD",
            direction = MovementDirection.FORWARD,
            icon = Icons.Default.KeyboardArrowUp,
            isActive = currentDirection == MovementDirection.FORWARD,
            onPressed = { onDirectionPressed(MovementDirection.FORWARD) },
            onReleased = onDirectionReleased,
            modifier = Modifier.size(width = 110.dp, height = 70.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // MIDDLE: LEFT - STOP - RIGHT
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // LEFT
            DirectionTouchButton(
                label = "LEFT",
                direction = MovementDirection.LEFT,
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                isActive = currentDirection == MovementDirection.LEFT,
                onPressed = { onDirectionPressed(MovementDirection.LEFT) },
                onReleased = onDirectionReleased,
                modifier = Modifier.size(width = 85.dp, height = 85.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // CENTRAL EMERGENCY STOP
            EmergencyStopButton(
                onStopClicked = onEmergencyStop
            )

            Spacer(modifier = Modifier.width(10.dp))

            // RIGHT
            DirectionTouchButton(
                label = "RIGHT",
                direction = MovementDirection.RIGHT,
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                isActive = currentDirection == MovementDirection.RIGHT,
                onPressed = { onDirectionPressed(MovementDirection.RIGHT) },
                onReleased = onDirectionReleased,
                modifier = Modifier.size(width = 85.dp, height = 85.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // BOTTOM: BACKWARD
        DirectionTouchButton(
            label = "BACKWARD",
            direction = MovementDirection.BACKWARD,
            icon = Icons.Default.KeyboardArrowDown,
            isActive = currentDirection == MovementDirection.BACKWARD,
            onPressed = { onDirectionPressed(MovementDirection.BACKWARD) },
            onReleased = onDirectionReleased,
            modifier = Modifier.size(width = 110.dp, height = 70.dp)
        )
    }
}

@Composable
private fun DirectionTouchButton(
    label: String,
    direction: MovementDirection,
    icon: ImageVector,
    isActive: Boolean,
    onPressed: () -> Unit,
    onReleased: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var isPressedLocal by remember { mutableStateOf(false) }

    val activeState = isActive || isPressedLocal
    val scale by animateFloatAsState(if (activeState) 0.94f else 1.0f, label = "pad_scale")

    val targetBackground = if (activeState) {
        listOf(CyanPrimary.copy(alpha = 0.35f), DarkSurfaceHighlight)
    } else {
        listOf(DarkSurfaceVariant, DarkSurface)
    }

    val animatedBorderColor by animateColorAsState(
        targetValue = if (activeState) CyanPrimary else DarkBorder,
        label = "border_color"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(targetBackground))
            .border(1.5.dp, animatedBorderColor, RoundedCornerShape(16.dp))
            .pointerInput(direction) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isPressedLocal = true
                    down.consume()
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onPressed()

                    val up = waitForUpOrCancellation()
                    isPressedLocal = false
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY_RELEASE)
                    onReleased()
                    up?.consume()
                }
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (activeState) CyanPrimary else TextPrimary,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = label,
                color = if (activeState) CyanPrimary else TextPrimary.copy(alpha = 0.8f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.8.sp
            )
        }
    }
}
