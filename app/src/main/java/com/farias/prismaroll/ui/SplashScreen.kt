package com.farias.prismaroll.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farias.prismaroll.audio.Sfx
import com.farias.prismaroll.audio.SoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

private val TILE_A = Color(0xFF22D3EE)
private val TILE_B = Color(0xFF8B5CF6)
private val RAIL = Color(0xFFE879F9)
private val LOGO_COLORS = listOf(Color(0xFF22D3EE), Color(0xFF60A5FA), Color(0xFF8B5CF6), Color(0xFFC084FC), Color(0xFFE879F9), Color(0xFFF472B6))

// Timeline (seconds)
private const val TRACK_IN = 0.9f
private const val BALL_START = 0.45f
private const val BALL_END = 1.85f
private const val JUMP_END = 2.2f
private const val FLASH = 2.22f
private const val LOGO = FLASH + 0.08f
private const val END = 4.5f
/** The whole opening plays this many times slower than the timeline above (real seconds = SLOW × t). */
private const val SLOW = 1.15f

private class SplashStar(val x: Float, val y: Float, val r: Float, val ph: Float)

/** Damped spring 0 → 1 (pops past 1 and settles). */
private fun spring01(t: Float): Float = if (t <= 0f) 0f else 1f - exp(-7f * t) * cos(13f * t)

/**
 * Opening: a neon track lights up from the horizon to the screen, the ball rolls down it towards you,
 * leaps off the end and becomes the "O" of ROLL in the logo. Tap to skip.
 */
@Composable
fun SplashScreen(sound: SoundManager, onFinish: () -> Unit) {
    var t by remember { mutableFloatStateOf(0f) }
    var done by remember { mutableStateOf(false) }
    val stars = remember { Random(3).let { r -> List(80) { SplashStar(r.nextFloat(), r.nextFloat() * 0.6f, 0.6f + r.nextFloat() * 1.6f, r.nextFloat() * 6.28f) } } }
    val sparks = remember { Random(11).let { r -> List(60) { floatArrayOf(r.nextFloat() * 6.28f, 0.3f + r.nextFloat(), r.nextFloat()) } } }
    val path = remember { Path() }

    fun finish() {
        if (done) return
        done = true
        onFinish()
    }

    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (!done) {
            withFrameNanos { now -> t = (now - start) / 1e9f / SLOW }
            if (t > END) finish()
        }
    }
    LaunchedEffect(Unit) {
        launch { delay((0.1f * 1000 * SLOW).toLong()); sound.play(Sfx.WHOOSH, 0.5f, 0.8f) }
        for (i in 0 until 4) launch {
            delay(((0.15f + i * 0.2f) * 1000 * SLOW).toLong())
            sound.play(Sfx.PASS, 0.35f, 0.9f + i * 0.12f)
        }
        launch { delay((BALL_START * 1000 * SLOW).toLong()); sound.play(Sfx.WHOOSH, 0.7f, 1.3f) }
        launch { delay((BALL_END * 1000 * SLOW).toLong()); sound.play(Sfx.BOUNCE, 0.9f, 1.2f) }
        launch { delay((FLASH * 1000 * SLOW).toLong()); sound.play(Sfx.RECORD) }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { finish() },
    ) {
        val hDp = maxHeight
        val shining by remember { androidx.compose.runtime.derivedStateOf { t > LOGO + 0.8f && t < LOGO + 1.6f } }
        // Static sky: drawn once, faded in by its layer (no redraw per frame).
        Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = (t / 0.5f).coerceIn(0f, 1f) }) { drawSplashSky(stars) }
        Canvas(Modifier.fillMaxSize()) { drawSplash(t, sparks, path, hDp.toPx()) }

        // Logo: PRISMA letter by letter, then ROLL with the ball as its "O".
        Column(
            Modifier
                .fillMaxWidth()
                .offset(y = hDp * 0.26f)
                .then(if (shining) Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen } else Modifier)
                .drawWithContent {
                    drawContent()
                    val s = ((t - (LOGO + 0.85f)) / 0.7f)
                    if (s in 0f..1f) {
                        val x = size.width * (-0.2f + 1.4f * s)
                        val band = size.width * 0.14f
                        drawRect(
                            Brush.linearGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.85f), Color.Transparent),
                                start = Offset(x - band, 0f),
                                end = Offset(x + band, size.height),
                            ),
                            blendMode = BlendMode.SrcAtop,
                        )
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(horizontalArrangement = Arrangement.Center) {
                "PRISMA".forEachIndexed { i, ch ->
                    Text(
                        ch.toString(),
                        style = Styles.glow(58, LOGO_COLORS[i], FontWeight.Black),
                        // Time is read only inside the layer block: the letters are redrawn, never recomposed.
                        modifier = Modifier.graphicsLayer {
                            val lt = t - (LOGO + i * 0.065f)
                            val sc = spring01(lt)
                            scaleX = sc
                            scaleY = sc
                            alpha = (lt * 6f).coerceIn(0f, 1f)
                            translationY = (1f - sc.coerceAtMost(1f)) * -60f
                            rotationZ = (1f - sc) * (if (i % 2 == 0) -25f else 25f)
                        },
                    )
                }
            }
            RollWord(
                30,
                Modifier.graphicsLayer {
                    val ta = ((t - (LOGO + 0.1f)) * 3.5f).coerceIn(0f, 1f)
                    alpha = ta
                    scaleX = 0.85f + 0.15f * ta
                    scaleY = 0.85f + 0.15f * ta
                },
            )
        }
        // Fade out at the very end
        Canvas(Modifier.fillMaxSize()) {
            val fade = ((t - (END - 0.35f)) / 0.35f).coerceIn(0f, 1f)
            if (fade > 0f) drawRect(Color(0xFF07051A), alpha = fade)
        }
    }
}

