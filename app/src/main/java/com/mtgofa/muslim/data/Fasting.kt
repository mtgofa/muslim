package com.mtgofa.muslim.data

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import com.mtgofa.muslim.MainActivity
import com.mtgofa.muslim.MuslimApp
import com.mtgofa.muslim.R
import com.mtgofa.muslim.prayer.Hijri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** A voluntary fast on [date], with why it is recommended. */
data class FastDay(val date: LocalDate, val reasons: List<String>)

/**
 * Voluntary fasts: Mondays and Thursdays, and the three white days (13–15 of each Hijri month),
 * each switchable, with a reminder the evening before.
 */
object Fasting {
    private lateinit var sp: SharedPreferences
    private val _changes = MutableStateFlow(0)
    val changes: StateFlow<Int> = _changes

    fun init(ctx: Context) {
        if (::sp.isInitialized) return
        sp = ctx.applicationContext.getSharedPreferences("fasting", Context.MODE_PRIVATE)
    }

    private fun edit(block: SharedPreferences.Editor.() -> Unit) { sp.edit().apply(block).apply(); _changes.value++ }

    var monThu: Boolean
        get() = sp.getBoolean("monThu", false)
        set(v) = edit { putBoolean("monThu", v) }
    var whiteDays: Boolean
        get() = sp.getBoolean("white", false)
        set(v) = edit { putBoolean("white", v) }
    var remindOn: Boolean
        get() = sp.getBoolean("remind", true)
        set(v) = edit { putBoolean("remind", v) }
    /** evening reminder time */
    var remindAt: LocalTime
        get() = sp.getInt("remindAt", 21 * 60).let { LocalTime.of(it / 60, it % 60) }
        set(v) = edit { putInt("remindAt", v.hour * 60 + v.minute) }

    val anyOn get() = monThu || whiteDays

    /**
     * Days a voluntary fast is not kept: all of Ramadan (it is obligatory then), the two Eids and
     * the days of Tashreeq.
     */
    private fun excluded(d: LocalDate): Boolean {
        val h = Hijri.of(d)
        return h.month == 9 || (h.month == 10 && h.day == 1) || (h.month == 12 && h.day in 10..13)
    }

    fun reasonsFor(d: LocalDate): List<String> {
        if (excluded(d)) return emptyList()
        val out = ArrayList<String>(2)
        if (monThu && d.dayOfWeek == DayOfWeek.MONDAY) out += "صيام الاثنين"
        if (monThu && d.dayOfWeek == DayOfWeek.THURSDAY) out += "صيام الخميس"
        if (whiteDays) {
            val h = Hijri.of(d)
            if (h.day in 13..15) out += "الأيام البيض (${listOf("الأول", "الثاني", "الثالث")[h.day - 13]})"
        }
        return out
    }

    /** Fast days from [from] onward, up to [limit] of them within [days] days. */
    fun upcoming(from: LocalDate = LocalDate.now(), days: Int = 60, limit: Int = 20): List<FastDay> =
        (0 until days).asSequence().map { from.plusDays(it.toLong()) }
            .mapNotNull { d -> reasonsFor(d).takeIf { it.isNotEmpty() }?.let { FastDay(d, it) } }
            .take(limit).toList()

    // ---- evening-before reminder ----------------------------------------------------------------

    private const val NOTE_ID = 40

    /** Arm an alarm for the evening before the next fast day. */
    fun schedule(ctx: Context) {
        init(ctx)
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(ctx, 41, Intent(ctx, FastingReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        am.cancel(pi)
        if (!anyOn || !remindOn) return
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        // the next fast day whose evening-before reminder is still ahead
        val next = upcoming(now.toLocalDate(), days = 90, limit = 3)
            .map { ZonedDateTime.of(it.date.minusDays(1), remindAt, zone) }
            .firstOrNull { it.isAfter(now) } ?: return
        val millis = next.toInstant().toEpochMilli()
        val exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
    }

    internal fun remind(ctx: Context) {
        init(ctx)
        val tomorrow = LocalDate.now().plusDays(1)
        val reasons = reasonsFor(tomorrow)
        if (reasons.isNotEmpty() && remindOn) {
            val open = PendingIntent.getActivity(
                ctx, 42, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val n = NotificationCompat.Builder(ctx, MuslimApp.CH_WIRD)
                .setSmallIcon(R.drawable.ic_stat_mosque)
                .setContentTitle("غداً ${reasons.joinToString(" · ")}")
                .setContentText("انوِ الصيام، ولا تنسَ السحور ﴿وَأَن تَصُومُوا خَيْرٌ لَّكُمْ﴾")
                .setColor(0xFF0F7A62.toInt())
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            ctx.getSystemService(NotificationManager::class.java).notify(NOTE_ID, n)
        }
        schedule(ctx)
    }
}

class FastingReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) = Fasting.remind(ctx)
}
