package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.Song
import com.example.ui.theme.*
import kotlin.math.PI
import kotlin.math.sin

// GLASSMORPHISM CONTAINERS

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable { onClick() }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(AeroSurfaceGlass)
            .border(
                width = borderWidth,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.03f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .then(clickableModifier)
            .padding(16.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
fun GlassmorphicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    text: String? = null,
    enabled: Boolean = true,
    testTag: String = "glass_button"
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .testTag(testTag)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.2f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(50)
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.08f),
            contentColor = AeroTextPrimary,
            disabledContainerColor = Color.White.copy(alpha = 0.02f),
            disabledContentColor = AeroTextSecondary.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(50),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = text ?: "Button Icon",
                    modifier = Modifier.size(18.dp)
                )
                if (text != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
            if (text != null) {
                Text(
                    text = text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// PREMIUM SHIFTING BLUR BACKGROUNDS

@Composable
fun AnimatedGlassBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "BgTransition")
    
    // First orb positions
    val xOffset1 by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 100f,
        animationSpec = infiniteTransitionSpec(8000),
        label = "x1"
    )
    val yOffset1 by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 100f,
        animationSpec = infiniteTransitionSpec(10000),
        label = "y1"
    )

    // Second orb positions
    val xOffset2 by infiniteTransition.animateFloat(
        initialValue = 100f,
        targetValue = -100f,
        animationSpec = infiniteTransitionSpec(9000),
        label = "x2"
    )
    val yOffset2 by infiniteTransition.animateFloat(
        initialValue = 100f,
        targetValue = -100f,
        animationSpec = infiniteTransitionSpec(7000),
        label = "y2"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AeroBackground)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(80.dp)
        ) {
            // Shifting cyan orb
            drawCircle(
                color = AeroCyan.copy(alpha = 0.15f),
                radius = size.width / 2f,
                center = center.copy(
                    x = center.x + xOffset1.dp.toPx(),
                    y = center.y + yOffset1.dp.toPx()
                )
            )
            // Shifting neon pink orb
            drawCircle(
                color = AeroAccentPink.copy(alpha = 0.12f),
                radius = size.width / 2.2f,
                center = center.copy(
                    x = center.x + xOffset2.dp.toPx(),
                    y = center.y + yOffset2.dp.toPx()
                )
            )
            // Grounding bottom gold orb
            drawCircle(
                color = AeroTeal.copy(alpha = 0.1f),
                radius = size.width / 1.8f,
                center = center.copy(
                    y = size.height * 0.9f
                )
            )
        }
        // Subtle dark mask over the canvas to make text elements read perfectly
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.15f))
        )
    }
}

