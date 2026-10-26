package com.mtgofa.muslim.prayer

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
     * The adhan screen over the lock screen. Since Android 14 a store install of an app that is
     * not an alarm clock or a dialer may be refused this, and then only a notification appears.
     */
    fun fullScreen(ctx: Context) = Build.VERSION.SDK_INT < 34 ||
        ctx.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    fun openFullScreen(ctx: Context) {
        runCatching {
            ctx.startActivity(Intent(AndroidSettings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure { openAppInfo(ctx) }
    }

    val isXiaomi: Boolean
        get() = Build.MANUFACTURER.lowercase() in setOf("xiaomi", "redmi", "poco")

    /**
     * Xiaomi's own permissions for this app («show on lock screen», «display pop-up windows while
     * running in the background»): without them the adhan screen never appears on MIUI/HyperOS.
     */
    fun openXiaomiPermissions(ctx: Context) {
        val tries = listOf(
            Intent("miui.intent.action.APP_PERM_EDITOR").setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity"),
            Intent("miui.intent.action.APP_PERM_EDITOR").setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.AppPermissionsEditorActivity"),
            Intent("miui.intent.action.APP_PERM_EDITOR"),
        )
        for (i in tries) {
            i.putExtra("extra_pkgname", ctx.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { ctx.startActivity(i) }.isSuccess) return
        }
        openAppInfo(ctx)
    }

    /**
     * What the home screen warns about: only what stops the adhan outright. Battery restriction
     * and the persistent notification are offered on the reliability page but never nagged about.
     */
    fun allGood(ctx: Context) = notificationsOn(ctx) && exactAlarms(ctx)

    /** The battery settings list (no one-tap request: that permission is restricted on Play). */
    fun askBattery(ctx: Context) {
        runCatching { ctx.startActivity(Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
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
