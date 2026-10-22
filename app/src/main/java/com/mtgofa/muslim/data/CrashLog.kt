package com.mtgofa.muslim.data

import android.content.Context
import android.os.Build
import com.mtgofa.muslim.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The last crash, kept on the phone so the user can send it from the app: phones in the hands of
 * users report nothing on their own, and a stack trace is the only way to know what broke.
 * Device-protected storage, so a crash before the first unlock is kept too.
 */
object CrashLog {
    private const val MAX = 12_000

    private fun file(ctx: Context) = File(Storage.dp(ctx).filesDir, "last-crash.txt")

    fun install(ctx: Context) {
        val app = ctx.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            write(app, "الخيط: ${thread.name}", e)
            previous?.uncaughtException(thread, e)
        }
    }

    /** An error the app survived (it showed a message instead of closing); kept the same way. */
    fun record(ctx: Context, where: String, e: Throwable) = write(ctx.applicationContext, "المكان: $where", e)

    private fun write(app: Context, where: String, e: Throwable) {
        runCatching {
            val text = buildString {
                append("الوقت: ").append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())).append('\n')
                append("الإصدار: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n")
                append("الجهاز: ${Build.MANUFACTURER} ${Build.MODEL}\n")
                append("أندرويد: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
                append(where).append("\n\n")
                append(e.stackTraceToString())
            }
            file(app).writeText(text.take(MAX))
        }
    }

    /** The saved report, or null when there is none. */
    fun read(ctx: Context): String? = runCatching { file(ctx).takeIf { it.exists() }?.readText() }.getOrNull()

    fun clear(ctx: Context) { runCatching { file(ctx).delete() } }
}
