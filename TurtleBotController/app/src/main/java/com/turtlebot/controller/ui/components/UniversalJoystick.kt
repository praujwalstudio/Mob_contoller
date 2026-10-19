package com.turtlebot.controller.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.turtlebot.controller.ui.theme.CyanGlow
import com.turtlebot.controller.ui.theme.CyanPrimary
import com.turtlebot.controller.ui.theme.CyanSecondary
import com.turtlebot.controller.ui.theme.DarkBackground
import com.turtlebot.controller.ui.theme.DarkBorder
import com.turtlebot.controller.ui.theme.DarkSurface
import com.turtlebot.controller.ui.theme.DarkSurfaceHighlight
import com.turtlebot.controller.ui.theme.DarkSurfaceVariant
import com.turtlebot.controller.ui.theme.ElectricBlue
import com.turtlebot.controller.ui.theme.SuccessGreen
import com.turtlebot.controller.ui.theme.TextMuted
import com.turtlebot.controller.ui.theme.TextPrimary
import com.turtlebot.controller.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun UniversalJoystick(
    onJoystickMoved: (normX: Float, normY: Float, magnitude: Float) -> Unit,
    onJoystickReleased: () -> Unit,
    modifier: Modifier = Modifier,
    deadzoneRatio: Float = 0.08f,
    maxRadiusDp: Dp? = null
) {
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Animatable thumbstick offsets for instantaneous spring return
    val thumbX = remember { Animatable(0f) }
    val thumbY = remember { Animatable(0f) }

    var isTouching by remember { mutableStateOf(false) }
    var currentMagnitude by remember { mutableFloatStateOf(0f) }
    var currentAngleDeg by remember { mutableFloatStateOf(0f) }
    var wasInDeadzone by remember { mutableStateOf(true) }

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        val availableSize = min(maxWidth, maxHeight)
        val joystickSize = if (maxRadiusDp != null) min(availableSize, maxRadiusDp * 2) else availableSize * 0.90f
        val radiusPx = with(density) { (joystickSize / 2).toPx() }
        val knobRadiusPx = radiusPx * 0.26f
        val deadzonePx = radiusPx * deadzoneRatio

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Interactive Joystick Surface
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(joystickSize)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                DarkSurfaceVariant.copy(alpha = 0.85f),
                                DarkSurface,
                                DarkBackground
                            )
                        )
                    )
                    .border(
                        2.dp,
                        Brush.radialGradient(
                            listOf(
                                if (isTouching) CyanPrimary.copy(alpha = 0.8f) else DarkBorder,
                                DarkBorder
                            )
                        ),
                        CircleShape
                    )
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            isTouching = true
                            down.consume()

                            // Initial touch calculation
                            val center = Offset(radiusPx, radiusPx)
                            var rawOffset = down.position - center
                            var dist = rawOffset.getDistance()

                            if (dist > radiusPx) {
                                rawOffset = rawOffset * (radiusPx / dist)
                                dist = radiusPx
                            }

                            coroutineScope.launch {
                                thumbX.snapTo(rawOffset.x)
                                thumbY.snapTo(rawOffset.y)
                            }

                            val normMag = dist / radiusPx
                            currentMagnitude = normMag

                            if (normMag > deadzoneRatio) {
                                if (wasInDeadzone) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    wasInDeadzone = false
                                }
                                val normX = rawOffset.x / radiusPx
                                val normY = -rawOffset.y / radiusPx
                                currentAngleDeg = ((Math.toDegrees(atan2(normY.toDouble(), normX.toDouble())) + 360) % 360).toFloat()
                                onJoystickMoved(normX, normY, normMag)
                            } else {
                                wasInDeadzone = true
                                onJoystickMoved(0f, 0f, 0f)
                            }

                            // Tracking loop
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break

                                if (!change.pressed) {
                                    break
                                }

                                change.consume()
                                var dragOffset = change.position - center
                                var dragDist = dragOffset.getDistance()

                                if (dragDist > radiusPx) {
                                    dragOffset = dragOffset * (radiusPx / dragDist)
                                    dragDist = radiusPx
                                }

                                coroutineScope.launch {
                                    thumbX.snapTo(dragOffset.x)
                                    thumbY.snapTo(dragOffset.y)
                                }

                                val moveMag = dragDist / radiusPx
                                currentMagnitude = moveMag

                                if (moveMag > deadzoneRatio) {
                                    if (wasInDeadzone) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        wasInDeadzone = false
                                    }
                                    val normX = dragOffset.x / radiusPx
                                    val normY = -dragOffset.y / radiusPx
                                    currentAngleDeg = ((Math.toDegrees(atan2(normY.toDouble(), normX.toDouble())) + 360) % 360).toFloat()
                                    onJoystickMoved(normX, normY, moveMag)
                                } else {
                                    wasInDeadzone = true
                                    onJoystickMoved(0f, 0f, 0f)
                                }
                            }

                            // Release: Spring Return to Neutral Zero
                            isTouching = false
                            currentMagnitude = 0f
                            wasInDeadzone = true

                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

                            coroutineScope.launch {
                                launch {
                                    thumbX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
                                launch {
                                    thumbY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
                            }

                            onJoystickReleased()
                        }
                    }
            ) {
                // HUD Radar Visuals and Vector Guide
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val thumbCenter = center + Offset(thumbX.value, thumbY.value)

                    // 1. Radar Concentric Graduated Rings (25%, 50%, 75%)
                    drawCircle(
                        color = DarkBorder.copy(alpha = 0.5f),
                        radius = radiusPx * 0.25f,
                        center = center,
                        style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                    )
                    drawCircle(
                        color = DarkBorder.copy(alpha = 0.6f),
                        radius = radiusPx * 0.50f,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = DarkBorder.copy(alpha = 0.6f),
                        radius = radiusPx * 0.75f,
                        center = center,
                        style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
                    )

                    // 2. Deadzone Ring
                    drawCircle(
                        color = if (isTouching && wasInDeadzone) CyanPrimary.copy(alpha = 0.4f) else DarkBorder.copy(alpha = 0.4f),
                        radius = deadzonePx,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // 3. Cardinal HUD Crosshairs with Degree Ticks
                    val crosshairAlpha = if (isTouching) 0.5f else 0.3f
                    // Horizontal
                    drawLine(
                        color = DarkBorder.copy(alpha = crosshairAlpha),
                        start = Offset(center.x - radiusPx * 0.95f, center.y),
                        end = Offset(center.x + radiusPx * 0.95f, center.y),
                        strokeWidth = 1.dp.toPx()
                    )
                    // Vertical
                    drawLine(
                        color = DarkBorder.copy(alpha = crosshairAlpha),
                        start = Offset(center.x, center.y - radiusPx * 0.95f),
                        end = Offset(center.x, center.y + radiusPx * 0.95f),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Diagonal 45-degree corner ticks
                    val diagLen = radiusPx * 0.92f
                    for (angle in listOf(45.0, 135.0, 225.0, 315.0)) {
                        val rad = Math.toRadians(angle)
                        val start = center + Offset((cos(rad) * (diagLen - 8.dp.toPx())).toFloat(), (sin(rad) * (diagLen - 8.dp.toPx())).toFloat())
                        val end = center + Offset((cos(rad) * diagLen).toFloat(), (sin(rad) * diagLen).toFloat())
                        drawLine(
                            color = DarkBorder.copy(alpha = 0.4f),
                            start = start,
                            end = end,
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }

                    // 4. Dynamic Vector Beam (Center to Thumb) when Driving
                    if (isTouching && currentMagnitude > deadzoneRatio) {
                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(CyanPrimary.copy(alpha = 0.9f), CyanSecondary.copy(alpha = 0.3f)),
                                start = center,
                                end = thumbCenter
                            ),
                            start = center,
                            end = thumbCenter,
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        // Target indicator point on center
                        drawCircle(
                            color = CyanPrimary,
                            radius = 3.dp.toPx(),
                            center = center
                        )
                    }

                    // 5. Thumbstick Knob
                    // Knob Glow Ring
                    if (isTouching) {
                        drawCircle(
                            color = CyanGlow,
                            radius = knobRadiusPx * 1.35f,
                            center = thumbCenter
                        )
                    }

                    // Metallic Knob Body
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                DarkSurfaceHighlight,
                                DarkSurfaceVariant,
                                DarkSurface
                            ),
                            center = thumbCenter - Offset(knobRadiusPx * 0.2f, knobRadiusPx * 0.2f),
                            radius = knobRadiusPx
                        ),
                        radius = knobRadiusPx,
                        center = thumbCenter
                    )

                    // Textured Outer Grip Ring
                    drawCircle(
                        color = if (isTouching) CyanPrimary else DarkBorder,
                        radius = knobRadiusPx,
                        center = thumbCenter,
                        style = Stroke(width = if (isTouching) 2.5.dp.toPx() else 1.5.dp.toPx())
                    )

                    // Center Glowing Core Pip
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = if (isTouching) {
                                listOf(Color.White, CyanPrimary, CyanSecondary)
                            } else {
                                listOf(DarkSurfaceHighlight, DarkBorder)
                            },
                            center = thumbCenter,
                            radius = knobRadiusPx * 0.40f
                        ),
                        radius = knobRadiusPx * 0.40f,
                        center = thumbCenter
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Real-Time HUD Status Bar
            val powerPercent = (currentMagnitude * 100).toInt().coerceIn(0, 100)
            val isDriving = isTouching && currentMagnitude > deadzoneRatio

            Box(
                modifier = Modifier
                    .background(DarkSurfaceVariant, RoundedCornerShape(16.dp))
                    .border(
                        1.dp,
                        if (isDriving) CyanPrimary.copy(alpha = 0.6f) else DarkBorder,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(if (isDriving) CyanPrimary else SuccessGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isDriving) {
                            String.format(Locale.US, "ACTIVE • %d%% POWER • %.0f°", powerPercent, currentAngleDeg)
                        } else {
                            "DEAD-MAN NEUTRAL (0% POWER)"
                        },
                        color = if (isDriving) CyanPrimary else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.6.sp
                    )
                }
            }
        }
    }
}
