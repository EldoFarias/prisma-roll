package com.farias.prismaroll.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.farias.prismaroll.R
import com.farias.prismaroll.ads.AdsManager
import com.farias.prismaroll.audio.Haptic
import com.farias.prismaroll.audio.Haptics
import com.farias.prismaroll.audio.MusicPlayer
import com.farias.prismaroll.data.Progress
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

const val PRIVACY_POLICY_URL = "https://eldofarias.github.io/prisma-tower/privacy.html"

@Composable
fun SettingsToggles(onNotifications: ((Boolean) -> Unit)? = null) {
    val ctx = LocalContext.current
    ToggleRow(stringResource(R.string.music), Progress.musicOn) {
        Progress.toggleMusic()
        MusicPlayer.update()
    }
    ToggleRow(stringResource(R.string.sound), Progress.soundOn) { Progress.toggleSound() }
    ToggleRow(stringResource(R.string.vibration), Progress.vibrationOn) {
        Progress.toggleVibration()
        if (Progress.vibrationOn) Haptics.play(ctx, Haptic.CLEAR)
    }
    ToggleRow(stringResource(R.string.detailed_scenery), Progress.detailedScenery) { Progress.changeDetailedScenery(!Progress.detailedScenery) }
    if (onNotifications != null) {
        ToggleRow(stringResource(R.string.notifications), Progress.notificationsOn) { onNotifications(!Progress.notificationsOn) }
    }
}

@Composable
fun SettingsDialog(ads: AdsManager, onNotifications: (Boolean) -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val ctx = LocalContext.current
    DimOverlay {
        GlassPanel {
            Text(stringResource(R.string.settings), style = Styles.gradient(30))
            Spacer(Modifier.height(4.dp))
            SettingsToggles(onNotifications)
            Spacer(Modifier.height(6.dp))
            PrimaryButton(stringResource(R.string.got_it), onClick = onClose)
            if (ads.privacyOptionsRequired) LinkText(stringResource(R.string.privacy_options)) { ads.showPrivacyOptions() }
            LinkText(stringResource(R.string.privacy_policy)) { openUrl(ctx, PRIVACY_POLICY_URL) }
        }
    }
}

fun openUrl(ctx: Context, url: String) {
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
    }
}

@Composable
fun LinkText(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = Styles.plain(14, Palette.TextSecondary),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
    )
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = Styles.plain(17), modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF8B5CF6),
                uncheckedThumbColor = Palette.TextSecondary,
                uncheckedTrackColor = Color(0x33FFFFFF),
                uncheckedBorderColor = Color(0x55FFFFFF),
            ),
        )
    }
}

@Composable
fun CoinsEarned(amount: Int, big: Boolean = false) {
    if (amount <= 0) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        CoinIcon(Modifier.size(if (big) 30.dp else 22.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.coins_earned, amount), style = Styles.glow(if (big) 26 else 19, Palette.Gold, FontWeight.Black))
    }
}

fun bannerAdSize(context: Context, widthPx: Int): AdSize =
    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, (widthPx / context.resources.displayMetrics.density).toInt())

@Composable
fun bannerHeight(adSize: AdSize): Dp {
    val context = LocalContext.current
    return with(LocalDensity.current) { adSize.getHeightInPixels(context).toDp() }
}

/** Adaptive banner at the bottom. Space is reserved up front so the layout doesn't jump. */
@Composable
fun BannerAd(ads: AdsManager) {
    val context = LocalContext.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val widthPx = constraints.maxWidth
        val adSize = remember(widthPx) { bannerAdSize(context, widthPx) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(bannerHeight(adSize)),
            contentAlignment = Alignment.Center,
        ) {
            if (ads.adsReady) {
                key(adSize) {
                    AndroidView(
                        factory = { ctx ->
                            AdView(ctx).apply {
                                setAdSize(adSize)
                                adUnitId = ads.bannerId
                                loadAd(AdRequest.Builder().build())
                            }
                        },
                        onRelease = { it.destroy() },
                    )
                }
            }
        }
    }
}
