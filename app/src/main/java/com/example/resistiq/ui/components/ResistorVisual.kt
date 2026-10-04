package com.example.resistiq.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.resistiq.model.BandCount
import com.example.resistiq.model.ResistorColor

@Composable
fun ResistorVisual(
    bands: List<ResistorColor>,
    bandCount: BandCount,
    modifier: Modifier = Modifier,
    selectedBandIndex: Int? = null,
    height: Dp = 100.dp
) {
    val countWord = when (bandCount) {
        BandCount.THREE -> "Three"
        BandCount.FOUR -> "Four"
        BandCount.FIVE -> "Five"
        BandCount.SIX -> "Six"
    }
    val description = "$countWord band resistor: ${bands.take(bandCount.count).joinToString(", ") { it.displayName }}"

    // Subtle pulse on the band that was just changed
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(bands, selectedBandIndex) {
        if (selectedBandIndex != null) {
            pulse.snapTo(1f)
            pulse.animateTo(0f, animationSpec = tween(500, easing = FastOutSlowInEasing))
        }
    }

    val glowColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription = description }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
            val totalWidth = size.width
            val totalHeight = size.height

            val bodyHeight = totalHeight * 0.48f
            val bodyWidth = (totalWidth * 0.62f).coerceAtMost(340.dp.toPx())
            val bodyLeft = (totalWidth - bodyWidth) / 2f
            val bodyTop = (totalHeight - bodyHeight) / 2f
            val bodyBottom = bodyTop + bodyHeight

            val leadHeight = 5.dp.toPx()
            val leadTop = (totalHeight - leadHeight) / 2f

            // 1. Draw metal leads
            val leadBrush = Brush.verticalGradient(
                colors = listOf(Color(0xFFE0E0E0), Color(0xFF9E9E9E), Color(0xFF757575)),
                startY = leadTop,
                endY = leadTop + leadHeight
            )
            // Left lead
            drawRect(
                brush = leadBrush,
                topLeft = Offset(x = 10f, y = leadTop),
                size = Size(width = bodyLeft - 5f, height = leadHeight)
            )
            // Right lead
            drawRect(
                brush = leadBrush,
                topLeft = Offset(x = bodyLeft + bodyWidth - 5f, y = leadTop),
                size = Size(width = totalWidth - (bodyLeft + bodyWidth) - 5f, height = leadHeight)
            )

            // 2. Draw Resistor Body with ceramic/carbon contours
            // Ceramic pale tan body with light gradient for cylindrical effect
            val ceramicGradient = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFEFE8DE),
                    Color(0xFFE4D7C3),
                    Color(0xFFDAC7AE),
                    Color(0xFFC7B193)
                ),
                startY = bodyTop,
                endY = bodyBottom
            )

            // Main body rounded rectangle
            drawRoundRect(
                brush = ceramicGradient,
                topLeft = Offset(bodyLeft, bodyTop),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = CornerRadius(bodyHeight * 0.35f, bodyHeight * 0.35f)
            )

            // Bulbous ends (classic axial resistor cap rings)
            val bulbWidth = bodyWidth * 0.12f
            val bulbHeight = bodyHeight * 1.12f
            val bulbTop = (totalHeight - bulbHeight) / 2f

            val bulbGradient = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFF3ECE2),
                    Color(0xFFE2D4BE),
                    Color(0xFFCFBA9A),
                    Color(0xFFBC9F7A)
                ),
                startY = bulbTop,
                endY = bulbTop + bulbHeight
            )

            // Left bulb
            drawRoundRect(
                brush = bulbGradient,
                topLeft = Offset(bodyLeft, bulbTop),
                size = Size(bulbWidth, bulbHeight),
                cornerRadius = CornerRadius(bulbHeight * 0.35f, bulbHeight * 0.35f)
            )

            // Right bulb
            drawRoundRect(
                brush = bulbGradient,
                topLeft = Offset(bodyLeft + bodyWidth - bulbWidth, bulbTop),
                size = Size(bulbWidth, bulbHeight),
                cornerRadius = CornerRadius(bulbHeight * 0.35f, bulbHeight * 0.35f)
            )

            // 3. Draw Bands
            val effectiveBands = bands.take(bandCount.count)
            val activeBandCount = effectiveBands.size
            if (activeBandCount > 0) {
                // Calculate band positions
                // Left group and right group separated by a gap (for tolerance/tempco)
                val innerLeft = bodyLeft + bulbWidth * 0.9f
                val innerRight = bodyLeft + bodyWidth - bulbWidth * 0.9f
                val innerWidth = innerRight - innerLeft

                val bandThickness = (innerWidth / (bandCount.count + 2.5f)).coerceIn(10.dp.toPx(), 22.dp.toPx())

                val bandCenters = calculateBandPositions(
                    bandCount = bandCount,
                    innerLeft = innerLeft,
                    innerWidth = innerWidth,
                    bandThickness = bandThickness
                )

                effectiveBands.forEachIndexed { index, resistorColor ->
                    if (index < bandCenters.size) {
                        val centerX = bandCenters[index]
                        val emphasis = if (selectedBandIndex == index) pulse.value else 0f
                        drawResistorBand(
                            centerX = centerX,
                            bodyTop = bodyTop - 2f,
                            bodyHeight = bodyHeight + 4f,
                            bandThickness = bandThickness * (1f + 0.18f * emphasis),
                            color = resistorColor,
                            emphasis = emphasis,
                            glowColor = glowColor
                        )
                    }
                }
            }

            // 4. Subtle top gloss/reflection across entire resistor for cylindrical 3D look
            val glossBrush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.38f),
                    Color.White.copy(alpha = 0.12f),
                    Color.Transparent
                ),
                startY = bodyTop,
                endY = bodyTop + bodyHeight * 0.4f
            )
            drawRoundRect(
                brush = glossBrush,
                topLeft = Offset(bodyLeft, bodyTop),
                size = Size(bodyWidth, bodyHeight * 0.4f),
                cornerRadius = CornerRadius(bodyHeight * 0.35f, bodyHeight * 0.35f)
            )
        }
    }
}

