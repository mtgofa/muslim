package com.mtgofa.muslem.data

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import com.mtgofa.muslem.MainActivity
import com.mtgofa.muslem.MuslemApp
import com.mtgofa.muslem.R
import com.mtgofa.muslem.prayer.arabicDigits
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The daily wird: a khatma read a fixed number of pages a day. The next page to read moves on
 * by itself whenever the reader turns forward past it in the Mushaf.
 */
object Wird {
    const val LAST_PAGE = 604
    private lateinit var sp: SharedPreferences
    private val _changes = MutableStateFlow(0)
    val changes: StateFlow<Int> = _changes

    fun init(ctx: Context) {
        if (::sp.isInitialized) return
        sp = ctx.applicationContext.getSharedPreferences("wird", Context.MODE_PRIVATE)
    }

    private fun edit(block: SharedPreferences.Editor.() -> Unit) { sp.edit().apply(block).apply(); _changes.value++ }

    val active get() = sp.getBoolean("active", false)
    val pagesPerDay get() = sp.getInt("ppd", 5)
    val startPage get() = sp.getInt("start", 1)
    /** the next page of the khatma to read; past [LAST_PAGE] means it is complete */
    val next get() = sp.getInt("next", 1)
    val reminder: LocalTime? get() = sp.getInt("remind", -1).takeIf { it >= 0 }?.let { LocalTime.of(it / 60, it % 60) }
    val startedOn: LocalDate get() = LocalDate.ofEpochDay(sp.getLong("startDay", LocalDate.now().toEpochDay()))

    val complete get() = next > LAST_PAGE
    val totalPages get() = LAST_PAGE - startPage + 1
    val readPages get() = (next - startPage).coerceIn(0, totalPages)

    /** pages read today (resets at midnight) */
    val readToday: Int
        get() = if (sp.getLong("todayDay", -1) == LocalDate.now().toEpochDay()) sp.getInt("todayCount", 0) else 0
    val leftToday get() = (pagesPerDay - readToday).coerceAtLeast(0).coerceAtMost((LAST_PAGE - next + 1).coerceAtLeast(0))

    /** Days needed to finish [remaining] pages at [perDay] a day, and the day it ends. */
    fun estimate(remaining: Int, perDay: Int, from: LocalDate = LocalDate.now()): Pair<Int, LocalDate> {
        val days = ((remaining + perDay - 1) / perDay).coerceAtLeast(1)
        return days to from.plusDays(days - 1L)
    }

    /** When the current khatma should end, counting today's remaining pages. */
    fun expectedEnd(): LocalDate {
        val remaining = LAST_PAGE - next + 1
        if (remaining <= 0) return LocalDate.now()
        // today's portion first, then whole days
        val afterToday = (remaining - leftToday).coerceAtLeast(0)
        val more = (afterToday + pagesPerDay - 1) / pagesPerDay
        return LocalDate.now().plusDays(more.toLong())
    }

    fun start(ctx: Context, perDay: Int, from: Int, remindAt: LocalTime?) {
        edit {
            putBoolean("active", true); putInt("ppd", perDay); putInt("start", from); putInt("next", from)
            putInt("remind", remindAt?.let { it.hour * 60 + it.minute } ?: -1)
            putLong("startDay", LocalDate.now().toEpochDay()); putLong("todayDay", -1); putInt("todayCount", 0)
        }
        schedule(ctx)
    }

    fun update(ctx: Context, perDay: Int, remindAt: LocalTime?) {
        edit { putInt("ppd", perDay); putInt("remind", remindAt?.let { it.hour * 60 + it.minute } ?: -1) }
        schedule(ctx)
    }

    fun stop(ctx: Context) { edit { putBoolean("active", false) }; schedule(ctx) }

    /**
     * The reader turned from [from] to [to] in the Mushaf. Turning forward past the next wird
     * page records it as read.
     */
    fun onPageTurn(ctx: Context, from: Int, to: Int) {
        if (!active || complete) return
        if (from == next && to == from + 1) markRead(ctx)
    }

    /** Record the next page as read (also used for the very last page, which has no "next"). */
    fun markRead(ctx: Context) {
        if (!active || complete) return
        val today = LocalDate.now().toEpochDay()
        val count = if (sp.getLong("todayDay", -1) == today) sp.getInt("todayCount", 0) else 0
        edit { putInt("next", next + 1); putLong("todayDay", today); putInt("todayCount", count + 1) }
        if (leftToday == 0) ctx.getSystemService(NotificationManager::class.java).cancel(NOTE_ID)
    }

    /** "وجه واحد", "وجهان", "٣ أوجه", "١١ وجهاً" */
    fun pages(n: Int): String = when {
        n == 1 -> "وجه واحد"
        n == 2 -> "وجهان"
        n in 3..10 -> "${arabicDigits(n)} أوجه"
        else -> "${arabicDigits(n)} وجهاً"
    }

    // ---- daily reminder ---------------------------------------------------------------------

    private const val NOTE_ID = 30

    fun schedule(ctx: Context) {
        init(ctx)
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            ctx, 31, Intent(ctx, WirdReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        am.cancel(pi)
        val at = reminder ?: return
        if (!active || complete) return
        val zone = ZoneId.systemDefault()
        var whenAt = ZonedDateTime.of(LocalDate.now(zone), at, zone)
        if (!whenAt.isAfter(ZonedDateTime.now(zone))) whenAt = whenAt.plusDays(1)
        val millis = whenAt.toInstant().toEpochMilli()
        val exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
    }

    internal fun remind(ctx: Context) {
        init(ctx)
        if (active && !complete && leftToday > 0) {
            val open = PendingIntent.getActivity(
                ctx, 32,
                Intent(ctx, MainActivity::class.java).putExtra("openMushaf", true).putExtra("page", next)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val n = NotificationCompat.Builder(ctx, MuslemApp.CH_WIRD)
                .setSmallIcon(R.drawable.ic_stat_mosque)
                .setContentTitle("حان وقت وردك اليومي")
                .setContentText("باقي ${pages(leftToday)} · من صفحة ${arabicDigits(next)}")
                .setColor(0xFF0F7A62.toInt())
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            ctx.getSystemService(NotificationManager::class.java).notify(NOTE_ID, n)
        }
        schedule(ctx)
    }
}

class WirdReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) = Wird.remind(ctx)
}
