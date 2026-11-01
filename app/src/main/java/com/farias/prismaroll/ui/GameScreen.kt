package com.farias.prismaroll.ui

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.farias.prismaroll.R
import com.farias.prismaroll.ads.AdsManager
import com.farias.prismaroll.audio.Sfx
import com.farias.prismaroll.audio.SoundManager
import com.farias.prismaroll.data.Progress
import com.farias.prismaroll.game.GameHost
import com.farias.prismaroll.game.GameHost.Phase
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.math.sin

/** How far the ball moves sideways when the finger crosses the whole screen width (world units). */
private const val STEER_PER_WIDTH = 7.5f

@Composable
fun GameScreen(host: GameHost, ads: AdsManager, sound: SoundManager, onGreatMoment: () -> Unit, onHome: () -> Unit) {
    BackHandler {
        when {
            host.phase == Phase.PLAYING -> host.pauseGame(!host.paused)
            host.phase == Phase.DEAD -> { host.giveUp(); onHome() }
            else -> onHome()
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        // Touch anywhere to roll, slide the finger left and right to steer.
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        host.steer(0f)
                        var lastX = down.position.x
                        while (true) {
                            val ev = awaitPointerEvent()
                            val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                            if (!c.pressed) break
                            val dx = c.position.x - lastX
                            lastX = c.position.x
                            if (dx != 0f) {
                                c.consume()
                                host.steer(dx / widthPx * STEER_PER_WIDTH)
                            }
                        }
                    }
                },
        )

        PopupLayer(host)

        Column(
            Modifier
                .fillMaxWidth()
                // Status bar hidden while playing: keep clear of the camera notch only.
                .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Top))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassCircleButton(onClick = { host.pauseGame(true) }) { PauseIcon(Modifier.size(18.dp)) }
                Spacer(Modifier.width(12.dp))
                if (host.endless) DepthBar(host, Modifier.weight(1f)) else LevelBar(host, Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                Box(Modifier.width(44.dp), contentAlignment = Alignment.Center) {
                    if (host.shields > 0) Text("🛡" + if (host.shields > 1) host.shields.toString() else "", style = Styles.glow(20, Color(0xFF7DD3FC), FontWeight.Black))
                }
            }
            Spacer(Modifier.height(10.dp))
            ScoreText(host)
            if (host.coinsThisLevel > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CoinIcon(Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("+" + host.coinsThisLevel * GameHost.COIN_VALUE, style = Styles.plain(15, Palette.Gold, FontWeight.Black))
                }
            }
        }

        // Boss tower banner at the start
        if (host.boss && !host.touched && host.phase == Phase.PLAYING) BossBanner(Modifier.align(Alignment.Center).padding(bottom = 160.dp))

        // A mechanic shown for the first time
        if (host.notice != 0 && host.phase == Phase.PLAYING) {
            NoticeCard(host.notice, Modifier.align(Alignment.BottomCenter).padding(bottom = 210.dp, start = 24.dp, end = 24.dp)) { host.clearNotice() }
        }

        // Shield offer before the first move on hard moments
        if (!host.touched && host.phase == Phase.PLAYING && host.shields == 0 && ads.rewardedReady &&
            (host.boss || host.endless || host.failsHere >= 1)
        ) {
            ShieldOffer(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 80.dp)) {
                ads.showRewarded { host.addShield() }
            }
        }

        if (!host.touched && host.phase == Phase.PLAYING && (Progress.levelsWon < 3 || host.level <= 2)) {
            DragHint(Modifier.align(Alignment.BottomCenter).padding(bottom = 140.dp))
        } else if (host.phase == Phase.PLAYING && (host.waiting || !host.touched)) {
            TapToRoll(Modifier.align(Alignment.BottomCenter).padding(bottom = 150.dp))
        }

        Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding()) { BannerAd(ads) }

        when {
            host.paused -> PauseOverlay(host, ads, onHome)
            host.phase == Phase.DEAD -> DeadOverlay(host, ads, sound, onHome)
            host.phase == Phase.WON -> WinOverlay(host, ads, sound, onGreatMoment, onHome)
        }
    }
}

// ------------------------------------------------------------------ HUD

@Composable
private fun DepthBar(host: GameHost, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Text("∞", style = Styles.glow(24, Palette.Cyan, FontWeight.Black))
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.depth_x, host.depth), style = Styles.plain(18, Color.White, FontWeight.Black))
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.best_x, Progress.bestEndless), style = Styles.plain(13, Palette.TextSecondary, FontWeight.Bold))
    }
}

