package com.farias.prismaroll.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.farias.prismaroll.ads.AdsManager
import com.farias.prismaroll.gl.Rarity
import com.farias.prismaroll.gl.Scene
import com.farias.prismaroll.gl.rarityOf
import com.farias.prismaroll.data.Trails
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.draw.drawBehind
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farias.prismaroll.R
import com.farias.prismaroll.audio.Sfx
import com.farias.prismaroll.audio.SoundManager
import com.farias.prismaroll.data.Progress
import com.farias.prismaroll.game.GameHost
import com.farias.prismaroll.gl.BallLook
import com.farias.prismaroll.gl.BallStyle
import com.farias.prismaroll.gl.Looks
import com.farias.prismaroll.gl.TowerLook

private fun col(f: FloatArray, a: Float = 1f) = Color(f[0], f[1], f[2], a)

/**
 * Towers and balls. The top of the screen stays clear: the live 3D tower behind shows the item
 * being looked at, so the player sees exactly what they buy.
 */
@Composable
fun ShopScreen(host: GameHost, ads: AdsManager, sound: SoundManager, onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var towerSel by remember { mutableStateOf(Progress.towerId) }
    var ballSel by remember { mutableStateOf(Progress.ballId) }
    var trailSel by remember { mutableStateOf(Progress.trailId) }
    var reveal by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    val context = LocalContext.current
    DisposableEffect(Unit) { onDispose { host.applyLooks() } }
    fun preview() = host.applyLooks(towerSel, ballSel, trailSel)
    BackHandler(onBack = onBack)

    // The shop slides up and covers the whole screen; only a framed window shows the live 3D preview.
    val slide = remember { Animatable(1f) }
    LaunchedEffect(Unit) { slide.animateTo(0f, spring(dampingRatio = 0.85f, stiffness = 380f)) }
    val panelBg = Brush.verticalGradient(listOf(Color(0xFF1B1240), Color(0xFF0B0720)))
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenH = maxHeight
        Column(
            Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = slide.value * size.height },
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF130C30))
                    .safeTop()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.shop), style = Styles.gradient(30), modifier = Modifier.weight(1f))
                CoinPill(Progress.coins, onPlus = {
                    if (ads.adsReady && ads.rewardedReady) {
                        ads.showRewarded { Progress.addCoins(VIDEO_COINS); sound.play(Sfx.COIN) }
                    } else Toast.makeText(context, R.string.no_video, Toast.LENGTH_SHORT).show()
                })
                Spacer(Modifier.width(10.dp))
                GlassCircleButton(onClick = onBack) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = Palette.TextPrimary)
                }
            }
            // Live preview window (the 3D view behind shows through here).
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(screenH * 0.36f)
                    .drawBehind {
                        val edge = 18.dp.toPx()
                        // Dim the scene behind the shop so the panel stands out.
                        drawRect(Color(0x4D07051A))
                        drawRect(Brush.verticalGradient(listOf(Color(0xFF130C30), Color.Transparent), endY = edge))
                        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF1B1240)), startY = size.height - edge, endY = size.height), topLeft = Offset(0f, size.height - edge), size = Size(size.width, edge))
                    },
            ) {
                Text(
                    stringResource(R.string.preview),
                    style = Styles.plain(12, Color.White, FontWeight.ExtraBold),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x66000000))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(panelBg)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TabChip(stringResource(R.string.towers), tab == 0, Modifier.weight(1f)) { tab = 0 }
                    TabChip(stringResource(R.string.balls), tab == 1, Modifier.weight(1f)) { tab = 1 }
                    TabChip(stringResource(R.string.trails), tab == 2, Modifier.weight(1f)) { tab = 2 }
                }
                val locked = if (tab == 0) Looks.towers.filter { !Progress.ownsTower(it.id) }.map { it.id }
                else Looks.balls.filter { !Progress.ownsBall(it.id) }.map { it.id }
                if (locked.isNotEmpty() && tab != 2) {
                    MysteryButton(MYSTERY_PRICE, Progress.coins >= MYSTERY_PRICE) {
                        if (!Progress.spend(MYSTERY_PRICE)) { sound.play(Sfx.INVALID); return@MysteryButton }
                        val id = locked.random()
                        if (tab == 0) {
                            Progress.giveTower(id); Progress.selectTower(id); towerSel = id
                        } else {
                            Progress.giveBall(id); Progress.selectBall(id); ballSel = id
                        }
                        preview()
                        sound.play(Sfx.RECORD)
                        reveal = (tab == 0) to id
                    }
                }
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (tab == 0) {
                        Looks.towers.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { t ->
                                    ItemCard(
                                        name = stringResource(t.nameRes), price = t.price, rarity = rarityOf(t.price),
                                        owned = Progress.ownsTower(t.id), inUse = Progress.towerId == t.id,
                                        selected = towerSel == t.id, modifier = Modifier.weight(1f),
                                        onClick = { towerSel = t.id; preview(); sound.play(Sfx.PICK, 0.6f) },
                                    ) { TowerThumb(t) }
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    } else if (tab == 2) {
                        Trails.all.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { tr ->
                                    ItemCard(
                                        name = stringResource(tr.nameRes), price = tr.price, rarity = rarityOf(tr.price),
                                        owned = Progress.ownsTrail(tr.id), inUse = Progress.trailId == tr.id,
                                        selected = trailSel == tr.id, modifier = Modifier.weight(1f),
                                        onClick = { trailSel = tr.id; preview(); sound.play(Sfx.PICK, 0.6f) },
                                    ) { TrailThumb(tr.style, Looks.ball(ballSel)) }
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    } else {
                        Looks.balls.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { b ->
                                    ItemCard(
                                        name = stringResource(b.nameRes), price = b.price, rarity = rarityOf(b.price),
                                        owned = Progress.ownsBall(b.id), inUse = Progress.ballId == b.id,
                                        selected = ballSel == b.id, modifier = Modifier.weight(1f),
                                        onClick = { ballSel = b.id; preview(); sound.play(Sfx.PICK, 0.6f) },
                                    ) { BallThumb(b) }
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
                // Action for the item being previewed (+ "try it" with a video for locked skins).
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        when (tab) {
                            0 -> {
                                val t = Looks.tower(towerSel)
                                ActionButton(Progress.ownsTower(t.id), Progress.towerId == t.id, t.price,
                                    onBuy = {
                                        if (Progress.buyTower(t.id, t.price)) { Progress.selectTower(t.id); sound.play(Sfx.COIN) } else sound.play(Sfx.INVALID)
                                    },
                                    onUse = { Progress.selectTower(t.id); sound.play(Sfx.PLACE) })
                            }
                            1 -> {
                                val b = Looks.ball(ballSel)
                                ActionButton(Progress.ownsBall(b.id), Progress.ballId == b.id, b.price,
                                    onBuy = {
                                        if (Progress.buyBall(b.id, b.price)) { Progress.selectBall(b.id); sound.play(Sfx.COIN) } else sound.play(Sfx.INVALID)
                                    },
                                    onUse = { Progress.selectBall(b.id); sound.play(Sfx.PLACE) })
                            }
                            else -> {
                                val tr = Trails.byId(trailSel)
                                ActionButton(Progress.ownsTrail(tr.id), Progress.trailId == tr.id, tr.price,
                                    onBuy = {
                                        if (Progress.buyTrail(tr.id, tr.price)) { Progress.selectTrail(tr.id); sound.play(Sfx.COIN) } else sound.play(Sfx.INVALID)
                                    },
                                    onUse = { Progress.selectTrail(tr.id); sound.play(Sfx.PLACE) })
                            }
                        }
                    }
                    val lockedSel = (tab == 0 && !Progress.ownsTower(towerSel)) || (tab == 1 && !Progress.ownsBall(ballSel))
                    if (lockedSel && ads.rewardedReady) {
                        Spacer(Modifier.width(10.dp))
                        TryButton {
                            ads.showRewarded {
                                if (tab == 0) Progress.startTrial(towerSel, "", TRIAL_GAMES) else Progress.startTrial("", ballSel, TRIAL_GAMES)
                                sound.play(Sfx.RECORD)
                                Toast.makeText(context, context.getString(R.string.trial_started, TRIAL_GAMES), Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
        reveal?.let { (isTower, id) -> RevealDialog(isTower, id) { reveal = null } }
    }
}

const val MYSTERY_PRICE = 750
const val TRIAL_GAMES = 3

@Composable
private fun TryButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .height(58.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF0EA5E9), Color(0xFF6366F1))))
            .border(1.dp, Color.White.copy(alpha = 0.35f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("▶", style = Styles.plain(14, Color.White, FontWeight.Black))
        Text(stringResource(R.string.try_skin), style = Styles.plain(13, Color.White, FontWeight.ExtraBold))
    }
}

/** A short comet in the trail's style. */
@Composable
fun TrailThumb(style: Int, ball: BallLook) {
    Canvas(Modifier.fillMaxSize(0.85f)) {
        val w = size.width
        val h = size.height
        if (w < 2f) return@Canvas
        val r = w * 0.14f
        val bx = w * 0.5f
        val by = h * 0.78f
        for (k in 0 until 9) {
            val f = k / 9f
            val y = by - (1f - f) * h * 0.62f
            val col = when (style) {
                Trails.RAINBOW -> Color.hsv((k * 40f) % 360f, 0.8f, 1f)
                Trails.BLUE_FIRE -> Color(0xFF4D9BFF)
                Trails.GOLD -> Color(0xFFFFD34D)
                Trails.GHOST -> Color.White
                else -> col(ball.trail)
            }
            val a = (if (style == Trails.GHOST) 0.25f else if (style == Trails.NEON) 0.8f else 0.5f) * f
            val rr = r * (0.4f + 0.8f * f) * (if (style == Trails.NEON || style == Trails.GHOST) 1.3f else 1f)
            drawCircle(col.copy(alpha = a), rr, Offset(bx, y))
        }
        if (style == Trails.STARS || style == Trails.GOLD) {
            for (k in 0 until 7) {
                val x = bx + ((k * 37) % 11 - 5) * w * 0.04f
                val y = by - ((k * 53) % 9) * h * 0.07f
                drawCircle(if (style == Trails.GOLD) Color(0xFFFFE680) else Color.White, w * 0.018f, Offset(x, y))
            }
        }
        drawCircle(Brush.radialGradient(listOf(Color.White, col(ball.c1)), Offset(bx - r * 0.3f, by - r * 0.3f), r * 1.4f), r, Offset(bx, by))
    }
}

@Composable
private fun MysteryButton(price: Int, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val t = rememberInfiniteTransition(label = "mystery")
    val shine by t.animateFloat(-0.3f, 1.3f, infiniteRepeatable(tween(1800)), label = "shine")
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFF7C3AED), Color(0xFFDB2777), Color(0xFFF59E0B))))
            .drawWithContent {
                drawContent()
                val x = size.width * shine
                drawRect(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent), start = Offset(x - 80f, 0f), end = Offset(x + 80f, size.height)))
            }
            .border(1.dp, Color.White.copy(alpha = 0.35f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.55f },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GiftIcon(Modifier.size(28.dp))
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.mystery_box), style = Styles.plain(16, Color.White, FontWeight.Black), modifier = Modifier.weight(1f))
        CoinIcon(Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(price.toString(), style = Styles.plain(16, Color.White, FontWeight.Black))
    }
}

