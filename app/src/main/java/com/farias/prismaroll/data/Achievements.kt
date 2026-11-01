package com.farias.prismaroll.data

import androidx.annotation.StringRes
import com.farias.prismaroll.R
import com.farias.prismaroll.gl.Looks

/** A long-term goal with a coin reward. [value] reads the current progress. */
class Achievement(val id: String, @StringRes val textRes: Int, val target: Int, val reward: Int, val value: () -> Int)

object Achievements {
    val all: List<Achievement> = listOf(
        Achievement("rings100", R.string.ach_rings, 1000, 50) { Progress.ringsPassed },
        Achievement("rings1000", R.string.ach_rings, 10000, 150) { Progress.ringsPassed },
        Achievement("rings5000", R.string.ach_rings, 50000, 400) { Progress.ringsPassed },
        Achievement("smash50", R.string.ach_smash, 30, 80) { Progress.ringsSmashed },
        Achievement("smash500", R.string.ach_smash, 300, 300) { Progress.ringsSmashed },
        Achievement("boss1", R.string.ach_boss, 1, 100) { Progress.bossesBeaten },
        Achievement("boss5", R.string.ach_boss, 5, 250) { Progress.bossesBeaten },
        Achievement("boss20", R.string.ach_boss, 20, 600) { Progress.bossesBeaten },
        Achievement("perfect1", R.string.ach_perfect, 1, 80) { Progress.perfectLevels },
        Achievement("perfect10", R.string.ach_perfect, 10, 300) { Progress.perfectLevels },
        Achievement("coins100", R.string.ach_coins, 100, 100) { Progress.coinsCollected },
        Achievement("glass50", R.string.ach_glass, 50, 120) { Progress.glassBroken },
        Achievement("portal20", R.string.ach_portal, 20, 150) { Progress.portalsUsed },
        Achievement("endless100", R.string.ach_endless, 800, 200) { Progress.bestEndless },
        Achievement("endless300", R.string.ach_endless, 2000, 500) { Progress.bestEndless },
        Achievement("streak10", R.string.ach_streak, 10, 200) { Progress.bestWinStreak },
        Achievement("level50", R.string.ach_level, 50, 250) { Progress.level - 1 },
        Achievement("level100", R.string.ach_level, 100, 500) { Progress.level - 1 },
        Achievement("skins10", R.string.ach_skins, 10, 300) { Progress.ownedTowers.size + Progress.ownedBalls.size },
        Achievement("daily7", R.string.ach_daily, 7, 300) { Progress.dailyStreak() },
    )

    fun progress(a: Achievement): Int = a.value().coerceAtMost(a.target)
    fun done(a: Achievement): Boolean = a.value() >= a.target
    fun claimable(): Int = all.count { done(it) && it.id !in Progress.claimedAchievements }
}

/** Random locked skin for prizes (roulette, mystery). */
fun randomLockedBall(): String? = Looks.balls.filter { !Progress.ownsBall(it.id) }.randomOrNull()?.id
