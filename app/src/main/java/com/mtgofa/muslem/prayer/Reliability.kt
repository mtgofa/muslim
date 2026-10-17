package com.mtgofa.muslem.prayer

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings as AndroidSettings

/** Checks and shortcuts for everything that can stop an adhan from sounding on time. */
object Reliability {
    fun notificationsOn(ctx: Context) = ctx.getSystemService(NotificationManager::class.java).areNotificationsEnabled()

    fun batteryUnrestricted(ctx: Context) =
        ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)

    fun exactAlarms(ctx: Context) = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    /**
     * What the home screen warns about: only what stops the adhan outright. Battery restriction
     * and the persistent notification are offered on the reliability page but never nagged about.
     */
    fun allGood(ctx: Context) = notificationsOn(ctx) && exactAlarms(ctx)

    @SuppressLint("BatteryLife")
    fun askBattery(ctx: Context) {
        val direct = Intent(AndroidSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))
        runCatching { ctx.startActivity(direct) }.onFailure {
            runCatching { ctx.startActivity(Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
        }
    }

    fun openExactAlarm(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) runCatching {
            ctx.startActivity(Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}")))
        }
    }

    fun openNotificationSettings(ctx: Context) {
        runCatching {
            ctx.startActivity(Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, ctx.packageName))
        }
    }

    /** App info page — where vendor "autostart" / "no restrictions" switches usually live. */
    fun openAppInfo(ctx: Context) {
        runCatching {
            ctx.startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
        }
    }

    /** Vendor auto-start managers (Xiaomi, Oppo, Vivo, Huawei…); falls back to app info. */
    fun openAutostart(ctx: Context) {
        val candidates = listOf(
            "com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity",
            "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
            "com.oppo.safe" to "com.oppo.safe.permission.startup.StartupAppListActivity",
            "com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
            "com.huawei.systemmanager" to "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
            "com.samsung.android.lool" to "com.samsung.android.sm.battery.ui.BatteryActivity",
        )
        for ((pkg, cls) in candidates) {
            val i = Intent().setClassName(pkg, cls).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { ctx.startActivity(i) }.isSuccess) return
        }
        openAppInfo(ctx)
    }

    val vendorHasAutostart: Boolean
        get() = Build.MANUFACTURER.lowercase() in setOf("xiaomi", "redmi", "poco", "oppo", "realme", "vivo", "huawei", "honor", "samsung", "infinix", "tecno")
}