@Composable
fun GiftIcon(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        drawRoundRect(Color(0xFFFDE68A), Offset(w * 0.1f, h * 0.4f), Size(w * 0.8f, h * 0.55f), CornerRadius(w * 0.08f))
        drawRoundRect(Color(0xFFFBBF24), Offset(w * 0.04f, h * 0.28f), Size(w * 0.92f, h * 0.18f), CornerRadius(w * 0.06f))
        drawRect(Color(0xFFDC2626), Offset(w * 0.44f, h * 0.28f), Size(w * 0.12f, h * 0.67f))
        drawCircle(Color(0xFFDC2626), w * 0.13f, Offset(w * 0.36f, h * 0.2f), style = androidx.compose.ui.graphics.drawscope.Stroke(w * 0.07f))
        drawCircle(Color(0xFFDC2626), w * 0.13f, Offset(w * 0.64f, h * 0.2f), style = androidx.compose.ui.graphics.drawscope.Stroke(w * 0.07f))
    }
}

/** "You got…": the new item spins in over light rays. */
@Composable
private fun RevealDialog(isTower: Boolean, id: String, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { anim.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 120f)) }
    val t = rememberInfiniteTransition(label = "rays")
    val rot by t.animateFloat(0f, 360f, infiniteRepeatable(tween(12000, easing = androidx.compose.animation.core.LinearEasing)), label = "rot")
    val price = if (isTower) Looks.tower(id).price else Looks.ball(id).price
    val rarity = rarityOf(price)
    val rc = rarityColor(rarity)
    DimOverlay {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text(stringResource(R.string.new_item), style = Styles.gradient(34))
            Spacer(Modifier.height(10.dp))
            Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize().graphicsLayer { rotationZ = rot }) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val r = size.minDimension / 2f
                    for (k in 0 until 12) {
                        val a0 = Math.toRadians(k * 30.0).toFloat()
                        val a1 = a0 + 0.22f
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(c.x, c.y)
                            lineTo(c.x + r * kotlin.math.cos(a0), c.y + r * kotlin.math.sin(a0))
                            lineTo(c.x + r * kotlin.math.cos(a1), c.y + r * kotlin.math.sin(a1))
                            close()
                        }
                        drawPath(path, rc.copy(alpha = 0.22f))
                    }
                    drawCircle(Brush.radialGradient(listOf(rc.copy(alpha = 0.55f), Color.Transparent), c, r), r, c)
                }
                Box(
                    Modifier
                        .size(150.dp)
                        .graphicsLayer {
                            val v = anim.value
                            scaleX = 0.2f + 0.8f * v; scaleY = 0.2f + 0.8f * v
                            rotationY = (1f - v) * 540f
                            alpha = v.coerceIn(0f, 1f)
                        }
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0x55000000))
                        .border(3.dp, rc, RoundedCornerShape(24.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isTower) TowerThumb(Looks.tower(id)) else BallThumb(Looks.ball(id))
                }
            }
            Text(
                stringResource(if (isTower) Looks.tower(id).nameRes else Looks.ball(id).nameRes),
                style = Styles.glow(28, Color.White, FontWeight.Black),
            )
            Text(stringResource(rarityName(rarity)), style = Styles.glow(17, rc, FontWeight.ExtraBold))
            Spacer(Modifier.height(16.dp))
            Box(Modifier.widthIn(max = 320.dp)) { PrimaryButton(stringResource(R.string.got_it), onClick = onClose) }
        }
    }
}

