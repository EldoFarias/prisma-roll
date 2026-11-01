package com.farias.prismaroll.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farias.prismaroll.audio.Sfx
import com.farias.prismaroll.audio.SoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private val RING_COLORS = listOf(
    Color(0xFF22D3EE), Color(0xFF818CF8), Color(0xFFC084FC), Color(0xFFF472B6), Color(0xFFFB923C),
)
private val LOGO_COLORS = listOf(Color(0xFF22D3EE), Color(0xFF60A5FA), Color(0xFF8B5CF6), Color(0xFFC084FC), Color(0xFFE879F9), Color(0xFFF472B6))

// Timeline (seconds)
private const val RING_DROP = 0.25f
private const val RING_STAGGER = 0.12f
private const val BALL_IN = 1.15f
private const val FIRST_HIT = 1.42f
private const val BOUNCE_TOP = 1.62f
private const val SMASH_START = 1.78f
private const val SMASH_STEP = 0.1f
private const val FLASH = SMASH_START + 5 * SMASH_STEP + 0.04f
private const val LOGO = FLASH + 0.12f
private const val END = 5.0f
/** The whole opening plays this many times slower than the timeline above (real seconds = SLOW × t). */
private const val SLOW = 1.45f

private class Shard(val ring: Int, val a: Float, val vx: Float, val vy: Float, val spin: Float, val size: Float, val bright: Boolean)
private class SplashStar(val x: Float, val y: Float, val r: Float, val ph: Float)

/** easeOutBack-like landing with a little overshoot. */
private fun land(t: Float): Float {
    if (t <= 0f) return 0f
    if (t >= 1f) return 1f
    val c1 = 1.9f
    val c3 = c1 + 1f
    val x = t - 1f
    return 1f + c3 * x * x * x + c1 * x * x
}

/** Damped spring 0 → 1 (pops past 1 and settles). */
private fun spring01(t: Float): Float = if (t <= 0f) 0f else 1f - exp(-7f * t) * cos(13f * t)

/**
 * Opening: rings fall and stack into a tower, the ball bounces once, catches fire and smashes down
 * through every ring, a flash of light, then the logo pops in letter by letter. Tap to skip.
 */
@Composable
fun SplashScreen(sound: SoundManager, onFinish: () -> Unit) {
    var t by remember { mutableFloatStateOf(0f) }
    var done by remember { mutableStateOf(false) }
    val shards = remember {
        val rnd = Random(7)
        List(5 * 26) { k ->
            val ring = k / 26
            val a = rnd.nextFloat() * 2f * PI.toFloat()
            Shard(ring, a, cos(a) * (0.35f + rnd.nextFloat() * 0.8f), -(0.25f + rnd.nextFloat() * 0.7f), (rnd.nextFloat() - 0.5f) * 900f, 0.012f + rnd.nextFloat() * 0.022f, rnd.nextFloat() < 0.25f)
        }
    }
    val stars = remember { Random(3).let { r -> List(80) { SplashStar(r.nextFloat(), r.nextFloat(), 0.6f + r.nextFloat() * 1.6f, r.nextFloat() * 6.28f) } } }
    val sparks = remember { Random(11).let { r -> List(70) { floatArrayOf(r.nextFloat() * 6.28f, 0.3f + r.nextFloat(), r.nextFloat()) } } }

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
        launch { delay((RING_DROP * 1000 * SLOW).toLong()); sound.play(Sfx.WHOOSH, 0.6f) }
        for (i in 0 until 5) launch {
            delay(((RING_DROP + i * RING_STAGGER + 0.28f) * 1000 * SLOW).toLong())
            sound.play(Sfx.PLACE, 0.45f, 0.8f + i * 0.08f)
        }
        launch { delay((FIRST_HIT * 1000 * SLOW).toLong()); sound.play(Sfx.BOUNCE, 0.9f) }
        launch { delay((BOUNCE_TOP * 1000 * SLOW).toLong()); sound.play(Sfx.COMBO, 0.8f) }
        for (i in 0 until 5) launch {
            delay(((SMASH_START + i * SMASH_STEP) * 1000 * SLOW).toLong())
            sound.play(Sfx.GLASS, 0.8f, 0.9f + i * 0.1f)
        }
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
        Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = (t / 0.6f).coerceIn(0f, 1f) }) { drawSplashSky(stars) }
        Canvas(Modifier.fillMaxSize()) { drawSplash(t, shards, sparks) }

        // Logo, letter by letter
        Column(
            Modifier
                .fillMaxWidth()
                .offset(y = hDp * 0.30f)
                // The light sweep needs an offscreen layer; only use it while the sweep runs.
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
            Text(
                "TOWER",
                style = Styles.glow(26, Color.White, FontWeight.Black).copy(letterSpacing = 12.sp),
                modifier = Modifier.graphicsLayer {
                    val ta = ((t - (LOGO + 0.45f)) * 3f).coerceIn(0f, 1f)
                    alpha = ta
                    scaleX = 0.8f + 0.2f * ta
                    translationY = (1f - ta) * 30f
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
    drawRect(Brush.verticalGradient(listOf(Color(0xFF07051A), Color(0xFF160B3A), Color(0xFF2A0F4A))))
    blob(Color(0xFF7C3AED), Offset(w * 0.22f, h * 0.2f), w * 0.9f, 0.35f)
    blob(Color(0xFF06B6D4), Offset(w * 0.95f, h * 0.55f), w * 0.7f, 0.22f)
    blob(Color(0xFFDB2777), Offset(w * 0.15f, h * 0.95f), w * 0.8f, 0.25f)
    for (s in stars) {
        val tw = 0.35f + 0.65f * (0.5f + 0.5f * sin(s.ph * 3f))
        drawCircle(Color.White.copy(alpha = 0.75f * tw), s.r * density * 0.8f, Offset(s.x * w, s.y * h))
    }
    for (k in 0 until 3) {
        val bx = w * (0.25f + k * 0.25f)
        val path = Path().apply {
            moveTo(bx - w * 0.03f, 0f); lineTo(bx + w * 0.03f, 0f); lineTo(bx + w * 0.18f, h); lineTo(bx - w * 0.18f, h); close()
        }
        drawPath(path, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.06f), Color.Transparent)))
    }
}