@Composable
private fun BossBanner(modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "boss")
    val s by t.animateFloat(0.95f, 1.06f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "s")
    Column(modifier.graphicsLayer { scaleX = s; scaleY = s }, horizontalAlignment = Alignment.CenterHorizontally) {
        CrownIcon(Modifier.size(54.dp, 40.dp))
        Text(stringResource(R.string.boss_tower), style = Styles.glow(30, Palette.Gold, FontWeight.Black))
        Text(stringResource(R.string.boss_hint), style = Styles.plain(14, Color.White, FontWeight.Bold))
    }
}

@Composable
private fun NoticeCard(textRes: Int, modifier: Modifier, onClose: () -> Unit) {
    LaunchedEffect(textRes) {
        delay(5500)
        onClose()
    }
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .widthIn(max = 380.dp)
            .clip(shape)
            .background(Color(0xE6140B33))
            .border(1.5.dp, Palette.Gold.copy(alpha = 0.7f), shape)
            .clickable(onClick = onClose)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.new_mechanic), style = Styles.glow(16, Palette.Gold, FontWeight.Black))
        Text(stringResource(textRes), style = Styles.plain(15, Color.White, FontWeight.SemiBold), textAlign = TextAlign.Center)
    }
}

@Composable
private fun ShieldOffer(modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFF0EA5E9), Color(0xFF6366F1))))
            .border(1.dp, Color.White.copy(alpha = 0.4f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🛡", style = Styles.plain(20, Color.White, FontWeight.Black))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.shield_video), style = Styles.plain(15, Color.White, FontWeight.ExtraBold))
    }
}

@Composable
private fun LevelBar(host: GameHost, modifier: Modifier) {
    val theme = Palette.theme
    val p by animateFloatAsState(host.progress, tween(260), label = "progress")
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (host.daily) {
            Text("★", style = Styles.glow(22, Palette.Gold, FontWeight.Black))
        } else {
            LevelBadge(host.level, theme.accent, filled = true)
        }
        Spacer(Modifier.width(8.dp))
        Canvas(
            Modifier
                .weight(1f)
                .height(14.dp),
        ) {
            val r = CornerRadius(size.height / 2f)
            drawRoundRect(Color(0x40000000), size = size, cornerRadius = r)
            drawRoundRect(Color(0x33FFFFFF), size = size, cornerRadius = r, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
            val w = size.width * p
            if (w > 1f) {
                drawRoundRect(
                    Brush.horizontalGradient(listOf(theme.accent, theme.accent2), endX = size.width.coerceAtLeast(1f)),
                    size = Size(w.coerceAtLeast(size.height), size.height),
                    cornerRadius = r,
                )
                drawRoundRect(
                    Color.White.copy(alpha = 0.35f),
                    topLeft = Offset(size.height * 0.3f, size.height * 0.18f),
                    size = Size((w - size.height * 0.6f).coerceAtLeast(0f), size.height * 0.22f),
                    cornerRadius = r,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        if (host.daily) {
            Text(stringResource(R.string.daily_short), style = Styles.plain(14, Palette.Gold, FontWeight.Black))
        } else {
            LevelBadge(host.level + 1, theme.accent2, filled = p >= 1f)
        }
    }
}

@Composable
private fun LevelBadge(n: Int, color: Color, filled: Boolean) {
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (filled) Brush.verticalGradient(listOf(Palette.lighter(color, 0.2f), Palette.darker(color, 0.25f))) else Brush.verticalGradient(listOf(Color(0x40000000), Color(0x40000000))))
            .border(2.dp, color.copy(alpha = if (filled) 0.9f else 0.6f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        FitText(n.toString(), Styles.plain(16, Color.White, FontWeight.Black), minSp = 9f)
    }
}

@Composable
private fun ScoreText(host: GameHost) {
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(host.score) {
        if (host.score == 0) return@LaunchedEffect
        pulse.snapTo(1.18f)
        pulse.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 500f))
    }
    val color = if (host.boosting) Color(0xFF67E8F9) else Color.White
    Text(
        host.score.toString(),
        style = Styles.glow(46, color, FontWeight.Black),
        modifier = Modifier.graphicsLayer {
            scaleX = pulse.value
            scaleY = pulse.value
        },
    )
}

@Composable
private fun PopupLayer(host: GameHost) {
    // The clock ticks only while there are texts on screen, and is read only in the layer/offset blocks:
    // the texts move every frame without recomposing anything.
    val now = remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    val any = host.popups.isNotEmpty()
    LaunchedEffect(any) {
        while (any) {
            withFrameMillis { now.longValue = SystemClock.uptimeMillis() }
            if (host.popups.all { now.longValue - it.born > 1150 }) {
                host.popups.clear()
                break
            }
        }
    }
    val density = LocalDensity.current
    val rise60 = with(density) { 60.dp.toPx() }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        for (p in host.popups) {
            key(p.id) {
                val (color, size) = when (p.kind) {
                    1 -> Palette.theme.accent to 30
                    2 -> Palette.Gold to 26
                    3 -> Color(0xFFFF8A2B) to 34
                    4 -> Color.White to 40
                    else -> Color.White to 24
                }
                Text(
                    p.text,
                    style = Styles.glow(size, color, FontWeight.Black),
                    modifier = Modifier
                        .offset {
                            val age = (now.longValue - p.born) / 1000f
                            IntOffset((p.x * w).roundToInt() + 60, (p.y * h - rise60 * age).roundToInt() - 90)
                        }
                        .graphicsLayer {
                            val age = (now.longValue - p.born) / 1000f
                            alpha = (1f - ((age - 0.6f) / 0.5f)).coerceIn(0f, 1f)
                            val pop = if (age < 0.15f) 0.6f + age / 0.15f * 0.6f else 1.2f - ((age - 0.15f) * 0.4f).coerceAtMost(0.2f)
                            scaleX = pop
                            scaleY = pop
                        },
                )
            }
        }
    }
}