fun rarityColor(r: Rarity): Color = when (r) {
    Rarity.COMMON -> Color(0xFF94A3B8)
    Rarity.RARE -> Color(0xFF38BDF8)
    Rarity.EPIC -> Color(0xFFC084FC)
    Rarity.LEGENDARY -> Color(0xFFFBBF24)
}

fun rarityName(r: Rarity): Int = when (r) {
    Rarity.COMMON -> R.string.rarity_common
    Rarity.RARE -> R.string.rarity_rare
    Rarity.EPIC -> R.string.rarity_epic
    Rarity.LEGENDARY -> R.string.rarity_legendary
}

@Composable
private fun ActionButton(owned: Boolean, inUse: Boolean, price: Int, onBuy: () -> Unit, onUse: () -> Unit) {
    when {
        inUse -> SecondaryButton(stringResource(R.string.in_use)) {}
        owned -> PrimaryButton(stringResource(R.string.use), onClick = onUse)
        else -> PrimaryButton(
            if (Progress.coins >= price) stringResource(R.string.buy_for, price) else stringResource(R.string.need_coins, price - Progress.coins),
            enabled = Progress.coins >= price,
            onClick = onBuy,
        )
    }
}

@Composable
private fun TabChip(text: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .height(42.dp)
            .clip(shape)
            .background(
                if (active) Brush.horizontalGradient(listOf(Color(0xFF06B6D4), Color(0xFF8B5CF6)))
                else Brush.horizontalGradient(listOf(Palette.Glass, Palette.Glass)),
            )
            .border(1.dp, if (active) Color.White.copy(alpha = 0.3f) else Palette.GlassBorder, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = Styles.plain(16, Color.White, FontWeight.ExtraBold)) }
}

