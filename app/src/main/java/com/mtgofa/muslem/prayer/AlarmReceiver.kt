package com.mtgofa.muslem.prayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.mtgofa.muslem.data.Settings

/** Fires at each prayer event, hands it to [AdhanService] and arms the next alarm. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Settings.init(ctx)
        if (intent.action == ACTION_FIRE) {
            val at = intent.getLongExtra("at", 0)
            val late = System.currentTimeMillis() - at
            // an alarm delivered long after its time (phone was off) must not blast an adhan
            if (late < LATE_LIMIT_MS) {
                ContextCompat.startForegroundService(ctx, Intent(ctx, AdhanService::class.java).apply {
                    action = AdhanService.ACTION_PLAY
                    putExtras(intent)
                })
            }
        }
        Scheduler.schedule(ctx)
    }

    companion object {
        const val ACTION_FIRE = "com.mtgofa.muslem.FIRE"
        private const val LATE_LIMIT_MS = 5 * 60_000L
    }
}

/** Re-arms the chain after reboot, app update, clock or time-zone changes. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Settings.init(ctx)
        Scheduler.schedule(ctx)
        com.mtgofa.muslem.data.Qiyam.schedule(ctx)
        // wird and fasting reminders keep their data in credential storage: wait for the unlock
        // (BOOT_COMPLETED follows LOCKED_BOOT_COMPLETED once the user unlocks)
        if (!com.mtgofa.muslem.data.Storage.unlocked(ctx)) return
        com.mtgofa.muslem.data.Storage.migrate(ctx)
        com.mtgofa.muslem.data.Wird.schedule(ctx)
        com.mtgofa.muslem.data.Fasting.schedule(ctx)
        com.mtgofa.muslem.data.AdhkarReminder.schedule(ctx)
    }
}