private fun DrawScope.drawSplashSky(stars: List<SplashStar>) {
    val w = size.width
    val h = size.height
    if (w < 2f || h < 2f) return
    drawRect(Brush.verticalGradient(listOf(Color(0xFF07051A), Color(0xFF1A0B45), Color(0xFF3A0F5C))))
    blob(Color(0xFF7C3AED), Offset(w * 0.22f, h * 0.2f), w * 0.9f, 0.35f)
    blob(Color(0xFF06B6D4), Offset(w * 0.95f, h * 0.45f), w * 0.7f, 0.22f)
    blob(Color(0xFFDB2777), Offset(w * 0.5f, h * 0.55f), w * 0.8f, 0.3f)
    for (s in stars) {
        val tw = 0.35f + 0.65f * (0.5f + 0.5f * sin(s.ph * 3f))
        drawCircle(Color.White.copy(alpha = 0.75f * tw), s.r * density * 0.8f, Offset(s.x * w, s.y * h))
    }
}

private fun DrawScope.drawSplash(t: Float, sparks: List<FloatArray>, path: Path, hPx: Float) {
    val w = size.width
    val h = size.height
    if (w < 2f || h < 2f) return
    val hor = h * 0.52f
    val bot = h * 1.04f
    val halfB = w * 0.44f
    val halfT = w * 0.03f
    fun near(k: Float) = (1f - k).pow(2.2f)
    fun rowY(k: Float) = hor + (bot - hor) * near(k)
    fun rowHalf(k: Float) = halfT + (halfB - halfT) * near(k)
    fun rowX(k: Float) = w / 2f + sin(k * 2.4f) * w * 0.1f * k

    // ---------------------------------------------------------------- the track, lighting up far → near
    val fadeOut = if (t > FLASH) (1f - (t - FLASH) / 0.6f).coerceIn(0f, 1f) else 1f
    if (fadeOut > 0f) {
        val n = 24
        for (i in n - 1 downTo 0) {
            val k0 = i / n.toFloat()
            val k1 = (i + 1) / n.toFloat()
            val lit = (1f - k1) * TRACK_IN
            val a = ((t - lit) / 0.14f).coerceIn(0f, 1f) * fadeOut
            if (a <= 0f) continue
            val x0 = rowX(k0); val y0 = rowY(k0); val h0 = rowHalf(k0)
            val x1 = rowX(k1); val y1 = rowY(k1); val h1 = rowHalf(k1)
            val c = if (i % 2 == 0) TILE_A else TILE_B
            // Side faces (thickness)
            val th = (y0 - y1) * 0.35f + h0 * 0.05f
            path.reset()
            path.moveTo(x0 - h0, y0); path.lineTo(x1 - h1, y1); path.lineTo(x1 - h1, y1 + th * 0.6f); path.lineTo(x0 - h0, y0 + th); path.close()
            drawPath(path, lerp(c, Color.Black, 0.7f), alpha = a)
            path.reset()
            path.moveTo(x0 + h0, y0); path.lineTo(x1 + h1, y1); path.lineTo(x1 + h1, y1 + th * 0.6f); path.lineTo(x0 + h0, y0 + th); path.close()
            drawPath(path, lerp(c, Color.Black, 0.7f), alpha = a)
            // Dark glass top with a glowing seam
            path.reset()
            path.moveTo(x0 - h0, y0); path.lineTo(x0 + h0, y0); path.lineTo(x1 + h1, y1); path.lineTo(x1 - h1, y1); path.close()
            // A light pulse runs towards the viewer.
            val pulse = (1f - kotlin.math.abs(((t * 0.9f) % 1f) - (1f - k0)) * 6f).coerceIn(0f, 1f)
            drawPath(path, lerp(lerp(c, Color.Black, 0.55f), c, 0.25f + 0.5f * pulse), alpha = a)
            drawLine(c.copy(alpha = 0.8f * a), Offset(x0 - h0, y0), Offset(x0 + h0, y0), (h0 * 0.02f).coerceAtLeast(1f))
            // Neon rails
            for (side in 0..1) {
                val sx = if (side == 0) -1f else 1f
                drawLine(RAIL.copy(alpha = 0.3f * a), Offset(x0 + sx * h0, y0), Offset(x1 + sx * h1, y1), (h0 * 0.07f).coerceAtLeast(2f), StrokeCap.Round)
                drawLine(Color.White.copy(alpha = 0.95f * a), Offset(x0 + sx * h0, y0), Offset(x1 + sx * h1, y1), (h0 * 0.018f).coerceAtLeast(1f), StrokeCap.Round)
            }
        }
    }

    // ---------------------------------------------------------------- the ball
    val target = Offset(w / 2f - 20.dp.toPx(), hPx * 0.26f + 96.dp.toPx())
    if (t in BALL_START..FLASH) {
        fun ballAt(tt: Float): Triple<Float, Float, Float> {
            return if (tt <= BALL_END) {
                val u = ((tt - BALL_START) / (BALL_END - BALL_START)).coerceIn(0f, 1f)
                val k = 0.97f - 0.9f * u.pow(1.7f)
                val r = rowHalf(k) * 0.3f
                Triple(rowX(k), rowY(k) - r, r)
            } else {
                val k = 0.07f
                val r0 = rowHalf(k) * 0.3f
                val sx = rowX(k); val sy = rowY(k) - r0
                val v = ((tt - BALL_END) / (JUMP_END - BALL_END)).coerceIn(0f, 1f)
                val e = 1f - (1f - v) * (1f - v)
                val x = sx + (target.x - sx) * e
                val y = sy + (target.y - sy) * e - sin(v * PI.toFloat()) * h * 0.08f
                Triple(x, y, r0 + (17.dp.toPx() - r0) * e)
            }
        }
        // Trail
        for (j in 7 downTo 1) {
            val (x, y, r) = ballAt(t - j * 0.025f)
            if (t - j * 0.025f < BALL_START) continue
            drawCircle(RAIL.copy(alpha = 0.12f * (8 - j) / 8f), r * (1f - j * 0.06f), Offset(x, y))
        }
        val (bx, by, br) = ballAt(t)
        if (t <= BALL_END) drawOval(Color.Black.copy(alpha = 0.35f), Offset(bx - br * 1.1f, by + br * 0.7f), androidx.compose.ui.geometry.Size(br * 2.2f, br * 0.5f))
        blob(RAIL, Offset(bx, by), br * 2.6f, 0.45f)
        drawBall(Offset(bx, by), br, t * 9f)
    }

    // ---------------------------------------------------------------- flash and sparks
    if (t > FLASH - 0.02f) {
        val ft = t - FLASH
        val f = exp(-ft * 6f)
        blob(Color.White, target, w * (0.3f + ft * 1.2f), 0.85f * f)
        if (ft < 1.2f) for (s in sparks) {
            val d = ft * w * 0.55f * s[1]
            val a = (1f - ft / 1.2f) * (0.6f + 0.4f * s[2])
            val c = if (s[2] > 0.5f) RAIL else TILE_A
            drawCircle(c.copy(alpha = a), (2.5f + 3f * s[2]) * density, Offset(target.x + cos(s[0]) * d, target.y + sin(s[0]) * d * 0.8f))
        }
    }
}

