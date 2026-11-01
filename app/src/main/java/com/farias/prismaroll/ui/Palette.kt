package com.farias.prismaroll.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.farias.prismaroll.gl.Looks
import com.farias.prismaroll.gl.TowerLook
import com.farias.prismaroll.data.Progress

/** UI colours of a tower look (Compose colours). */
class GameTheme(val bgTop: Color, val bgBottom: Color, val aurora: List<Color>, val stars: Boolean, val accent: Color, val accent2: Color)

private fun c(f: FloatArray) = Color(f[0], f[1], f[2])

fun TowerLook.ui(): GameTheme = GameTheme(c(bgTop), c(bgBottom), aurora.map { c(it) }, stars, c(ringA), c(ringB))

object Palette {
    /** Current theme. Reading it inside composition or drawing subscribes to theme changes. */
    val theme: GameTheme get() = Looks.tower(Progress.towerId).ui()

    val BgTop: Color get() = theme.bgTop
    val BgBottom: Color get() = theme.bgBottom

    val Glass = Color(0x14FFFFFF)
    val GlassBorder = Color(0x26FFFFFF)
    val EmptyCell = Color(0x0FFFFFFF)
    val EmptyCellInner = Color(0x33000000)

    val TextPrimary = Color(0xFFF5F3FF)
    val TextSecondary = Color(0xFFA5A3C9)
    val Gold = Color(0xFFFACC15)
    val Cyan = Color(0xFF22D3EE)
    val Magenta = Color(0xFFE879F9)
    val Danger = Color(0xFFF43F5E)

    fun lighter(c: Color, f: Float = 0.35f) = lerp(c, Color.White, f)
    fun darker(c: Color, f: Float = 0.35f) = lerp(c, Color.Black, f)

    /** Desaturated gray version, used when the game is over. */
    fun gray(c: Color): Color {
        val l = c.red * 0.3f + c.green * 0.59f + c.blue * 0.11f
        return Color(l * 0.55f, l * 0.55f, l * 0.62f, c.alpha)
    }
}