@Composable
private fun ItemCard(
    name: String,
    price: Int,
    rarity: Rarity,
    owned: Boolean,
    inUse: Boolean,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    thumb: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val rc = rarityColor(rarity)
    val legendary = rarity == Rarity.LEGENDARY
    // Only legendary cards pulse, and the pulse is drawn (not recomposed).
    val glow = if (legendary && !selected && !inUse) {
        rememberInfiniteTransition(label = "card").animateFloat(0.35f, 0.95f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "glow")
    } else null
    val borderColor = when {
        selected -> Color.White
        inUse -> Color(0xFF34D399)
        else -> rc.copy(alpha = 0.55f)
    }
    Column(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(rc.copy(alpha = if (selected) 0.32f else 0.16f), Color(0x10FFFFFF))))
            .border(if (selected) 2.5.dp else 1.5.dp, borderColor, shape)
            .drawWithContent {
                drawContent()
                if (glow != null) {
                    val sw = 2.5.dp.toPx()
                    drawRoundRect(
                        rc.copy(alpha = glow.value),
                        topLeft = Offset(sw / 2, sw / 2),
                        size = Size(size.width - sw, size.height - sw),
                        cornerRadius = CornerRadius(18.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(sw),
                    )
                }
            }
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
            thumb()
            if (inUse) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(22.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF34D399)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
            } else if (!owned) {
                Icon(Icons.Default.Lock, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.align(Alignment.TopEnd).size(18.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        FitText(name, Styles.plain(13, Palette.TextPrimary, FontWeight.Bold), minSp = 9f)
        FitText(stringResource(rarityName(rarity)), Styles.plain(10, rc, FontWeight.ExtraBold), minSp = 7f)
        if (!owned) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinIcon(Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text(price.toString(), style = Styles.plain(13, Palette.Gold, FontWeight.Black))
            }
        } else {
            Text(" ", style = Styles.plain(13))
        }
    }
}