/** "Touch to roll" pulsing (before the start and after a continue). */
@Composable
private fun TapToRoll(modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "tap")
    val a by t.animateFloat(0.45f, 1f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "a")
    Text(
        stringResource(R.string.tap_to_roll),
        style = Styles.glow(20, Color.White, FontWeight.Black),
        modifier = modifier.graphicsLayer { alpha = a },
    )
}

@Composable
private fun DragHint(modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "hint")
    val x by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "x")
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(150.dp, 60.dp)) {
            val cy = size.height * 0.45f
            val cx = size.width / 2f + x * size.width * 0.32f
            // Track
            drawLine(Color.White.copy(alpha = 0.25f), Offset(size.width * 0.12f, cy), Offset(size.width * 0.88f, cy), strokeWidth = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            // Finger
            drawCircle(Color.White.copy(alpha = 0.18f), 22.dp.toPx(), Offset(cx, cy))
            drawCircle(Color.White, 12.dp.toPx(), Offset(cx, cy))
        }
        Text(
            stringResource(R.string.drag_hint),
            style = Styles.plain(17, Color.White, FontWeight.Bold),
            textAlign = TextAlign.Center,
        )
    }
}

// ------------------------------------------------------------------ overlays

@Composable
private fun PauseOverlay(host: GameHost, ads: AdsManager, onHome: () -> Unit) {
    DimOverlay {
        GlassPanel {
            Text(stringResource(R.string.paused), style = Styles.gradient(34))
            Spacer(Modifier.height(4.dp))
            SettingsToggles()
            Spacer(Modifier.height(8.dp))
            PrimaryButton(stringResource(R.string.resume)) { host.pauseGame(false) }
            SecondaryButton(stringResource(R.string.restart)) {
                host.pauseGame(false)
                ads.onNewGame { host.retry() }
            }
            SecondaryButton(stringResource(R.string.home)) {
                host.pauseGame(false)
                onHome()
            }
            if (ads.privacyOptionsRequired) LinkText(stringResource(R.string.privacy_options)) { ads.showPrivacyOptions() }
        }
    }
}

/** Waits a moment so the player sees what happened before the panel covers it. */
@Composable
private fun Delayed(ms: Long, content: @Composable () -> Unit) {
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(ms)
        show = true
    }
    AnimatedVisibility(show, enter = fadeIn(tween(250)), exit = fadeOut()) { content() }
}

