package com.farias.prismaroll.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
enum class Haptic { TICK, PLACE, CLEAR, COMBO, HEAVY }

/**
 * Real vibrations through the device vibrator. Unlike View.performHapticFeedback, this does not
 * depend on the system "touch feedback" setting (off by default on many phones).
 */
object Haptics {
    private var vibrator: Vibrator? = null

    private fun vibrator(context: Context): Vibrator? {
        vibrator?.let { return it }
        val v = if (Build.VERSION.SDK_INT >= 31) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator = v
        return v
    }

    fun play(context: Context, kind: Haptic) {
        val v = vibrator(context.applicationContext) ?: return
        if (!v.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                val amp = v.hasAmplitudeControl()
                val effect = when (kind) {
                    // Without amplitude control a "light" tick is a full-strength buzz, so skip it.
                    Haptic.TICK -> if (amp) VibrationEffect.createOneShot(8, 40) else return
                    Haptic.PLACE -> VibrationEffect.createOneShot(18, if (amp) 110 else VibrationEffect.DEFAULT_AMPLITUDE)
                    Haptic.CLEAR -> VibrationEffect.createOneShot(40, if (amp) 190 else VibrationEffect.DEFAULT_AMPLITUDE)
                    Haptic.COMBO -> waveform(longArrayOf(0, 35, 45, 35, 45, 60), intArrayOf(0, 170, 0, 210, 0, 255), amp)
                    Haptic.HEAVY -> waveform(longArrayOf(0, 70, 40, 90), intArrayOf(0, 255, 0, 200), amp)
                }
                vibrateAsGame(v, effect)
            } else {
                @Suppress("DEPRECATION")
                when (kind) {
                    Haptic.TICK -> Unit
                    Haptic.PLACE -> v.vibrate(18)
                    Haptic.CLEAR -> v.vibrate(40)
                    Haptic.COMBO -> v.vibrate(longArrayOf(0, 35, 45, 35, 45, 60), -1)
                    Haptic.HEAVY -> v.vibrate(longArrayOf(0, 70, 40, 90), -1)
                }
            }
        } catch (e: Exception) {
            // Some devices throw when the vibrator is busy or restricted; vibration is optional.
        }
    }

    /**
     * Some ROMs (MIUI/HyperOS and others) silence vibrations without attributes as "touch" feedback.
     * Tagging them as media/game makes them follow the normal vibration setting instead.
     */
    private fun vibrateAsGame(v: Vibrator, effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= 33) {
            v.vibrate(effect, android.os.VibrationAttributes.createForUsage(android.os.VibrationAttributes.USAGE_MEDIA))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(
                effect,
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_GAME)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
    }

    private fun waveform(timings: LongArray, amplitudes: IntArray, amp: Boolean): VibrationEffect =
        if (amp) VibrationEffect.createWaveform(timings, amplitudes, -1)
        else VibrationEffect.createWaveform(timings, -1)
}