/** Little drawing of a tower look: its sky and scenery, the base, the pillar and rings. */
@Composable
fun TowerThumb(t: TowerLook) {
    Canvas(Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))) {
        val w = size.width
        val hgt = size.height
        if (w < 2f || hgt < 2f) return@Canvas
        val e = t.env
        drawRect(Brush.verticalGradient(listOf(col(t.bgTop), col(t.bgBottom))))
        drawCircle(Brush.radialGradient(listOf(col(e.sun, 0.9f), col(e.sun, 0.0f)), Offset(w * 0.8f, hgt * 0.18f), w * 0.3f), w * 0.3f, Offset(w * 0.8f, hgt * 0.18f))
        drawCircle(col(e.sun), w * 0.06f, Offset(w * 0.8f, hgt * 0.18f))
        if (t.stars) for (k in 0 until 10) drawCircle(Color.White.copy(alpha = 0.7f), 1.2f, Offset(w * ((k * 37 % 100) / 100f), hgt * ((k * 53 % 45) / 100f)))
        // Scenery silhouette
        val land = col(e.land1)
        val path = androidx.compose.ui.graphics.Path()
        path.moveTo(0f, hgt)
        val n = 24
        for (i in 0..n) {
            val x = w * i / n
            val u = i.toFloat() / n
            val yy = when (e.scene) {
                Scene.PEAKS, Scene.VOLCANO -> 0.62f - 0.16f * kotlin.math.abs(kotlin.math.sin(u * 9.4f)) - (if (e.scene == Scene.VOLCANO) 0.12f * (1f - kotlin.math.abs(u - 0.3f) * 4f).coerceAtLeast(0f) else 0f)
                Scene.CITY -> 0.72f - 0.18f * (((i * 7919) % 13) / 13f)
                Scene.HILLS -> 0.68f - 0.06f * kotlin.math.sin(u * 7f) - 0.03f * kotlin.math.sin(u * 19f)
                Scene.DUNES -> 0.7f - 0.05f * kotlin.math.sin(u * 5f) - (0.14f * (1f - kotlin.math.abs(u - 0.75f) * 7f)).coerceAtLeast(0f)
                Scene.SEA -> 0.74f - (0.08f * (1f - kotlin.math.abs(u - 0.2f) * 8f)).coerceAtLeast(0f)
                Scene.CASTLE -> 0.7f - 0.04f * kotlin.math.sin(u * 6f) - (if (i % 6 == 2) 0.16f else if (i % 6 == 3) 0.1f else 0f)
                else -> 1.2f
            }
            path.lineTo(x, hgt * yy)
        }
        path.lineTo(w, hgt); path.close()
        if (e.scene == Scene.SPACE) {
            drawCircle(Brush.verticalGradient(listOf(col(e.land1), col(e.land2)), startY = hgt * 0.75f, endY = hgt), w * 0.38f, Offset(w * 0.15f, hgt * 1.02f))
        } else {
            drawPath(path, Brush.verticalGradient(listOf(land, col(e.land2)), startY = hgt * 0.45f, endY = hgt))
            if (e.scene == Scene.PEAKS) drawPath(path, Color.White.copy(alpha = 0.25f))
        }
        // Base
        val cx = w / 2f
        val baseTop = hgt * 0.8f
        val rock = col(e.rock)
        val basePath = androidx.compose.ui.graphics.Path().apply {
            if (e.baseKind == com.farias.prismaroll.gl.BaseKind.CASTLE) {
                moveTo(cx - w * 0.2f, baseTop); lineTo(cx + w * 0.2f, baseTop); lineTo(cx + w * 0.2f, hgt * 0.9f)
                lineTo(cx + w * 0.42f, hgt); lineTo(cx - w * 0.42f, hgt); lineTo(cx - w * 0.2f, hgt * 0.9f); close()
            } else {
                moveTo(cx - w * 0.2f, baseTop); lineTo(cx + w * 0.2f, baseTop); lineTo(cx + w * 0.48f, hgt); lineTo(cx - w * 0.48f, hgt); close()
            }
        }
        drawPath(basePath, Brush.horizontalGradient(listOf(Palette.lighter(rock, 0.15f), Palette.darker(rock, 0.4f)), startX = cx - w * 0.4f, endX = cx + w * 0.4f))
        if (e.baseKind == com.farias.prismaroll.gl.BaseKind.CASTLE) {
            for (k in 0 until 5) drawRect(rock, Offset(cx - w * 0.2f + k * w * 0.09f, baseTop - hgt * 0.035f), Size(w * 0.05f, hgt * 0.035f))
            drawRect(col(e.top), Offset(cx - w * 0.04f, hgt * 0.84f), Size(w * 0.03f, hgt * 0.04f))
            drawRect(col(e.top), Offset(cx + w * 0.06f, hgt * 0.84f), Size(w * 0.03f, hgt * 0.04f))
        } else {
            drawOval(col(e.top), Offset(cx - w * 0.2f, baseTop - hgt * 0.015f), Size(w * 0.4f, hgt * 0.04f))
        }
        // Pillar and rings
        val pw = w * 0.13f
        drawRect(
            Brush.horizontalGradient(listOf(col(t.pillar), Palette.lighter(col(t.pillar), 0.35f), col(t.pillar)), startX = cx - pw / 2, endX = cx + pw / 2),
            Offset(cx - pw / 2f, hgt * 0.06f), Size(pw, baseTop - hgt * 0.06f),
        )
        drawCircle(Brush.radialGradient(listOf(Color.White, col(t.ringB)), Offset(cx - 2f, hgt * 0.05f), w * 0.07f), w * 0.06f, Offset(cx, hgt * 0.06f))
        for (i in 0 until 4) {
            val y = hgt * (0.16f + i * 0.16f)
            val c = if (i % 2 == 0) col(t.ringA) else col(t.ringB)
            ring(cx, y, w * 0.33f, hgt * 0.045f, c, gapLeft = i % 2 == 0, danger = if (i == 2) col(t.danger) else null)
        }
        drawOval(Color(0xFFFBBF24), Offset(cx - w * 0.3f, baseTop - hgt * 0.04f), Size(w * 0.6f, hgt * 0.05f))
    }
}