private fun DrawScope.drawSplash(t: Float, shards: List<Shard>, sparks: List<FloatArray>) {
    val w = size.width
    val h = size.height
    if (w < 2f || h < 2f) return
    // ---------------------------------------------------------------- tower
    val cx = w / 2f
    val rx = min(w * 0.36f, h * 0.2f)
    val ry = rx * 0.3f
    val th = ry * 0.42f
    val spacing = h * 0.085f
    val topY = h * 0.32f
    val pillarHalf = rx * 0.17f
    val towerGone = t > FLASH
    val pillarGrow = ((t - 0.1f) / 0.6f).coerceIn(0f, 1f)
    val pillarTop = topY - spacing * 0.9f
    val pillarBottom = topY + 4 * spacing + spacing * 1.2f
    val pillarAlpha = if (towerGone) (1f - (t - FLASH) / 0.25f).coerceIn(0f, 1f) else 1f
    val baseY = pillarBottom

    fun ringY(i: Int): Float {
        val k = land((t - (RING_DROP + i * RING_STAGGER)) / 0.5f)
        val target = topY + i * spacing
        return -h * 0.2f + (target + h * 0.2f) * k
    }
    fun smashedAt(i: Int) = SMASH_START + i * SMASH_STEP

    if (pillarAlpha > 0f && pillarGrow > 0f) {
        val pt = pillarBottom - (pillarBottom - pillarTop) * pillarGrow
        drawRect(
            Brush.horizontalGradient(listOf(Color(0xFF2B1B5E), Color(0xFF6D5BD0), Color(0xFF2B1B5E)), startX = cx - pillarHalf, endX = cx + pillarHalf),
            Offset(cx - pillarHalf, pt), Size(pillarHalf * 2f, pillarBottom - pt), alpha = pillarAlpha,
        )
        // Neon bands on the pillar
        var y = pillarBottom - spacing * 0.5f
        while (y > pt) {
            drawRect(Color(0xFF22D3EE), Offset(cx - pillarHalf, y), Size(pillarHalf * 2f, 2.5f * density), alpha = 0.7f * pillarAlpha)
            y -= spacing * 0.5f
        }
        // Finial
        if (pillarGrow >= 1f) {
            drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFFC084FC), Color(0xFF7C3AED)), Offset(cx - pillarHalf * 0.3f, pillarTop - pillarHalf * 0.6f), pillarHalf * 1.4f), pillarHalf * 0.95f, Offset(cx, pillarTop - pillarHalf * 0.4f), alpha = pillarAlpha)
        }
        // Golden base plate
        drawOval(Color(0xFF8A5A0A), Offset(cx - rx * 1.1f, baseY - ry * 0.8f + th), Size(rx * 2.2f, ry * 1.6f), alpha = pillarAlpha)
        drawOval(Brush.verticalGradient(listOf(Color(0xFFFFE680), Color(0xFFF59E0B)), startY = baseY - ry, endY = baseY + ry), Offset(cx - rx * 1.1f, baseY - ry * 0.8f), Size(rx * 2.2f, ry * 1.6f), alpha = pillarAlpha)
    }

    for (i in 0 until 5) {
        if (t < RING_DROP + i * RING_STAGGER) continue
        if (t >= smashedAt(i)) continue
        drawRing(cx, ringY(i), rx, ry, th, pillarHalf, RING_COLORS[i], gapAt = 200f + i * 47f, danger = if (i == 1 || i == 3) 300f + i * 20f else null)
    }

    // Shards of smashed rings
    for (s in shards) {
        val dt = t - smashedAt(s.ring)
        if (dt <= 0f || dt > 1.1f) continue
        val y0 = topY + s.ring * spacing
        val x = cx + cos(s.a) * rx * 0.85f + s.vx * w * dt
        val y = y0 + sin(s.a) * ry + s.vy * h * dt + 1.6f * h * dt * dt
        val a = (1f - dt / 1.1f).coerceIn(0f, 1f)
        val sz = s.size * w * (1f - dt * 0.4f)
        val col = if (s.bright) Color.White else RING_COLORS[s.ring]
        rotate(s.spin * dt, Offset(x, y)) {
            drawRect(col, Offset(x - sz, y - sz * 0.45f), Size(sz * 2f, sz * 0.9f), alpha = a)
        }
        drawCircle(col.copy(alpha = 0.25f * a), sz * 1.6f, Offset(x, y))
    }

    // ---------------------------------------------------------------- ball
    val br = rx * 0.17f
    val bx = cx + rx * 0.62f
    val ring0 = topY + ry * 0.6f
    val restOn0 = ring0 - br
    var by = Float.NaN
    var squash = 1f
    var fire = false
    when {
        t < BALL_IN -> Unit
        t < FIRST_HIT -> {
            val k = (t - BALL_IN) / (FIRST_HIT - BALL_IN)
            by = -br * 2f + (restOn0 + br * 2f) * k * k
        }
        t < SMASH_START -> {
            val k = (t - FIRST_HIT) / (SMASH_START - FIRST_HIT)
            by = restOn0 - sin(k * PI.toFloat()) * spacing * 1.2f
            squash = 1f - 0.35f * exp(-(t - FIRST_HIT) * 30f)
            fire = t > BOUNCE_TOP
        }
        t < FLASH -> {
            val k = (t - SMASH_START) / (FLASH - SMASH_START)
            by = restOn0 + (baseY - br - restOn0) * k
            fire = true
            squash = 1.15f
        }
        else -> {
            by = baseY - br - ry * 0.4f + sin((t - FLASH) * 3f) * br * 0.25f
            fire = t - FLASH < 0.9f
        }
    }
    if (!by.isNaN()) {
        // Trail
        if (fire) {
            for (k in 1..10) {
                val ty = by - k * br * 0.55f
                val tr = br * (1f - k / 12f)
                drawCircle(lerp(Color(0xFFFFE680), Color(0xFFFF4D00), k / 10f).copy(alpha = 0.55f * (1f - k / 11f)), tr, Offset(bx + sin(t * 40f + k) * br * 0.12f, ty))
            }
        }
        drawCircle(Brush.radialGradient(listOf((if (fire) Color(0xFFFF8A2B) else Color(0xFFE879F9)).copy(alpha = 0.6f), Color.Transparent), Offset(bx, by), br * 3.2f), br * 3.2f, Offset(bx, by))
        val sx = 1f / squash
        drawOval(
            Brush.radialGradient(
                if (fire) listOf(Color.White, Color(0xFFFFD166), Color(0xFFFF6A00)) else listOf(Color.White, Color(0xFFEDE9FE), Color(0xFFA78BFA)),
                Offset(bx - br * 0.35f, by - br * 0.4f), br * 1.5f,
            ),
            Offset(bx - br * sx, by - br * squash), Size(br * 2f * sx, br * 2f * squash),
        )
    }

    // ---------------------------------------------------------------- flash & shockwave
    val ft = t - FLASH
    if (ft > 0f) {
        val a = (1f - ft / 0.5f).coerceIn(0f, 1f)
        if (a > 0f) drawRect(Color.White.copy(alpha = a * a * 0.85f))
        val rr = ft * w * 1.4f
        if (ft < 0.9f && rr > 1f) {
            drawOval(
                Color(0xFFFFE680).copy(alpha = (1f - ft / 0.9f) * 0.8f),
                Offset(cx - rr, baseY - rr * 0.3f), Size(rr * 2f, rr * 0.6f),
                style = Stroke(6f * density * (1f - ft / 0.9f) + 1f),
            )
        }
        // Halo behind the logo
        val logoY = h * 0.30f + 60f * density
        val ha = (ft / 0.6f).coerceIn(0f, 1f)
        blob(Color(0xFF8B5CF6), Offset(cx, logoY), w * 0.6f, 0.45f * ha)
        // Sparks bursting from the landing point, then twinkling around the logo
        for (s in sparks) {
            val st = ft * s[1]
            if (st > 1.6f) continue
            val dist = w * 0.55f * (1f - exp(-st * 3f))
            val px = cx + cos(s[0]) * dist
            val py = baseY + sin(s[0]) * dist * 0.5f - h * 0.25f * (1f - exp(-st * 2f))
            val al = (1f - st / 1.6f).coerceIn(0f, 1f) * (0.5f + 0.5f * sin(t * 20f + s[2] * 30f))
            drawCircle(lerp(Color(0xFFFFE680), Color(0xFFE879F9), s[2]).copy(alpha = al), (1.5f + 2.5f * s[2]) * density, Offset(px, py))
        }
    }
}

