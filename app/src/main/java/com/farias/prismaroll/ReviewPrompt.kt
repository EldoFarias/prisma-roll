package com.farias.prismaroll

import android.app.Activity
import com.farias.prismaroll.data.Progress
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Asks for a Play Store rating inside the game, right after a great moment.
 * Google decides whether the dialog actually shows; we also limit it to once a month
 * and only after the player has finished a few games.
 */
object ReviewPrompt {
    private const val MIN_GAMES = 3
    private const val INTERVAL_MS = 30L * 24 * 60 * 60 * 1000

    fun maybeAsk(activity: Activity) {
        if (Progress.levelsWon < MIN_GAMES) return
        val now = System.currentTimeMillis()
        if (now - Progress.lastReviewAsk < INTERVAL_MS) return
        Progress.markReviewAsked(now)
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful && !activity.isFinishing) {
                manager.launchReviewFlow(activity, task.result)
            }
        }
    }
}
