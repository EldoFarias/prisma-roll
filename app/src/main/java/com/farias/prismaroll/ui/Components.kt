package com.farias.prismaroll.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farias.prismaroll.audio.Haptic

fun easeOutBack(t: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val x = t - 1f
    return 1f + c3 * x * x * x + c1 * x * x
}

fun performHaptic(view: View, kind: Haptic) {
    val constant = when (kind) {
        Haptic.TICK -> HapticFeedbackConstants.CLOCK_TICK
        Haptic.PLACE -> HapticFeedbackConstants.KEYBOARD_TAP
        Haptic.CLEAR -> if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
        Haptic.COMBO -> HapticFeedbackConstants.LONG_PRESS
        Haptic.HEAVY -> HapticFeedbackConstants.LONG_PRESS
    }
    view.performHapticFeedback(constant)
}

object Styles {
    val brandBrush = Brush.linearGradient(listOf(Palette.Cyan, Palette.Magenta))

    @OptIn(ExperimentalTextApi::class)
    fun gradient(sizeSp: Int, glow: Color = Palette.Magenta) = TextStyle(
        brush = brandBrush,
        fontSize = sizeSp.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.5).sp,
        shadow = Shadow(color = glow.copy(alpha = 0.55f), offset = Offset.Zero, blurRadius = 30f),
    )

    fun glow(sizeSp: Int, color: Color, weight: FontWeight = FontWeight.ExtraBold) = TextStyle(
        color = color,
        fontSize = sizeSp.sp,
        fontWeight = weight,
        shadow = Shadow(color = color.copy(alpha = 0.8f), offset = Offset.Zero, blurRadius = 22f),
    )

    fun plain(sizeSp: Int, color: Color = Palette.TextPrimary, weight: FontWeight = FontWeight.SemiBold) = TextStyle(
        color = color,
        fontSize = sizeSp.sp,
        fontWeight = weight,
    )
}

/** Top padding that keeps clear of the status bar (if shown) and the camera notch. */
@Composable
fun Modifier.safeTop(): Modifier = this.windowInsetsPadding(
    androidx.compose.foundation.layout.WindowInsets.statusBars
        .union(androidx.compose.foundation.layout.WindowInsets.displayCutout)
        .only(androidx.compose.foundation.layout.WindowInsetsSides.Top),
)

/** Padding for system bars (if shown) and the camera notch on every side. */
@Composable
fun Modifier.safeBars(): Modifier = this.windowInsetsPadding(
    androidx.compose.foundation.layout.WindowInsets.systemBars
        .union(androidx.compose.foundation.layout.WindowInsets.displayCutout),
)

/** Swallows touches so nothing behind an overlay reacts. */
fun Modifier.blockTouches(): Modifier = this.clickable(
    interactionSource = MutableInteractionSource(),
    indication = null,
    onClick = {},
)

@Composable
fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(shape)
            .background(
                if (enabled) Brush.horizontalGradient(listOf(Color(0xFF06B6D4), Color(0xFF8B5CF6), Color(0xFFDB2777)))
                else Brush.horizontalGradient(listOf(Color(0xFF3A3560), Color(0xFF3A3560)))
            )
            .border(1.dp, Color.White.copy(alpha = 0.25f), shape)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Styles.plain(18, if (enabled) Color.White else Palette.TextSecondary, FontWeight.ExtraBold))
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .background(Palette.Glass)
            .border(1.dp, Palette.GlassBorder, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Styles.plain(17, Palette.TextPrimary, FontWeight.Bold))
    }
}

@Composable
fun GlassCircleButton(size: Dp = 44.dp, onClick: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Palette.Glass)
            .border(1.dp, Palette.GlassBorder, CircleShape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
fun PauseIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width * 0.28f
        val r = CornerRadius(w / 2f)
        drawRoundRect(Palette.TextPrimary, Offset(size.width * 0.14f, 0f), Size(w, size.height), r)
        drawRoundRect(Palette.TextPrimary, Offset(size.width * 0.58f, 0f), Size(w, size.height), r)
    }
}

@Composable
fun CrownIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(0.05f * w, 0.78f * h)
            lineTo(0.0f * w, 0.25f * h)
            lineTo(0.30f * w, 0.50f * h)
            lineTo(0.50f * w, 0.10f * h)
            lineTo(0.70f * w, 0.50f * h)
            lineTo(1.0f * w, 0.25f * h)
            lineTo(0.95f * w, 0.78f * h)
            close()
        }
        drawPath(path, Brush.verticalGradient(listOf(Color(0xFFFFE680), Palette.Gold, Color(0xFFD97706))))
        drawRoundRect(
            Palette.Gold,
            Offset(0.05f * w, 0.84f * h),
            Size(0.9f * w, 0.14f * h),
            CornerRadius(0.05f * h),
        )
    }
}

@Composable
fun CoinIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2f
        val c = Offset(size.width / 2f, size.height / 2f)
        drawCircle(Brush.verticalGradient(listOf(Color(0xFFFFE680), Palette.Gold, Color(0xFFD97706))), r, c)
        drawCircle(Color(0xFFB45309), r * 0.72f, c, style = androidx.compose.ui.graphics.drawscope.Stroke(r * 0.12f))
        drawCircle(Color.White.copy(alpha = 0.7f), r * 0.16f, Offset(c.x - r * 0.35f, c.y - r * 0.4f))
    }
}

