package com.farias.prismaroll.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private class Star(val x: Float, val y: Float, val r: Float, val phase: Float)

private val STARS: List<Star> = Random(11).let { rnd ->
    List(70) { Star(rnd.nextFloat(), rnd.nextFloat(), 0.6f + rnd.nextFloat() * 1.6f, rnd.nextFloat() * 6.28f) }
}

/** Animated aurora background in the colors of the current theme (with stars on some themes). */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier.fillMaxSize(), override: GameTheme? = null) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16_000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    Canvas(modifier) {
        val theme = override ?: Palette.theme
        drawRect(Brush.verticalGradient(listOf(theme.bgTop, theme.bgBottom)))
        val a = phase * 2f * PI.toFloat()
        val w = size.width
        val h = size.height
        val (c1, c2, c3) = theme.aurora
        blob(c1, Offset(w * (0.25f + 0.18f * cos(a)), h * (0.18f + 0.07f * sin(a))), w * 0.85f, 0.32f)
        blob(c2, Offset(w * (0.85f + 0.12f * sin(a)), h * (0.55f + 0.09f * cos(a))), w * 0.70f, 0.16f)
        blob(c3, Offset(w * (0.20f + 0.14f * sin(a + 1.3f)), h * (0.92f + 0.05f * cos(a + 2f))), w * 0.80f, 0.22f)
        if (theme.stars) {
            val density = this.density
            for (s in STARS) {
                val tw = 0.35f + 0.65f * ((sin(a * 3f + s.phase) + 1f) / 2f)
                drawCircle(Color.White.copy(alpha = 0.7f * tw), s.r * density, Offset(s.x * w, s.y * h))
            }
        }
    }
}

private fun DrawScope.blob(color: Color, center: Offset, radius: Float, alpha: Float) {
    if (radius < 1f) return
    drawCircle(
        brush = Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), center, radius),
        radius = radius,
        center = center,
    )
}
