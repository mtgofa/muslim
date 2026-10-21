package com.mtgofa.muslim.prayer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mtgofa.muslim.MainActivity
import com.mtgofa.muslim.data.AlertMode
import com.mtgofa.muslim.data.Settings
import java.time.LocalDate
import java.time.ZonedDateTime

enum class EventType { PRE, ADHAN, IQAMA }

data class PrayerEvent(val type: EventType, val prayer: Prayer, val at: Long, val prayerTime: Long)

/**
 * Turns settings + location into concrete times, and keeps exactly one exact alarm armed: the
 * next event. Every time it fires, the receiver re-arms the following one, so the chain never
 * breaks (also re-armed on boot, time/zone change, app update and from the keep-alive service).
 */
object Scheduler {

    fun times(date: LocalDate): DayTimes = PrayerCalc.compute(
        date, Settings.lat, Settings.lng, Settings.zone, Settings.method, Settings.madhab, Settings.offsets(),
    )

    fun today(): DayTimes = times(LocalDate.now(Settings.zone))

    /** The next prayer (never Sunrise) at or after now, possibly tomorrow's Fajr. */
    fun nextPrayer(now: ZonedDateTime = ZonedDateTime.now(Settings.zone), includeSunrise: Boolean = true): Pair<Prayer, ZonedDateTime> {
        for (d in 0..1) {
            val t = times(now.toLocalDate().plusDays(d.toLong()))
            for (p in Prayer.entries) {
                if (!includeSunrise && p == Prayer.SUNRISE) continue
                if (t[p].isAfter(now)) return p to t[p]
            }
        }
        val t = times(now.toLocalDate().plusDays(2)); return Prayer.FAJR to t[Prayer.FAJR]
    }

    /** The prayer whose time we are currently in (the last one that has passed). */
    fun currentPrayer(now: ZonedDateTime = ZonedDateTime.now(Settings.zone)): Prayer? {
        val t = times(now.toLocalDate())
        return Prayer.entries.lastOrNull { !t[it].isAfter(now) }
    }

    fun events(from: Long): List<PrayerEvent> {
        val zone = Settings.zone
        val start = java.time.Instant.ofEpochMilli(from).atZone(zone).toLocalDate()
        val out = ArrayList<PrayerEvent>()
        for (d in -1..2) {
            val t = times(start.plusDays(d.toLong()))
            for (p in Prayer.entries) {
                val at = t[p].toInstant().toEpochMilli()
                val pre = Settings.preAlert(p)
                if (pre > 0) out += PrayerEvent(EventType.PRE, p, at - pre * 60_000L, at)
                if (Settings.mode(p) != AlertMode.SILENT) out += PrayerEvent(EventType.ADHAN, p, at, at)
                val iq = Settings.iqama(p)
                if (iq > 0 && p.hasAdhan) out += PrayerEvent(EventType.IQAMA, p, at + iq * 60_000L, at)
            }
        }
        return out.filter { it.at > from }.sortedBy { it.at }
    }

    fun nextEvent(now: Long = System.currentTimeMillis()): PrayerEvent? = events(now + 500).firstOrNull()

    /** Arm the alarm for the next event (replacing any previous one). */
    fun schedule(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val ev = nextEvent() ?: return
        val pi = PendingIntent.getBroadcast(
            ctx, 1,
            Intent(ctx, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_FIRE
                putExtra("type", ev.type.name)
                putExtra("prayer", ev.prayer.name)
                putExtra("at", ev.at)
                putExtra("prayerTime", ev.prayerTime)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val show = PendingIntent.getActivity(
            ctx, 2, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val exactOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (exactOk) {
            // setAlarmClock is the one alarm type Doze and OEM battery savers never defer
            am.setAlarmClock(AlarmManager.AlarmClockInfo(ev.at, show), pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ev.at, pi)
        }
        KeepAliveService.refresh(ctx)
    }
}