/** Gold pill with the coin balance. [onPlus] shows a small "+" for getting more coins. */
@Composable
fun CoinPill(coins: Int, onPlus: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(50)
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .height(40.dp)
            .clip(shape)
            .background(Palette.Glass)
            .border(1.dp, Palette.Gold.copy(alpha = 0.45f), shape)
            .then(if (onPlus != null) Modifier.clickable(onClick = onPlus) else Modifier)
            .padding(start = 8.dp, end = if (onPlus != null) 6.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoinIcon(Modifier.size(24.dp))
        androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
        Text(coins.toString(), style = Styles.glow(18, Palette.Gold))
        if (onPlus != null) {
            androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
            Box(
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(Color(0xFF34D399), Color(0xFF059669)))),
                contentAlignment = Alignment.Center,
            ) { Text("+", style = Styles.plain(18, Color.White, FontWeight.Black)) }
        }
    }
}

@Composable
fun BombIcon(modifier: Modifier = Modifier, tint: Color = Palette.TextPrimary) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val c = Offset(w * 0.45f, h * 0.58f)
        drawCircle(tint, w * 0.36f, c)
        drawCircle(Color.White.copy(alpha = 0.35f), w * 0.09f, Offset(c.x - w * 0.13f, c.y - w * 0.13f))
        drawLine(tint, Offset(w * 0.66f, h * 0.30f), Offset(w * 0.80f, h * 0.14f), strokeWidth = w * 0.09f)
        drawCircle(Palette.Gold, w * 0.09f, Offset(w * 0.84f, h * 0.10f))
    }
}

@Composable
fun UndoIcon(modifier: Modifier = Modifier, tint: Color = Palette.TextPrimary) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.12f
        drawArc(
            color = tint, startAngle = 180f, sweepAngle = 230f, useCenter = false,
            topLeft = Offset(w * 0.18f, h * 0.22f), size = Size(w * 0.66f, h * 0.66f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
        )
        val tip = Path().apply {
            moveTo(w * 0.02f, h * 0.48f)
            lineTo(w * 0.34f, h * 0.48f)
            lineTo(w * 0.18f, h * 0.22f)
            close()
        }
        drawPath(tip, tint)
    }
}

@Composable
fun ShuffleIcon(modifier: Modifier = Modifier, tint: Color = Palette.TextPrimary) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(w * 0.11f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawArc(tint, 200f, 140f, false, Offset(w * 0.12f, h * 0.12f), Size(w * 0.76f, h * 0.76f), style = stroke)
        drawArc(tint, 20f, 140f, false, Offset(w * 0.12f, h * 0.12f), Size(w * 0.76f, h * 0.76f), style = stroke)
        val a = Path().apply { moveTo(w * 0.70f, h * 0.02f); lineTo(w * 0.94f, h * 0.24f); lineTo(w * 0.64f, h * 0.30f); close() }
        val b = Path().apply { moveTo(w * 0.30f, h * 0.98f); lineTo(w * 0.06f, h * 0.76f); lineTo(w * 0.36f, h * 0.70f); close() }
        drawPath(a, tint)
        drawPath(b, tint)
    }
}

/** Full-screen dim layer that blocks touches behind it. */
@Composable
fun DimOverlay(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC05040F))
            .blockTouches()
            .safeBars(),
    ) {
        // Centered when it fits; scrolls on short screens instead of being cut off.
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        ) { content() }
    }
}

/** The premium glass panel used by every dialog. */
@Composable
fun GlassPanel(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .padding(24.dp)
            .widthIn(max = 380.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xF2251B52), Color(0xF2110B2A))))
            .border(1.dp, Palette.GlassBorder, shape)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/** Five-pointed star: gold when earned, dim glass when not. */
@Composable
fun StarIcon(filled: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outer = size.minDimension / 2f
        val inner = outer * 0.48f
        val path = Path()
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) outer else inner
            val a = Math.PI / 2 + i * Math.PI / 5
            val x = cx + (r * kotlin.math.cos(a)).toFloat()
            val y = cy - (r * kotlin.math.sin(a)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        if (filled) {
            drawPath(path, Brush.verticalGradient(listOf(Color(0xFFFFF3B0), Palette.Gold, Color(0xFFD97706))))
            drawPath(path, Color(0xFF92400E).copy(alpha = 0.6f), style = androidx.compose.ui.graphics.drawscope.Stroke(outer * 0.07f))
        } else {
            drawPath(path, Color(0x33FFFFFF))
            drawPath(path, Color(0x55FFFFFF), style = androidx.compose.ui.graphics.drawscope.Stroke(outer * 0.06f))
        }
    }
}

/** Single-line text that shrinks its font (down to [minSp]) instead of being cut off on narrow screens. */
@Composable
fun FitText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    minSp: Float = 10f,
) {
    val max = if (style.fontSize.isSp) style.fontSize.value else 16f
    androidx.compose.foundation.text.BasicText(
        text = text,
        modifier = modifier,
        style = style,
        maxLines = 1,
        autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(
            minFontSize = minSp.sp,
            maxFontSize = max.sp,
            stepSize = 0.5.sp,
        ),
    )
}

/** Draws [content] bigger by [scale] (dp and sp), used to enlarge controls on tablets. */
@Composable
fun Scaled(scale: Float, content: @Composable () -> Unit) {
    if (scale == 1f) {
        content()
        return
    }
    val d = androidx.compose.ui.platform.LocalDensity.current
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(d.density * scale, d.fontScale),
        content = content,
    )
}