/** Ring outlines built once per screen size (at the origin) and moved with translate. */
private object RingPaths {
    var key = 0f
    val side = arrayOfNulls<Path>(5)
    val top = arrayOfNulls<Path>(5)
    val danger = arrayOfNulls<Path>(5)
}

private fun DrawScope.drawRing(cx: Float, cy: Float, rx: Float, ry: Float, th: Float, inner: Float, c: Color, gapAt: Float, danger: Float?) {
    val irx = inner * 1.15f
    val iry = irx * (ry / rx)
    fun annulus(dy: Float, start: Float, sweep: Float): Path = Path().apply {
        fillType = PathFillType.EvenOdd
        arcTo(Rect(cx - rx, -ry + dy, cx + rx, ry + dy), start, sweep, true)
        arcTo(Rect(cx - irx, -iry + dy, cx + irx, iry + dy), start + sweep, -sweep, false)
        close()
    }
    val gapW = 38f
    val start = gapAt + gapW
    val sweep = 360f - gapW
    val idx = (((gapAt - 200f) / 47f) + 0.5f).toInt().coerceIn(0, 4)
    val key = rx * 31f + cx
    if (RingPaths.key != key) {
        RingPaths.key = key
        for (k in 0 until 5) { RingPaths.side[k] = null; RingPaths.top[k] = null; RingPaths.danger[k] = null }
    }
    val side = RingPaths.side[idx] ?: annulus(th, start, sweep).also { RingPaths.side[idx] = it }
    val top = RingPaths.top[idx] ?: annulus(0f, start, sweep).also { RingPaths.top[idx] = it }
    val dangerPath = if (danger != null) RingPaths.danger[idx] ?: annulus(0f, danger, 40f).also { RingPaths.danger[idx] = it } else null
    translate(top = cy) {
        drawPath(side, Brush.verticalGradient(listOf(Palette.darker(c, 0.25f), Palette.darker(c, 0.55f)), startY = 0f, endY = ry + th))
        drawPath(top, Brush.verticalGradient(listOf(Palette.lighter(c, 0.35f), c, Palette.darker(c, 0.1f)), startY = -ry, endY = ry))
        if (dangerPath != null) drawPath(dangerPath, Brush.verticalGradient(listOf(Color(0xFFFF6B81), Color(0xFFE11D48)), startY = -ry, endY = ry))
    }
    // Rim highlight on the front edge
    drawArc(Color.White.copy(alpha = 0.35f), max(start, 10f), 160f, false, Offset(cx - rx, cy - ry), Size(rx * 2f, ry * 2f), style = Stroke(1.5f * density))
    // The pillar is in front of the far half of the ring.
    clipRect(top = cy - ry * 0.6f, bottom = cy) {
        drawRect(
            Brush.horizontalGradient(listOf(Color(0xFF2B1B5E), Color(0xFF6D5BD0), Color(0xFF2B1B5E)), startX = cx - inner, endX = cx + inner),
            Offset(cx - inner, cy - ry - 1f), Size(inner * 2f, ry + 1f),
        )
    }
}

private fun DrawScope.blob(color: Color, center: Offset, radius: Float, alpha: Float) {
    if (radius < 1f || alpha <= 0f) return
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), center, radius), radius, center)
}
