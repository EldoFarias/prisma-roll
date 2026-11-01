package com.farias.prismaroll.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farias.prismaroll.R
import com.farias.prismaroll.ads.AdsManager
import com.farias.prismaroll.audio.Sfx
import com.farias.prismaroll.audio.SoundManager
import com.farias.prismaroll.data.Achievements
import com.farias.prismaroll.data.Progress
import com.farias.prismaroll.data.randomLockedBall
import com.farias.prismaroll.gl.Looks
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

// ------------------------------------------------------------------------------ achievements

@Composable
fun AchievementsDialog(sound: SoundManager, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    DimOverlay {
        GlassPanel {
            Text(stringResource(R.string.achievements), style = Styles.gradient(30))
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Ready to claim first, then in progress, then claimed.
                val sorted = Achievements.all.sortedBy {
                    when {
                        it.id in Progress.claimedAchievements -> 2
                        Achievements.done(it) -> 0
                        else -> 1
                    }
                }
                for (a in sorted) {
                    val claimed = a.id in Progress.claimedAchievements
                    val done = Achievements.done(a)
                    val shape = RoundedCornerShape(14.dp)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(if (done && !claimed) Color(0x3334D399) else Palette.Glass)
                            .border(1.dp, if (done && !claimed) Color(0xFF34D399) else Palette.GlassBorder, shape)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TrophyIcon(Modifier.size(28.dp).graphicsLayer { alpha = if (claimed || done) 1f else 0.4f })
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(a.textRes, a.target), style = Styles.plain(14, Palette.TextPrimary, FontWeight.Bold))
                            val p = Achievements.progress(a)
                            Canvas(Modifier.fillMaxWidth().height(6.dp).padding(top = 2.dp)) {
                                drawRoundRect(Color(0x33FFFFFF), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height))
                                drawRoundRect(
                                    Color(0xFF34D399), size = Size(size.width * p / a.target, size.height),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height),
                                )
                            }
                            Text("$p / ${a.target}", style = Styles.plain(11, Palette.TextSecondary))
                        }
                        Spacer(Modifier.width(8.dp))
                        when {
                            claimed -> Text("✓", style = Styles.glow(20, Color(0xFF34D399), FontWeight.Black))
                            done -> Box(
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Brush.horizontalGradient(listOf(Color(0xFF34D399), Color(0xFF059669))))
                                    .clickable {
                                        if (Progress.claimAchievement(a.id, a.reward)) sound.play(Sfx.COIN)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) { Text("+${a.reward}", style = Styles.plain(14, Color.White, FontWeight.Black)) }
                            else -> Row(verticalAlignment = Alignment.CenterVertically) {
                                CoinIcon(Modifier.size(14.dp))
                                Spacer(Modifier.width(3.dp))
                                Text(a.reward.toString(), style = Styles.plain(13, Palette.Gold, FontWeight.Bold))
                            }
                        }
                    }
                }
            }
            PrimaryButton(stringResource(R.string.got_it), onClick = onClose)
        }
    }
}

// ------------------------------------------------------------------------------ roulette

/** One slice of the wheel: coins, or a random locked ball (coins = 0). */
private class Prize(val coins: Int, val color: Color)

private val PRIZES = listOf(
    Prize(20, Color(0xFF22D3EE)), Prize(60, Color(0xFF8B5CF6)), Prize(30, Color(0xFFF472B6)), Prize(150, Color(0xFFF59E0B)),
    Prize(25, Color(0xFF34D399)), Prize(0, Color(0xFFE11D48)), Prize(40, Color(0xFF60A5FA)), Prize(300, Color(0xFFFACC15)),
)
// Chance weights (big prizes are rare).
private val WEIGHTS = intArrayOf(22, 14, 20, 6, 20, 4, 12, 2)

