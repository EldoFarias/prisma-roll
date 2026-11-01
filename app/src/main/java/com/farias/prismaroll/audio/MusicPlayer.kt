package com.farias.prismaroll.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.farias.prismaroll.R
import com.farias.prismaroll.data.Progress

/** Background music loop. Follows the music setting and the activity lifecycle. */
object MusicPlayer {
    private const val VOLUME = 0.35f
    private var player: MediaPlayer? = null
    private var appContext: Context? = null
    private var foreground = false
    private var track = R.raw.music_chill
    private var playing = 0

    /** Music that suits each tower look (boss towers always get the epic one). */
    fun trackFor(towerId: String, boss: Boolean = false): Int = when {
        boss -> R.raw.music_epic
        towerId in setOf("ocean", "candy", "jungle", "desert") -> R.raw.music_sunny
        towerId in setOf("lava", "gold", "castle") -> R.raw.music_epic
        towerId in setOf("galaxy", "ice", "cyber", "aurora") -> R.raw.music_space
        else -> R.raw.music_chill
    }

    /** Switches to [res] (keeps playing if it's already the current one). */
    fun select(res: Int) {
        if (res == track) return
        track = res
        if (player != null) {
            player?.release()
            player = null
        }
        update()
    }

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun onResume() {
        foreground = true
        update()
    }

    fun onPause() {
        foreground = false
        player?.takeIf { it.isPlaying }?.pause()
    }

    /** Starts or stops the music to match the setting. */
    fun update() {
        if (!foreground) return
        if (!Progress.musicOn) {
            player?.takeIf { it.isPlaying }?.pause()
            return
        }
        val p = player?.takeIf { playing == track } ?: run {
            player?.release()
            player = null
            create()
        } ?: return
        if (!p.isPlaying) p.start()
    }

    private fun create(): MediaPlayer? {
        val ctx = appContext ?: return null
        return try {
            MediaPlayer.create(
                ctx,
                track,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
                0,
            )?.apply {
                isLooping = true
                setVolume(VOLUME, VOLUME)
            }.also { player = it; playing = track }
        } catch (e: Exception) {
            null
        }
    }
}
