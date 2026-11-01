package com.farias.prismaroll.data

import androidx.annotation.StringRes
import com.farias.prismaroll.R
import kotlin.random.Random

enum class MissionType(@StringRes val textRes: Int) {
    LEVELS(R.string.mission_levels),
    RINGS(R.string.mission_rings),
    SMASH(R.string.mission_smash),
    SCORE(R.string.mission_score),
    STREAK(R.string.mission_streak),
}

data class Mission(val type: MissionType, val target: Int, val reward: Int)

/** Three daily missions, the same for every player on the same day. */
object Missions {
    const val COUNT = 3

    fun forDay(day: Int): List<Mission> {
        val rnd = Random(day * 31L + 11)
        return MissionType.entries.shuffled(rnd).take(COUNT).map { type ->
            val tier = rnd.nextInt(3)
            val target = when (type) {
                MissionType.LEVELS -> listOf(3, 5, 8)[tier]
                MissionType.RINGS -> listOf(1200, 2500, 5000)[tier]
                MissionType.SMASH -> listOf(5, 12, 25)[tier]
                MissionType.SCORE -> listOf(150, 300, 500)[tier]
                MissionType.STREAK -> listOf(2, 3, 5)[tier]
            }
            Mission(type, target, listOf(30, 45, 70)[tier])
        }
    }
}
