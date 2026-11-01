package com.farias.prismaroll.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.farias.prismaroll.R
import com.farias.prismaroll.data.Progress

private class Stat(val label: Int, val value: String, val color: Color)

@Composable
fun StatsScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val stats = listOf(
        Stat(R.string.stat_level, Progress.level.toString(), Palette.Gold),
        Stat(R.string.stat_best, Progress.bestScore.toString(), Palette.Magenta),
        Stat(R.string.stat_won, Progress.levelsWon.toString(), Palette.Cyan),
        Stat(R.string.stat_rings, Progress.ringsPassed.toString(), Color(0xFFA3E635)),
        Stat(R.string.stat_smashed, "🔥 " + Progress.ringsSmashed, Color(0xFFFB923C)),
        Stat(R.string.stat_combo, "×" + Progress.bestCombo, Color(0xFF60A5FA)),
        Stat(R.string.stat_best_streak, Progress.bestWinStreak.toString(), Color(0xFFF472B6)),
        Stat(R.string.stat_coins, Progress.coins.toString(), Palette.Gold),
    )
    Box(Modifier.fillMaxSize()) {
        AuroraBackground()
        Column(
            Modifier
                .fillMaxSize()
                .safeBars(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassCircleButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Palette.TextPrimary)
                }
                Spacer(Modifier.width(14.dp))
                Text(stringResource(R.string.stats), style = Styles.gradient(30))
            }
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = if (maxWidth >= 600.dp) 4 else 2
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    stats.chunked(columns).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            row.forEach { StatCard(it, Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(stat: Stat, modifier: Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .height(104.dp)
            .clip(shape)
            .background(Palette.Glass)
            .border(1.dp, stat.color.copy(alpha = 0.4f), shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            stringResource(stat.label),
            style = Styles.plain(14, Palette.TextSecondary, FontWeight.SemiBold),
            textAlign = TextAlign.Start,
        )
        Text(stat.value, style = Styles.glow(28, stat.color, FontWeight.Black), maxLines = 1)
    }
}
