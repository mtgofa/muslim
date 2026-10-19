package com.mtgofa.muslem.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.mtgofa.muslem.MainActivity
import com.mtgofa.muslem.prayer.AdhanService
import com.mtgofa.muslem.prayer.Prayer
import com.mtgofa.muslem.prayer.Scheduler
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * The night-prayer alarm: at a set hour, or a set time before Fajr (which then moves with Fajr
 * through the year). Armed as an alarm clock so it rings on time in deep sleep; its settings live
 * with the prayer settings in device storage, so it also rings after a reboot before unlocking.
 */
object Qiyam {
    /** the next time the alarm rings, or null when it is off */
    fun next(now: ZonedDateTime = ZonedDateTime.now(Settings.zone)): ZonedDateTime? {
        if (!Settings.qiyamOn) return null
        val today = now.toLocalDate()
        return (-1..2).map { d ->
            val day = today.plusDays(d.toLong())
            if (Settings.qiyamBeforeFajr) Scheduler.times(day)[Prayer.FAJR].minusMinutes(Settings.qiyamBefore.toLong())
            else ZonedDateTime.of(day, LocalTime.of(Settings.qiyamAt / 60, Settings.qiyamAt % 60), Settings.zone)
        }.filter { it.isAfter(now) }.minOrNull()
    }

    fun schedule(ctx: Context) {
        Settings.init(ctx)
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = fireIntent(ctx)
        am.cancel(pi)
        val at = next() ?: return
        val show = PendingIntent.getActivity(
            ctx, 72, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val millis = at.toInstant().toEpochMilli()
        runCatching { am.setAlarmClock(AlarmManager.AlarmClockInfo(millis, show), pi) }
            .onFailure { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi) }
    }

    private fun fireIntent(ctx: Context) = PendingIntent.getBroadcast(
        ctx, 71, Intent(ctx, QiyamReceiver::class.java).putExtra("at", 0L),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** "قبل الفجر بساعة وربع" style summary with tonight's time, for the settings rows */
    fun summary(): String {
        if (!Settings.qiyamOn) return "متوقف"
        val at = next() ?: return "متوقف"
        val clock = AdhanService.formatTime(at.toInstant().toEpochMilli())
        return if (Settings.qiyamBeforeFajr) "قبل الفجر بـ ${beforeText(Settings.qiyamBefore)} · $clock" else "كل ليلة الساعة $clock"
    }

    fun beforeText(min: Int): String {
        val h = min / 60; val m = min % 60
        val hours = when (h) { 0 -> ""; 1 -> "ساعة"; 2 -> "ساعتين"; else -> "${com.mtgofa.muslem.prayer.arabicDigits(h)} ساعات" }
        val mins = if (m == 0) "" else "${com.mtgofa.muslem.prayer.arabicDigits(m)} دقيقة"
        return listOf(hours, mins).filter { it.isNotEmpty() }.joinToString(" و").ifEmpty { "٠ دقيقة" }
    }
}

class QiyamReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Settings.init(ctx)
        if (Settings.qiyamOn) ContextCompat.startForegroundService(ctx, Intent(ctx, AdhanService::class.java).setAction(AdhanService.ACTION_QIYAM))
        Qiyam.schedule(ctx)
    }
}
