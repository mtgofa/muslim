package com.mtgofa.muslim

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.mtgofa.muslim.data.Settings
import com.mtgofa.muslim.prayer.Scheduler
import com.mtgofa.muslim.quran.QuranDb
import kotlin.concurrent.thread

class MuslimApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Settings.init(this)
        com.mtgofa.muslim.data.Content.init(this)
        createChannels()
        // before the first unlock after a reboot only the adhan chain can run (see Storage)
        if (!com.mtgofa.muslim.data.Storage.unlocked(this)) {
            Scheduler.schedule(this)
            com.mtgofa.muslim.data.Qiyam.schedule(this)
            return
        }
        com.mtgofa.muslim.data.Storage.migrate(this)
        com.mtgofa.muslim.data.Content.dropStaleDownloads()
        com.mtgofa.muslim.data.Wird.init(this)
        com.mtgofa.muslim.data.Fasting.init(this)
        thread(name = "quran-db") { QuranDb.init(this) }
        Scheduler.schedule(this)
        com.mtgofa.muslim.data.Wird.schedule(this)
        com.mtgofa.muslim.data.Fasting.schedule(this)
        com.mtgofa.muslim.data.AdhkarReminder.schedule(this)
        com.mtgofa.muslim.data.Qiyam.schedule(this)
    }

    private fun createChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_ADHAN, "الأذان والتنبيهات", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "الأذان وتنبيه ما قبل الأذان والإقامة"
                setSound(null, null) // the service plays the audio itself on the alarm stream
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_ADHKAR, "تذكير الأذكار", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "تذكير أذكار الصباح والمساء بصوت الإشعار"
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_ADHKAR_QUIET, "تذكير الأذكار بالاهتزاز", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "تذكير أذكار الصباح والمساء بالاهتزاز فقط"
                setSound(null, null)
                enableVibration(false) // the app vibrates in its own pattern
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_WIRD, "الورد اليومي", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "تذكير يومي بورد القرآن"
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_STATUS, "الصلاة القادمة", NotificationManager.IMPORTANCE_LOW).apply {
                description = "إشعار دائم بموعد الصلاة القادمة يحافظ على عمل الأذان"
                setShowBadge(false)
                setSound(null, null)
            },
        )
    }

    companion object {
        const val CH_ADHAN = "adhan"
        const val CH_STATUS = "status"
        const val CH_WIRD = "wird"
        const val CH_ADHKAR = "adhkar"
        const val CH_ADHKAR_QUIET = "adhkar_quiet"
    }
}
