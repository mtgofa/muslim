package com.mtgofa.muslem.prayer

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.mtgofa.muslem.MainActivity
import com.mtgofa.muslem.MuslemApp
import com.mtgofa.muslem.R
import com.mtgofa.muslem.data.Settings
import java.time.LocalDate

/**
 * A quiet, permanent notification that shows the next prayer with a live countdown. Being a
 * foreground service keeps the process (and the alarm chain) alive on phones whose vendor
 * battery managers kill background apps.
 */
class KeepAliveService : Service() {

    private val main = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Settings.init(this)
        if (!Settings.keepAlive) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, NOTE_ID, build(), type)
        // the system may have restarted us after killing the process: make sure the alarm is armed
        if (intent == null) Scheduler.schedule(this)
        planNextUpdate()
        return START_STICKY
    }

    fun update() {
        getSystemService(NotificationManager::class.java).notify(NOTE_ID, build())
        planNextUpdate()
    }

    private fun planNextUpdate() {
        main.removeCallbacksAndMessages(null)
        val (_, at) = Scheduler.nextPrayer()
        val delay = (at.toInstant().toEpochMilli() - System.currentTimeMillis() + 1_000).coerceIn(5_000, 3_600_000)
        main.postDelayed({ update() }, delay)
    }

    private fun build(): Notification {
        val (p, at) = Scheduler.nextPrayer()
        val atMillis = at.toInstant().toEpochMilli()
        val name = p.label(at.toLocalDate())
        val today = LocalDate.now(Settings.zone)
        val day = if (at.toLocalDate() == today) "" else " (غداً)"
        val title = (if (p == Prayer.SUNRISE) "الشروق" else "صلاة $name") + " ${AdhanService.formatTime(atMillis)}$day"
        val open = PendingIntent.getActivity(this, 5, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, MuslemApp.CH_STATUS)
            .setSmallIcon(R.drawable.ic_stat_mosque)
            .setContentTitle(title)
            .setContentText("${Settings.locationName} · ${Hijri.of(today)}")
            .setSubText("الصلاة القادمة")
            .setWhen(atMillis)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setColor(0xFF0F7A62.toInt())
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(open)
            .build()
    }

    override fun onDestroy() {
        main.removeCallbacksAndMessages(null)
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        private const val NOTE_ID = 20
        @Volatile private var instance: KeepAliveService? = null

        /** Start, update or stop the status notification to match the settings. */
        fun refresh(ctx: Context) {
            Settings.init(ctx)
            val running = instance
            if (!Settings.keepAlive) {
                running?.let { ctx.stopService(Intent(ctx, KeepAliveService::class.java)) }
                return
            }
            if (running != null) { running.main.post { running.update() }; return }
            runCatching {
                ContextCompat.startForegroundService(ctx, Intent(ctx, KeepAliveService::class.java).setAction("start"))
            }
        }
    }
}
