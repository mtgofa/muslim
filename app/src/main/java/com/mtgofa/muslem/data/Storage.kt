package com.mtgofa.muslem.data

import android.content.Context
import android.os.UserManager
import java.io.File

/**
 * Direct Boot: after a reboot the phone may stay locked all night, and only device-protected
 * storage can be read until the first unlock. Everything the adhan needs (settings and the
 * downloaded muezzin voices) lives there, so the Fajr adhan still fires on a phone that
 * restarted and was never unlocked.
 */
object Storage {
    fun unlocked(ctx: Context): Boolean = ctx.getSystemService(UserManager::class.java)?.isUserUnlocked ?: true

    /** device-protected context: readable before the first unlock */
    fun dp(ctx: Context): Context = ctx.applicationContext.createDeviceProtectedStorageContext()

    /** Move what earlier versions kept in credential storage over to device storage (once). */
    fun migrate(ctx: Context) {
        if (!unlocked(ctx)) return
        val app = ctx.applicationContext
        val old = File(app.filesDir, "adhan")
        if (old.isDirectory) {
            val dst = File(dp(app).filesDir, "adhan").apply { mkdirs() }
            // a copy, not a rename: the two areas are encrypted differently and rename fails across them
            old.listFiles()?.forEach { f ->
                val t = File(dst, f.name)
                if (t.length() != f.length()) runCatching { f.copyTo(File(dst, f.name + ".part"), true).renameTo(t) }
                if (t.length() == f.length()) f.delete()
            }
            old.delete()
        }
    }
}
