package com.farias.prismaroll.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farias.prismaroll.R
import com.farias.prismaroll.ads.AdsManager
import com.farias.prismaroll.audio.Sfx
import com.farias.prismaroll.audio.SoundManager
import com.farias.prismaroll.data.Progress

private val Green = Color(0xFF34D399)

// ------------------------------------------------------------------------------ missions

@Composable
fun MissionsDialog(sound: SoundManager, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val missions = Progress.todaysMissions()
    DimOverlay {
        GlassPanel {
            Text(stringResource(R.string.missions), style = Styles.gradient(30))
            missions.forEachIndexed { i, m ->
                val progress = Progress.missionProgressOf(i)
                val claimed = Progress.missionClaimedOf(i)
                val done = progress >= m.target
                MissionRow(
                    text = stringResource(m.type.textRes, m.target),
                    progress = progress,
                    target = m.target,
                    reward = m.reward,
                    done = done,
                    claimed = claimed,
                ) {
                    if (Progress.claimMission(i) > 0) sound.play(Sfx.COIN)
                }
            }
            Spacer(Modifier.height(4.dp))
            PrimaryButton(stringResource(R.string.got_it)) { onClose() }
        }
    }
}

@Composable
private fun MissionRow(
    text: String,
    progress: Int,
    target: Int,
    reward: Int,
    done: Boolean,
    claimed: Boolean,
    onClaim: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Palette.Glass)
            .border(1.dp, if (done && !claimed) Green else Palette.GlassBorder, shape)
            .padding(12.dp),
    ) {
        Text(text, style = Styles.plain(15, Palette.TextPrimary, FontWeight.Bold))
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0x33FFFFFF)),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((progress.toFloat() / target).coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(Palette.Cyan, Green))),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text("$progress/$target", style = Styles.plain(13, Palette.TextSecondary, FontWeight.Bold))
            Spacer(Modifier.width(10.dp))
            val pill = RoundedCornerShape(12.dp)
            Row(
                modifier = Modifier
                    .height(34.dp)
                    .clip(pill)
                    .background(
                        when {
                            claimed -> Color(0x2234D399)
                            done -> Green
                            else -> Color(0x22FACC15)
                        },
                    )
                    .clickable(enabled = done && !claimed, onClick = onClaim)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (claimed) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Green, modifier = Modifier.size(18.dp))
                } else {
                    CoinIcon(Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (done) stringResource(R.string.claim) + " $reward" else reward.toString(),
                        style = Styles.plain(14, if (done) Color(0xFF052E1C) else Palette.Gold, FontWeight.Black),
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------ daily chest

@Composable
fun ChestDialog(ads: AdsManager, sound: SoundManager, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val available = Progress.chestAvailable()
    val today = Progress.chestIndex()
    val rewards = Progress.CHEST_REWARDS
    // Days already collected in the current run (shown with a check).
    val collectedBefore = if (available) today else -1

    fun open(multiplier: Int) {
        if (Progress.openChest(multiplier) > 0) sound.play(Sfx.RECORD, 0.8f)
        onClose()
    }

    DimOverlay {
        GlassPanel {
            Text(stringResource(R.string.daily_reward), style = Styles.gradient(28))
            ChestIcon(Modifier.size(84.dp), shaking = available)
            // 7 days: 4 on the first row, 3 on the second.
            listOf(0..3, 4..6).forEach { range ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in range) {
                        DayTile(
                            day = i + 1,
                            reward = rewards[i],
                            isToday = available && i == today,
                            collected = i < collectedBefore,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (range.count() == 3) Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(4.dp))
            if (available) {
                if (ads.adsReady && ads.rewardedReady) {
                    PrimaryButton(stringResource(R.string.collect_double, rewards[today] * 2)) {
                        ads.showRewarded { open(2) }
                    }
                    SecondaryButton(stringResource(R.string.collect, rewards[today])) { open(1) }
                } else {
                    PrimaryButton(stringResource(R.string.collect, rewards[today])) { open(1) }
                }
            } else {
                Text(stringResource(R.string.chest_tomorrow), style = Styles.plain(15, Palette.TextSecondary, FontWeight.Bold))
                PrimaryButton(stringResource(R.string.got_it)) { onClose() }
            }
        }
    }
}

@Composable
private fun DayTile(day: Int, reward: Int, isToday: Boolean, collected: Boolean, modifier: Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .height(78.dp)
            .clip(shape)
            .background(
                when {
                    isToday -> Brush.verticalGradient(listOf(Color(0x66FACC15), Color(0x33F59E0B)))
                    else -> Brush.verticalGradient(listOf(Palette.Glass, Palette.Glass))
                },
            )
            .border(if (isToday) 2.dp else 1.dp, if (isToday) Palette.Gold else Palette.GlassBorder, shape)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Text(stringResource(R.string.day_x, day), style = Styles.plain(12, Palette.TextSecondary, FontWeight.Bold), maxLines = 1)
        if (collected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Green, modifier = Modifier.size(22.dp))
        } else {
            CoinIcon(Modifier.size(20.dp))
        }
        Text(reward.toString(), style = Styles.plain(14, if (isToday) Palette.Gold else Palette.TextPrimary, FontWeight.Black))
    }
}

/** A little treasure chest that wiggles while there is something to collect. */
@Composable
fun ChestIcon(modifier: Modifier = Modifier, shaking: Boolean = false) {
    val transition = rememberInfiniteTransition(label = "chest")
    val t by transition.animateFloat(
        -1f, 1f, infiniteRepeatable(tween(260), RepeatMode.Reverse), label = "wiggle",
    )
    Canvas(modifier.graphicsLayer { rotationZ = if (shaking) t * 6f else 0f }) {
        val w = size.width
        val h = size.height
        val bodyTop = h * 0.42f
        // glow
        if (shaking) {
            drawCircle(
                Brush.radialGradient(listOf(Palette.Gold.copy(alpha = 0.45f), Color.Transparent), Offset(w / 2f, h / 2f), w * 0.6f),
                w * 0.6f, Offset(w / 2f, h / 2f),
            )
        }
        // body
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0xFFB45309), Color(0xFF7C2D12)), bodyTop, h * 0.92f),
            Offset(w * 0.1f, bodyTop), Size(w * 0.8f, h * 0.5f), CornerRadius(w * 0.06f),
        )
        // lid
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0xFFD97706), Color(0xFF9A3412)), h * 0.18f, bodyTop),
            Offset(w * 0.08f, h * 0.18f), Size(w * 0.84f, h * 0.28f), CornerRadius(w * 0.12f),
        )
        // gold bands
        drawRect(Palette.Gold, Offset(w * 0.1f, bodyTop - h * 0.03f), Size(w * 0.8f, h * 0.06f))
        drawRect(Palette.Gold, Offset(w * 0.44f, h * 0.18f), Size(w * 0.12f, h * 0.74f))
        // lock
        drawRoundRect(Color(0xFFFFE680), Offset(w * 0.41f, bodyTop - h * 0.02f), Size(w * 0.18f, h * 0.16f), CornerRadius(w * 0.03f))
        drawCircle(Color(0xFF7C2D12), w * 0.025f, Offset(w * 0.5f, bodyTop + h * 0.06f))
    }
}