private fun calculateBandPositions(
    bandCount: BandCount,
    innerLeft: Float,
    innerWidth: Float,
    bandThickness: Float
): List<Float> {
    return when (bandCount) {
        BandCount.THREE -> {
            listOf(
                innerLeft + innerWidth * 0.22f,
                innerLeft + innerWidth * 0.48f,
                innerLeft + innerWidth * 0.76f
            )
        }
        BandCount.FOUR -> {
            listOf(
                innerLeft + innerWidth * 0.16f,
                innerLeft + innerWidth * 0.36f,
                innerLeft + innerWidth * 0.56f,
                innerLeft + innerWidth * 0.85f // tolerance spaced out
            )
        }
        BandCount.FIVE -> {
            listOf(
                innerLeft + innerWidth * 0.14f,
                innerLeft + innerWidth * 0.30f,
                innerLeft + innerWidth * 0.46f,
                innerLeft + innerWidth * 0.62f,
                innerLeft + innerWidth * 0.86f // tolerance spaced out
            )
        }
        BandCount.SIX -> {
            listOf(
                innerLeft + innerWidth * 0.12f,
                innerLeft + innerWidth * 0.26f,
                innerLeft + innerWidth * 0.40f,
                innerLeft + innerWidth * 0.54f,
                innerLeft + innerWidth * 0.74f,
                innerLeft + innerWidth * 0.88f
            )
        }
    }
}

private fun DrawScope.drawResistorBand(
    centerX: Float,
    bodyTop: Float,
    bodyHeight: Float,
    bandThickness: Float,
    color: ResistorColor,
    emphasis: Float,
    glowColor: Color
) {
    val left = centerX - bandThickness / 2f
    val baseColor = color.color

    // Cylindrical gradient for realistic wrap-around sheen
    val bandBrush = when (color) {
        ResistorColor.GOLD -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFE082),
                Color(0xFFD4AF37),
                Color(0xFFA67C00),
                Color(0xFF7A5900)
            ),
            startY = bodyTop,
            endY = bodyTop + bodyHeight
        )
        ResistorColor.SILVER -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFDCDCDC),
                Color(0xFFA8A8A8),
                Color(0xFF757575)
            ),
            startY = bodyTop,
            endY = bodyTop + bodyHeight
        )
        ResistorColor.BLACK -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF424242),
                Color(0xFF212121),
                Color(0xFF141414),
                Color(0xFF000000)
            ),
            startY = bodyTop,
            endY = bodyTop + bodyHeight
        )
        else -> Brush.verticalGradient(
            colors = listOf(
                baseColor.copy(alpha = 0.95f),
                baseColor,
                baseColor.copy(red = baseColor.red * 0.75f, green = baseColor.green * 0.75f, blue = baseColor.blue * 0.75f)
            ),
            startY = bodyTop,
            endY = bodyTop + bodyHeight
        )
    }

    // Glow behind the band while it is emphasized, so the band color itself stays accurate
    if (emphasis > 0f) {
        drawRect(
            color = glowColor.copy(alpha = 0.45f * emphasis),
            topLeft = Offset(left - 3.dp.toPx(), bodyTop - 4.dp.toPx()),
            size = Size(bandThickness + 6.dp.toPx(), bodyHeight + 8.dp.toPx())
        )
    }

    // Band rectangle
    drawRect(
        brush = bandBrush,
        topLeft = Offset(left, bodyTop),
        size = Size(bandThickness, bodyHeight)
    )
}