private fun infiniteTransitionSpec(duration: Int): InfiniteRepeatableSpec<Float> {
    return infiniteRepeatable(
        animation = tween(duration, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse
    )
}

// REAL-TIME AUDIO WAVE VISUALIZER

@Composable
fun MusicWaveVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 16,
    color: Color = AeroCyan
) {
    val infiniteTransition = rememberInfiniteTransition(label = "VisualizerTransition")
    
    // Create animated offsets for waves
    val waveHeights = List(barCount) { index ->
        val duration = 800 + (index * 70)
        infiniteTransition.animateFloat(
            initialValue = 0.15f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(duration, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Bar$index"
        )
    }

    Canvas(modifier = modifier) {
        val spacing = 4.dp.toPx()
        val totalSpacing = spacing * (barCount - 1)
        val barWidth = (size.width - totalSpacing) / barCount
        val maxBarHeight = size.height

        for (i in 0 until barCount) {
            val hRatio = if (isPlaying) waveHeights[i].value else 0.1f
            val barHeight = maxBarHeight * hRatio
            
            val x = i * (barWidth + spacing)
            val y = (maxBarHeight - barHeight) / 2 // Centered wave

            drawRoundRect(
                color = color.copy(alpha = if (isPlaying) 1.0f else 0.4f),
                topLeft = androidx.compose.ui.geometry.Offset(x, y),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

// GORGEOUS VINYL ARTWORK WITH SHADOW & GLOW

@Composable
fun VinylDisk(
    isPlaying: Boolean,
    song: Song?,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 260.dp
) {
    val rotationTransition = rememberInfiniteTransition(label = "DiskRotation")
    val angle by rotationTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    // Pulse disk cover slightly to beat
    val scaleTransition = rememberInfiniteTransition(label = "PulseTransition")
    val pulseScale by scaleTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val currentRotation = if (isPlaying) angle else 0f
    val currentScale = if (isPlaying) pulseScale else 1.0f

    Box(
        modifier = modifier
            .size(sizeDp * currentScale)
            .shadow(
                elevation = 25.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = AeroCyan.copy(alpha = 0.5f),
                spotColor = AeroAccentPink.copy(alpha = 0.5f)
            ),
        contentAlignment = Alignment.Center
    ) {
        // Outer black vinyl disc
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(currentRotation)
        ) {
            // Draw Vinyl background
            drawCircle(color = Color(0xFF0D0F13))

            // Draw glossy grooves on the vinyl
            for (r in 1..8) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.04f),
                    radius = (size.width / 2f) * (0.4f + (r * 0.07f)),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Draw the metallic/gloss shine reflections (arcs)
            drawArc(
                color = Color.White.copy(alpha = 0.06f),
                startAngle = -45f,
                sweepAngle = 90f,
                useCenter = false,
                style = Stroke(width = size.width / 4f)
            )
            drawArc(
                color = Color.White.copy(alpha = 0.06f),
                startAngle = 135f,
                sweepAngle = 90f,
                useCenter = false,
                style = Stroke(width = size.width / 4f)
            )
        }

        // Inner Album Art Centerpiece
        Box(
            modifier = Modifier
                .size(sizeDp * 0.55f * currentScale)
                .rotate(currentRotation)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AeroCyan,
                            AeroTeal,
                            AeroAccentPink
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Draw abstract musical geometry as Album Art
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerPt = this.center
                val r = size.width / 2.2f
                
                // Outer circle border in art
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f),
                    radius = r,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Abstract lines inside art
                for (angleRad in 0 until 360 step 45) {
                    val rad = angleRad * PI / 180.0
                    val x = centerPt.x + r * kotlin.math.cos(rad).toFloat()
                    val y = centerPt.y + r * sin(rad).toFloat()
                    drawLine(
                        color = Color.White.copy(alpha = 0.2f),
                        start = centerPt,
                        end = androidx.compose.ui.geometry.Offset(x, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            // Central icon for decoration
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = "Vinyl logo",
                modifier = Modifier.size(36.dp),
                tint = Color.White
            )
        }

        // Central spindle hole
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(AeroBackground)
                .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
        )
    }
}

// SLEEP TIMER PICKER DIALOG

@Composable
fun SleepTimerDialog(
    currentMinutes: Int,
    onMinutesSelected: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel", color = AeroTextSecondary)
            }
        },
        title = {
            Text(
                "Sleep Timer",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AeroTextPrimary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (currentMinutes > 0) "Timer active: $currentMinutes min remaining" else "Select countdown time to pause music automatically",
                    fontSize = 14.sp,
                    color = AeroTextSecondary,
                    textAlign = TextAlign.Center
                )
                
                if (currentMinutes > 0) {
                    Button(
                        onClick = {
                            onMinutesSelected(0)
                            onDismissRequest()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AeroAccentPink),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Turn Off Timer", color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val options = listOf(5, 15, 30, 45, 60, 90)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    options.take(3).forEach { min ->
                        TimerOptionButton(min = min) {
                            onMinutesSelected(min)
                            onDismissRequest()
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    options.drop(3).forEach { min ->
                        TimerOptionButton(min = min) {
                            onMinutesSelected(min)
                            onDismissRequest()
                        }
                    }
                }
            }
        },
        containerColor = AeroSurface,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun TimerOptionButton(min: Int, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.08f),
            contentColor = AeroCyan
        ),
        modifier = Modifier.width(72.dp),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text("$min min", fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
