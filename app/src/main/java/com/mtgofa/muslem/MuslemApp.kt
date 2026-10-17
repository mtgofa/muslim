package com.mtgofa.muslem

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.mtgofa.muslem.data.Settings
import com.mtgofa.muslem.prayer.Scheduler
import com.mtgofa.muslem.quran.QuranDb
import kotlin.concurrent.thread

class MuslemApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Settings.init(this)
        com.mtgofa.muslem.data.Content.init(this)
        createChannels()
        thread(name = "quran-db") { QuranDb.init(this) }
        Scheduler.schedule(this)
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
    }
}
