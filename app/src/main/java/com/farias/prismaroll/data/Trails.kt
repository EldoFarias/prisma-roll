package com.farias.prismaroll.data

import androidx.annotation.StringRes
import com.farias.prismaroll.R

/** Trails behind the ball (third shop tab). Styles are drawn by the renderer. */
class TrailLook(val id: String, @StringRes val nameRes: Int, val price: Int, val style: Int)

object Trails {
    const val CLASSIC = 0
    const val RAINBOW = 1
    const val STARS = 2
    const val BLUE_FIRE = 3
    const val GOLD = 4
    const val GHOST = 5
    const val NEON = 6

    val all = listOf(
        TrailLook("classic", R.string.trail_classic, 0, CLASSIC),
        TrailLook("neon", R.string.trail_neon, 600, NEON),
        TrailLook("ghost", R.string.trail_ghost, 900, GHOST),
        TrailLook("stars", R.string.trail_stars, 1500, STARS),
        TrailLook("bluefire", R.string.trail_bluefire, 2200, BLUE_FIRE),
        TrailLook("gold", R.string.trail_gold, 3000, GOLD),
        TrailLook("rainbow", R.string.trail_rainbow, 4000, RAINBOW),
    )

    fun byId(id: String) = all.firstOrNull { it.id == id } ?: all[0]
}

