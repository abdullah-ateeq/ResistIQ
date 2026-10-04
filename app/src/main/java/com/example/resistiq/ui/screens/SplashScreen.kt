package com.example.resistiq.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val bodyAnim = remember { Animatable(0f) }
    val bandsAnim = remember { Animatable(0f) }
    val ohmAnim = remember { Animatable(0f) }
    val titleAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Stage 1: Resistor body appears (0 - 400ms)
        launch {
            bodyAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
        }

        // Stage 2: Colored bands slide onto resistor (300 - 800ms)
        delay(250)
        launch {
            bandsAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }

        // Stage 3: Ohm symbol appears subtly & Title fades in (600 - 1100ms)
        delay(350)
        launch {
            ohmAnim.animateTo(
                targetValue = 0.25f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }
        launch {
            titleAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }

        // Total duration: ~1.4 seconds
        delay(750)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Subtle watermark Ohm symbol in background
        Text(
            text = "Ω",
            fontSize = 240.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.alpha(ohmAnim.value)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Animated Resistor Drawing
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(90.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    val bodyScale = bodyAnim.value
                    if (bodyScale > 0f) {
                        val bodyW = (w * 0.65f) * bodyScale
                        val bodyH = 34.dp.toPx()
                        val bodyLeft = (w - bodyW) / 2f
                        val bodyTop = (h - bodyH) / 2f

                        // Leads
                        val leadH = 4.dp.toPx()
                        val leadTop = (h - leadH) / 2f
                        drawLine(
                            color = Color(0xFFB0BEC5),
                            start = Offset(0f, leadTop + leadH / 2),
                            end = Offset(bodyLeft, leadTop + leadH / 2),
                            strokeWidth = leadH
                        )
                        drawLine(
                            color = Color(0xFFB0BEC5),
                            start = Offset(bodyLeft + bodyW, leadTop + leadH / 2),
                            end = Offset(w, leadTop + leadH / 2),
                            strokeWidth = leadH
                        )

                        // Ceramic body
                        val ceramicGradient = Brush.verticalGradient(
                            colors = listOf(Color(0xFFEFE8DE), Color(0xFFDAC7AE), Color(0xFFC7B193)),
                            startY = bodyTop,
                            endY = bodyTop + bodyH
                        )
                        drawRoundRect(
                            brush = ceramicGradient,
                            topLeft = Offset(bodyLeft, bodyTop),
                            size = Size(bodyW, bodyH),
                            cornerRadius = CornerRadius(bodyH * 0.4f, bodyH * 0.4f)
                        )

                        // Bands sliding in
                        val bandProgress = bandsAnim.value
                        if (bandProgress > 0f) {
                            val bandColors = listOf(
                                Color(0xFF8D5B4C), // Brown (1)
                                Color(0xFF212121), // Black (0)
                                Color(0xFFE53935), // Red (×100)
                                Color(0xFFD4AF37)  // Gold (±5%)
                            )
                            val bandWidth = 10.dp.toPx()
                            val bandOffsets = listOf(0.2f, 0.38f, 0.56f, 0.82f)

                            bandColors.forEachIndexed { i, color ->
                                val targetX = bodyLeft + bodyW * bandOffsets[i]
                                val startX = bodyLeft
                                val currentX = startX + (targetX - startX) * bandProgress

                                val slideYOffset = (1f - bandProgress) * 30f

                                drawRect(
                                    color = color,
                                    topLeft = Offset(currentX - bandWidth / 2, bodyTop + slideYOffset),
                                    size = Size(bandWidth, bodyH)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App Title
            Text(
                text = "RESISTIQ",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.alpha(titleAnim.value)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle
            Text(
                text = "Resistor & Ohm's Law Calculator",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp,
                modifier = Modifier.alpha(titleAnim.value)
            )
        }
    }
}