private fun DrawScope.ring(cx: Float, y: Float, rx: Float, h: Float, c: Color, gapLeft: Boolean, danger: Color?) {
    val dark = Palette.darker(c, 0.35f)
    drawOval(dark, Offset(cx - rx, y - h * 0.4f), Size(rx * 2f, h * 1.6f))
    drawOval(Brush.verticalGradient(listOf(Palette.lighter(c, 0.3f), c), startY = y - h, endY = y + h), Offset(cx - rx, y - h), Size(rx * 2f, h * 1.6f))
    val gx = if (gapLeft) cx - rx * 0.55f else cx + rx * 0.2f
    drawOval(Color(0x99000000), Offset(gx, y - h * 0.2f), Size(rx * 0.35f, h * 1.1f))
    if (danger != null) {
        drawOval(danger, Offset(cx + (if (gapLeft) 0.15f else -0.6f) * rx, y - h * 0.15f), Size(rx * 0.4f, h * 0.9f))
    }
}

/** Little drawing of a ball look. */
@Composable
fun BallThumb(b: BallLook) {
    Canvas(Modifier.fillMaxSize(0.8f)) {
        val r = size.minDimension / 2f
        if (r < 2f) return@Canvas
        val c = Offset(size.width / 2f, size.height / 2f)
        val c1 = col(b.c1)
        val c2 = col(b.c2)
        drawCircle(Brush.radialGradient(listOf(col(b.trail, 0.45f), Color.Transparent), c, r * 1.25f), r * 1.25f, c)
        val brush = when (b.style) {
            BallStyle.RAINBOW -> Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red), c)
            BallStyle.BEACH -> Brush.sweepGradient(listOf(c1, c1, c2, c2, Color(0xFFFACC15), Color(0xFFFACC15), Color(0xFF3B82F6), Color(0xFF3B82F6), c1), c)
            BallStyle.STRIPES -> Brush.linearGradient(listOf(c1, c2, c1, c2, c1, c2, c1), start = Offset(c.x - r, c.y - r), end = Offset(c.x + r, c.y + r))
            BallStyle.METAL, BallStyle.DISCO -> Brush.linearGradient(listOf(Palette.lighter(c1, 0.5f), c1, c2, c1), start = Offset(c.x - r, c.y - r), end = Offset(c.x + r, c.y + r))
            BallStyle.PLANET -> Brush.verticalGradient(listOf(c1, c2, c1, Palette.lighter(c1, 0.3f), c2), startY = c.y - r, endY = c.y + r)
            BallStyle.LAVA -> Brush.radialGradient(listOf(c1, Color(0xFFFF8A00), c2), Offset(c.x - r * 0.2f, c.y - r * 0.2f), r * 1.2f)
            BallStyle.NEON -> Brush.radialGradient(listOf(Color.White, c1, c2), c, r)
            BallStyle.GALAXY -> Brush.radialGradient(listOf(c2, Color(0xFF3B1F7A), c1), Offset(c.x + r * 0.2f, c.y - r * 0.1f), r * 1.1f)
            BallStyle.GEM -> Brush.sweepGradient(listOf(c1, Color(0xFFF0ABFC), Color(0xFF60A5FA), c1, Color(0xFFE9D5FF), c1), c)
            else -> Brush.radialGradient(listOf(Palette.lighter(c1, 0.4f), c1, Palette.darker(c1, 0.3f)), Offset(c.x - r * 0.3f, c.y - r * 0.3f), r * 1.4f)
        }
        drawCircle(brush, r, c)
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(r * 0.09f)
        when (b.style) {
            BallStyle.EIGHT -> {
                drawCircle(Color.White, r * 0.42f, Offset(c.x, c.y - r * 0.1f))
                drawCircle(Color.Black, r * 0.16f, Offset(c.x, c.y - r * 0.2f))
                drawCircle(Color.Black, r * 0.19f, Offset(c.x, c.y + r * 0.06f))
            }
            BallStyle.DISCO -> for (i in 0 until 5) for (j in 0 until 5) {
                if ((i + j) % 2 == 0) drawRect(Color.White.copy(alpha = 0.35f), Offset(c.x - r * 0.7f + i * r * 0.3f, c.y - r * 0.7f + j * r * 0.3f), Size(r * 0.16f, r * 0.16f))
            }
            BallStyle.SOCCER -> {
                drawCircle(c2, r * 0.26f, c)
                for (k in 0 until 5) {
                    val a = (k * 72 - 90) * Math.PI.toFloat() / 180f
                    drawCircle(c2, r * 0.2f, Offset(c.x + kotlin.math.cos(a) * r * 0.82f, c.y + kotlin.math.sin(a) * r * 0.82f))
                    drawLine(c2.copy(alpha = 0.6f), c, Offset(c.x + kotlin.math.cos(a) * r * 0.8f, c.y + kotlin.math.sin(a) * r * 0.8f), r * 0.05f)
                }
            }
            BallStyle.BASKET -> {
                drawLine(c2, Offset(c.x, c.y - r), Offset(c.x, c.y + r), r * 0.07f)
                drawLine(c2, Offset(c.x - r, c.y), Offset(c.x + r, c.y), r * 0.07f)
                drawArc(c2, -60f, 120f, false, Offset(c.x - r * 1.6f, c.y - r), Size(r * 1.4f, r * 2f), style = stroke)
                drawArc(c2, 120f, 120f, false, Offset(c.x + r * 0.2f, c.y - r), Size(r * 1.4f, r * 2f), style = stroke)
            }
            BallStyle.TENNIS -> {
                drawArc(c2, -70f, 140f, false, Offset(c.x - r * 1.5f, c.y - r * 0.9f), Size(r * 1.3f, r * 1.8f), style = androidx.compose.ui.graphics.drawscope.Stroke(r * 0.1f))
                drawArc(c2, 110f, 140f, false, Offset(c.x + r * 0.2f, c.y - r * 0.9f), Size(r * 1.3f, r * 1.8f), style = androidx.compose.ui.graphics.drawscope.Stroke(r * 0.1f))
            }
            BallStyle.MELON -> for (k in -3..3) {
                drawArc(c1, -90f, 180f, false, Offset(c.x - r * 0.15f * kotlin.math.abs(k) - r * 0.1f, c.y - r), Size(r * (0.3f * kotlin.math.abs(k) + 0.2f), r * 2f), style = androidx.compose.ui.graphics.drawscope.Stroke(r * 0.12f))
            }
            BallStyle.MOON -> {
                drawCircle(c2.copy(alpha = 0.6f), r * 0.2f, Offset(c.x - r * 0.3f, c.y - r * 0.2f))
                drawCircle(c2.copy(alpha = 0.5f), r * 0.13f, Offset(c.x + r * 0.35f, c.y + r * 0.25f))
                drawCircle(c2.copy(alpha = 0.45f), r * 0.09f, Offset(c.x + r * 0.1f, c.y - r * 0.5f))
                drawCircle(c2.copy(alpha = 0.4f), r * 0.11f, Offset(c.x - r * 0.25f, c.y + r * 0.45f))
            }
            BallStyle.EARTH -> {
                drawOval(c2, Offset(c.x - r * 0.7f, c.y - r * 0.5f), Size(r * 0.7f, r * 0.9f))
                drawOval(c2, Offset(c.x + r * 0.1f, c.y - r * 0.1f), Size(r * 0.55f, r * 0.75f))
                drawOval(Color.White.copy(alpha = 0.7f), Offset(c.x - r * 0.5f, c.y - r * 0.95f), Size(r, r * 0.25f))
                drawOval(Color.White.copy(alpha = 0.5f), Offset(c.x - r * 0.2f, c.y + r * 0.3f), Size(r * 0.7f, r * 0.14f))
            }
            BallStyle.SMILEY -> {
                drawOval(c2, Offset(c.x - r * 0.38f, c.y - r * 0.4f), Size(r * 0.16f, r * 0.28f))
                drawOval(c2, Offset(c.x + r * 0.22f, c.y - r * 0.4f), Size(r * 0.16f, r * 0.28f))
                drawArc(c2, 20f, 140f, false, Offset(c.x - r * 0.55f, c.y - r * 0.5f), Size(r * 1.1f, r * 1.1f), style = androidx.compose.ui.graphics.drawscope.Stroke(r * 0.1f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                drawCircle(Color(0x66FF6B6B), r * 0.13f, Offset(c.x - r * 0.55f, c.y + r * 0.1f))
                drawCircle(Color(0x66FF6B6B), r * 0.13f, Offset(c.x + r * 0.55f, c.y + r * 0.1f))
            }
            BallStyle.EYE -> {
                drawCircle(Brush.radialGradient(listOf(Palette.lighter(c2, 0.4f), c2, Palette.darker(c2, 0.4f)), c, r * 0.45f), r * 0.45f, c)
                drawCircle(Color.Black, r * 0.2f, c)
                drawCircle(Color.White, r * 0.07f, Offset(c.x + r * 0.1f, c.y - r * 0.1f))
            }
            BallStyle.GALAXY -> for (k in 0 until 14) {
                drawCircle(Color.White.copy(alpha = 0.8f), r * (0.02f + (k % 3) * 0.015f), Offset(c.x + r * 0.8f * kotlin.math.cos(k * 2.4f) * ((k * 37 % 10) / 10f), c.y + r * 0.8f * kotlin.math.sin(k * 2.4f) * ((k * 53 % 10) / 10f)))
            }
            BallStyle.GEM -> for (k in 0 until 6) {
                val a = (k * 60) * Math.PI.toFloat() / 180f
                drawLine(Color.White.copy(alpha = 0.45f), c, Offset(c.x + kotlin.math.cos(a) * r, c.y + kotlin.math.sin(a) * r), r * 0.04f)
            }
            else -> Unit
        }
        drawCircle(Brush.radialGradient(listOf(Color.Transparent, Color(0x55000000)), Offset(c.x - r * 0.3f, c.y - r * 0.3f), r * 1.6f), r, c)
        drawCircle(Color.White.copy(alpha = 0.75f), r * 0.16f, Offset(c.x - r * 0.38f, c.y - r * 0.4f))
    }
}
