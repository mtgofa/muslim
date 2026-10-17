package com.mtgofa.muslem.data

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Deliberate vibrations (Qibla found, tasbeeh round complete). They are tagged as alerts: a plain
 * vibrate() counts as touch feedback, which many phones silence when "vibrate on touch" is off.
 */
object Haptics {
    private fun vibrator(ctx: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= 31) ctx.getSystemService(VibratorManager::class.java)?.defaultVibrator
        else @Suppress("DEPRECATION") ctx.getSystemService(Vibrator::class.java)

    fun buzz(ctx: Context, ms: Long = 60) = play(ctx, VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))

    /** A barely-there tick for each tasbeeh tap: short and soft, but never muted like touch feedback. */
    fun tick(ctx: Context) {
        val v = vibrator(ctx)?.takeIf { it.hasVibrator() } ?: return
        play(ctx, VibrationEffect.createOneShot(22, if (v.hasAmplitudeControl()) 90 else VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun pattern(ctx: Context, vararg timings: Long) = play(ctx, VibrationEffect.createWaveform(timings, -1))

    private fun play(ctx: Context, effect: VibrationEffect) {
        val v = vibrator(ctx)?.takeIf { it.hasVibrator() } ?: return
        if (Build.VERSION.SDK_INT >= 33) {
            v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
        }
    }
}
