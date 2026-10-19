package com.mtgofa.muslem.data

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.mtgofa.muslem.MainActivity
import com.mtgofa.muslem.MuslemApp
import com.mtgofa.muslem.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Daily reminders for the morning and evening adhkar, each at its own time, as a notification
 * with a sound or as a vibration only. One alarm is armed at a time: the next of the two.
 */
object AdhkarReminder {
    private const val EXTRA = "chapter"

    private fun times(): List<Pair<Int, Int>> = buildList {
        if (Settings.adhkarMorningOn) add(Adhkar.MORNING to Settings.adhkarMorningAt)
        if (Settings.adhkarEveningOn) add(Adhkar.EVENING to Settings.adhkarEveningAt)
    }

    fun schedule(ctx: Context) {
        Settings.init(ctx)
        val am = ctx.getSystemService(AlarmManager::class.java)
        am.cancel(intent(ctx, 0))
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        val (chapter, at) = times().map { (id, min) ->
            var t = ZonedDateTime.of(LocalDate.now(zone), LocalTime.of(min / 60, min % 60), zone)
            if (!t.isAfter(now)) t = t.plusDays(1)
            id to t
        }.minByOrNull { it.second } ?: return
        val millis = at.toInstant().toEpochMilli()
        val pi = intent(ctx, chapter)
        val exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
    }

    private fun intent(ctx: Context, chapter: Int) = PendingIntent.getBroadcast(
        ctx, 51, Intent(ctx, AdhkarReceiver::class.java).putExtra(EXTRA, chapter),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    internal fun fire(ctx: Context, chapter: Int) {
        Settings.init(ctx)
        val morning = chapter == Adhkar.MORNING
        if (if (morning) Settings.adhkarMorningOn else Settings.adhkarEveningOn) show(ctx, chapter)
        schedule(ctx)
    }

    /** The reminder itself; also used by the settings card to try it. */
    fun show(ctx: Context, chapter: Int) {
        val morning = chapter == Adhkar.MORNING
        val vibrate = Settings.adhkarVibrate
        val open = PendingIntent.getActivity(
            ctx, 52 + chapter,
            Intent(ctx, MainActivity::class.java).putExtra("openAdhkar", chapter)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(ctx, if (vibrate) MuslemApp.CH_ADHKAR_QUIET else MuslemApp.CH_ADHKAR)
            .setSmallIcon(R.drawable.ic_stat_mosque)
            .setContentTitle(if (morning) "أذكار الصباح" else "أذكار المساء")
            .setContentText(
                if (morning) "﴿فَسُبْحَانَ اللَّهِ حِينَ تُمْسُونَ وَحِينَ تُصْبِحُونَ﴾ — اضغط لقراءة أذكار الصباح"
                else "﴿وَسَبِّحْ بِحَمْدِ رَبِّكَ بِالْعَشِيِّ وَالْإِبْكَارِ﴾ — اضغط لقراءة أذكار المساء",
            )
            .setColor(0xFF0F7A62.toInt())
            .setContentIntent(open)
            .setAutoCancel(true)
            .setSilent(vibrate)
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify(60 + chapter, n)
        // tagged as an alarm vibration so phones do not mute it like touch feedback
        if (vibrate) Haptics.pattern(ctx, *Haptics.ADHKAR)
    }
}

class AdhkarReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) =
        AdhkarReminder.fire(ctx, intent.getIntExtra("chapter", Adhkar.MORNING))
}