@Composable
private fun DeadOverlay(host: GameHost, ads: AdsManager, sound: SoundManager, onHome: () -> Unit) {
    Delayed(850) {
        DimOverlay {
            GlassPanel {
                Text(stringResource(R.string.so_close), style = Styles.gradient(34))
                if (host.endless) {
                    Text(stringResource(R.string.depth_x, host.depth), style = Styles.glow(40, Color.White, FontWeight.Black))
                    if (host.newBest) NewBestBadge() else Text(stringResource(R.string.best_x, Progress.bestEndless), style = Styles.plain(16, Palette.TextSecondary, FontWeight.Bold))
                    CoinsEarned(host.lastReward)
                } else {
                    val pct = (host.deathProgress * 100).roundToInt()
                    Text(stringResource(R.string.percent_done, pct), style = Styles.plain(17, Palette.TextSecondary, FontWeight.Bold))
                    ProgressRing(host.deathProgress)
                }
                if (!host.revived) {
                    if (ads.rewardedReady) {
                        PrimaryButton(stringResource(R.string.continue_video)) { ads.showRewarded { host.revive() } }
                    } else {
                        Text(stringResource(R.string.loading_video), style = Styles.plain(14, Palette.TextSecondary))
                    }
                }
                SecondaryButton(stringResource(R.string.try_again)) {
                    host.giveUp()
                    sound.play(Sfx.WHOOSH)
                    ads.onNewGame { host.retry() }
                }
                LinkText(stringResource(R.string.home)) {
                    host.giveUp()
                    onHome()
                }
            }
        }
    }
}

@Composable
private fun ProgressRing(value: Float) {
    val theme = Palette.theme
    val anim = remember { Animatable(0f) }
    LaunchedEffect(value) { anim.animateTo(value, tween(900)) }
    Canvas(Modifier.size(110.dp)) {
        val stroke = 12.dp.toPx()
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(Color(0x22FFFFFF), 0f, 360f, false, Offset(inset, inset), arcSize, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
        drawArc(
            Brush.sweepGradient(listOf(theme.accent, theme.accent2, theme.accent)),
            -90f, 360f * anim.value, false, Offset(inset, inset), arcSize,
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
        )
    }
}

@Composable
private fun WinOverlay(host: GameHost, ads: AdsManager, sound: SoundManager, onGreatMoment: () -> Unit, onHome: () -> Unit) {
    LaunchedEffect(Unit) {
        if (!host.daily && host.level >= 6 && host.level % 4 == 2) onGreatMoment()
    }
    Delayed(1300) {
        var bonus by remember { mutableStateOf(0) }
        LaunchedEffect(Unit) { sound.play(Sfx.COIN) }
        DimOverlay {
            GlassPanel {
                Text(
                    when {
                        host.daily -> stringResource(R.string.daily_complete)
                        host.boss -> stringResource(R.string.boss_beaten)
                        else -> stringResource(R.string.level_complete)
                    },
                    style = Styles.gradient(30),
                    textAlign = TextAlign.Center,
                )
                if (!host.daily) Text(stringResource(R.string.level_x, host.level), style = Styles.plain(17, Palette.TextSecondary, FontWeight.Bold))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text(host.score.toString(), style = Styles.glow(44, Color.White, FontWeight.Black))
                    if (host.newBest) {
                        Spacer(Modifier.width(10.dp))
                        NewBestBadge()
                    }
                }
                CoinsEarned(host.lastReward + bonus, big = true)
                if (bonus == 0 && host.lastReward > 0 && ads.rewardedReady) {
                    SecondaryButton(stringResource(R.string.triple_video)) {
                        ads.showRewarded {
                            val extra = host.lastReward * 2
                            Progress.addCoins(extra)
                            bonus = extra
                            sound.play(Sfx.COIN)
                        }
                    }
                }
                if (host.daily) {
                    PrimaryButton(stringResource(R.string.home), onClick = onHome)
                } else {
                    PrimaryButton(stringResource(R.string.next_level_n, Progress.level)) {
                        sound.play(Sfx.WHOOSH)
                        ads.onNewGame { host.next() }
                    }
                    LinkText(stringResource(R.string.home), onClick = onHome)
                }
            }
        }
    }
}

@Composable
private fun NewBestBadge() {
    val t = rememberInfiniteTransition(label = "best")
    val s by t.animateFloat(0.92f, 1.08f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "s")
    Box(
        Modifier
            .graphicsLayer { scaleX = s; scaleY = s; rotationZ = 6f * sin(s * 6f) }
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFFFFE680), Palette.Gold, Color(0xFFF59E0B))))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(stringResource(R.string.new_best), style = Styles.plain(13, Color(0xFF3B2300), FontWeight.Black))
    }
}