/** A glossy ball with a rolling stripe ([spin] in radians). Shared by the splash and the logo. */
fun DrawScope.drawBall(c: Offset, r: Float, spin: Float) {
    if (r < 1f) return
    drawCircle(
        Brush.radialGradient(listOf(Color.White, Color(0xFFF5A3FF), Color(0xFFB026D9), Color(0xFF4C0E7A)), Offset(c.x - r * 0.35f, c.y - r * 0.4f), r * 1.5f),
        r, c,
    )
    // A stripe around the ball that turns as it rolls.
    val band = cos(spin)
    val y = c.y + r * 0.75f * sin(spin)
    if (band > -0.2f) {
        val hw = r * kotlin.math.sqrt((1f - (0.75f * sin(spin)).pow(2)).coerceAtLeast(0f))
        drawLine(Color(0xFF22D3EE).copy(alpha = 0.85f * (0.4f + 0.6f * band.coerceAtLeast(0f))), Offset(c.x - hw * 0.96f, y), Offset(c.x + hw * 0.96f, y), r * 0.22f, StrokeCap.Round)
    }
    drawCircle(Color.White.copy(alpha = 0.85f), r * 0.2f, Offset(c.x - r * 0.38f, c.y - r * 0.42f))
}

private fun DrawScope.blob(color: Color, center: Offset, radius: Float, alpha: Float) {
    if (radius < 1f || alpha <= 0f) return
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), center, radius), radius, center)
}
