package com.farias.prismaroll.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farias.prismaroll.R
import com.farias.prismaroll.data.Progress
import com.farias.prismaroll.game.RollLevels
import com.farias.prismaroll.gl.Looks

/** Worlds of 10 levels; the 10th is a boss tower. Any reached level can be replayed. */
@Composable
fun MapScreen(onBack: () -> Unit, onPlay: (Int) -> Unit) {
    BackHandler(onBack = onBack)
    val current = Progress.level
    val worlds = (current - 1) / 10 + 2 // the reached worlds plus the next one (locked)
    val state = rememberLazyListState()
    LaunchedEffect(Unit) { state.scrollToItem(((current - 1) / 10).coerceAtLeast(0)) }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF130C30), Color(0xFF07051A))))) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .safeTop()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.map_title), style = Styles.gradient(30), modifier = Modifier.weight(1f))
                GlassCircleButton(onClick = onBack) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = Palette.TextPrimary)
                }
            }
            LazyColumn(
                state = state,
                modifier = Modifier.weight(1f).fillMaxWidth().navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            ) {
                items((0 until worlds).toList()) { w -> WorldCard(w, current, onPlay) }
            }
        }
    }
}

@Composable
private fun WorldCard(w: Int, current: Int, onPlay: (Int) -> Unit) {
    val look = Looks.towers[w % Looks.towers.size]
    val theme = look.ui()
    val first = w * 10 + 1
    val unlocked = first <= current
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(theme.bgTop, theme.bgBottom)))
            .border(1.5.dp, if (unlocked) theme.accent.copy(alpha = 0.7f) else Palette.GlassBorder, shape)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.world_x, w + 1), style = Styles.glow(22, Color.White, FontWeight.Black))
                Text(stringResource(look.nameRes), style = Styles.plain(14, theme.accent, FontWeight.Bold))
            }
            val done = (current - first).coerceIn(0, 10)
            Text("$done/10", style = Styles.plain(16, Color.White, FontWeight.Black))
        }
        Spacer(Modifier.height(12.dp))
        // Two rows of five nodes; the last is the boss.
        for (row in 0 until 2) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (c in 0 until 5) {
                    val n = first + row * 5 + c
                    LevelNode(n, current, theme.accent, theme.accent2, onPlay)
                }
            }
            if (row == 0) Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun LevelNode(n: Int, current: Int, a: Color, b: Color, onPlay: (Int) -> Unit) {
    val boss = RollLevels.isBoss(n)
    val reached = n <= current
    val isCurrent = n == current
    val nodeSize = if (boss) 54.dp else 46.dp
    Box(Modifier.size(54.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(nodeSize)
                .clip(CircleShape)
                .background(
                    when {
                        isCurrent -> Brush.verticalGradient(listOf(Color.White, a))
                        reached -> Brush.verticalGradient(listOf(Palette.lighter(a, 0.2f), Palette.darker(b, 0.2f)))
                        else -> Brush.verticalGradient(listOf(Color(0x33000000), Color(0x55000000)))
                    },
                )
                .border(if (isCurrent) 3.dp else 1.5.dp, if (boss) Palette.Gold else Color.White.copy(alpha = if (reached) 0.6f else 0.2f), CircleShape)
                .then(if (reached) Modifier.clickable { onPlay(n) } else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            when {
                !reached -> Icon(Icons.Default.Lock, null, tint = Color.White.copy(alpha = 0.45f), modifier = Modifier.size(16.dp))
                else -> Text(n.toString(), style = Styles.plain(if (n > 99) 13 else 16, if (isCurrent) Color(0xFF1E1240) else Color.White, FontWeight.Black))
            }
        }
        if (boss) CrownIcon(Modifier.align(Alignment.TopCenter).size(24.dp, 16.dp))
        if (reached && n < current) {
            Canvas(Modifier.align(Alignment.BottomEnd).size(16.dp)) {
                drawCircle(Color(0xFF34D399))
                drawLine(Color.White, Offset(size.width * 0.25f, size.height * 0.52f), Offset(size.width * 0.43f, size.height * 0.7f), size.width * 0.14f)
                drawLine(Color.White, Offset(size.width * 0.43f, size.height * 0.7f), Offset(size.width * 0.76f, size.height * 0.32f), size.width * 0.14f)
            }
        }
    }
}