@Composable
fun RouletteDialog(ads: AdsManager, sound: SoundManager, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val rot = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var spinning by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }

    fun spin(video: Boolean) {
        if (spinning) return
        spinning = true
        result = null
        Progress.useRouletteSpin(video)
        // Pick by weight, then aim the wheel so that slice ends under the pointer (top).
        var r = (Math.random() * WEIGHTS.sum()).toInt()
        var idx = 0
        while (r >= WEIGHTS[idx]) { r -= WEIGHTS[idx]; idx++ }
        val seg = 360f / PRIZES.size
        val target = rot.value - rot.value % 360f + 360f * 6 + (360f - idx * seg)
        sound.play(Sfx.WHOOSH)
        scope.launch {
            rot.animateTo(target, tween(4200, easing = CubicBezierEasing(0.1f, 0.6f, 0.2f, 1f)))
            val p = PRIZES[idx]
            if (p.coins > 0) {
                Progress.addCoins(p.coins)
                result = context.getString(R.string.coins_reward, p.coins)
            } else {
                val id = randomLockedBall()
                if (id != null) {
                    Progress.giveBall(id)
                    result = context.getString(R.string.won_ball, context.getString(Looks.ball(id).nameRes))
                } else {
                    Progress.addCoins(500)
                    result = context.getString(R.string.coins_reward, 500)
                }
            }
            sound.play(Sfx.RECORD)
            spinning = false
        }
    }

    DimOverlay {
        GlassPanel {
            Text(stringResource(R.string.roulette), style = Styles.gradient(30))
            Box(Modifier.size(270.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize().graphicsLayer { rotationZ = rot.value }) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val r = size.minDimension / 2f
                    val seg = 360f / PRIZES.size
                    for ((i, p) in PRIZES.withIndex()) {
                        // Slice i is centred at angle i*seg (0 = top), clockwise.
                        drawArc(
                            Brush.radialGradient(listOf(Palette.lighter(p.color, 0.25f), p.color), c, r),
                            -90f + i * seg - seg / 2f, seg, true, Offset(c.x - r, c.y - r), Size(r * 2, r * 2),
                        )
                        drawArc(Color.White.copy(alpha = 0.35f), -90f + i * seg - seg / 2f, seg, true, Offset(c.x - r, c.y - r), Size(r * 2, r * 2), style = Stroke(2f))
                    }
                    drawCircle(Color.White.copy(alpha = 0.8f), r, c, style = Stroke(6f))
                    // Lights around the rim
                    for (k in 0 until 24) {
                        val a = Math.toRadians(k * 15.0).toFloat()
                        drawCircle(if (k % 2 == 0) Color(0xFFFFF3B0) else Color.White, r * 0.025f, Offset(c.x + cos(a) * r * 0.95f, c.y + sin(a) * r * 0.95f))
                    }
                }
                // Prize labels (turn with the wheel)
                Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = rot.value }) {
                    for ((i, p) in PRIZES.withIndex()) {
                        Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = i * 360f / PRIZES.size }, contentAlignment = Alignment.TopCenter) {
                            Text(
                                if (p.coins > 0) p.coins.toString() else "🎁",
                                style = Styles.glow(18, Color.White, FontWeight.Black),
                                modifier = Modifier.padding(top = 26.dp),
                            )
                        }
                    }
                }
                // Hub and pointer
                Canvas(Modifier.fillMaxSize()) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(Brush.radialGradient(listOf(Color.White, Palette.Gold, Color(0xFFB45309)), c, size.minDimension * 0.12f), size.minDimension * 0.09f, c)
                    val path = Path().apply {
                        moveTo(c.x - 16f, 0f); lineTo(c.x + 16f, 0f); lineTo(c.x, 40f); close()
                    }
                    drawPath(path, Color.White)
                    drawPath(path, Color(0xFFDC2626), style = Stroke(3f))
                }
            }
            result?.let { Text(it, style = Styles.glow(20, Palette.Gold, FontWeight.Black)) }
            when {
                Progress.rouletteFreeAvailable() -> PrimaryButton(stringResource(R.string.spin_free), enabled = !spinning) { spin(false) }
                Progress.rouletteVideoLeft() > 0 -> PrimaryButton(stringResource(R.string.spin_video, Progress.rouletteVideoLeft()), enabled = !spinning) {
                    if (ads.rewardedReady) ads.showRewarded { spin(true) }
                    else Toast.makeText(context, R.string.no_video, Toast.LENGTH_SHORT).show()
                }
                else -> Text(stringResource(R.string.roulette_tomorrow), style = Styles.plain(15, Palette.TextSecondary, FontWeight.Bold))
            }
            LinkText(stringResource(R.string.close), onClick = onClose)
        }
    }
}
