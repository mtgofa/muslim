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
    /** present once the user was asked about the current report, so the prompt shows only once */
    private fun seen(ctx: Context) = File(Storage.dp(ctx).filesDir, "last-crash.seen")

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
            seen(app).delete()
        }
    }

    /** The saved report, or null when there is none. */
    fun read(ctx: Context): String? = runCatching { file(ctx).takeIf { it.exists() }?.readText() }.getOrNull()

    /** The report, only if the user was not asked about it yet (for the prompt on launch). */
    fun unseen(ctx: Context): String? = if (runCatching { seen(ctx).exists() }.getOrDefault(true)) null else read(ctx)

    fun markSeen(ctx: Context) { runCatching { seen(ctx).createNewFile() } }

    fun clear(ctx: Context) { runCatching { file(ctx).delete(); seen(ctx).delete() } }

    /**
     * Hand the report to any app the user picks (Gmail, WhatsApp, …). ACTION_SEND, not a mailto
     * link: mail apps drop the text of a mailto intent, which sent empty emails. The report stays
     * on the phone (the feedback page can send it again) until a newer one replaces it.
     */
    fun share(ctx: Context, report: String): Boolean {
        val send = android.content.Intent(android.content.Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(Feedback.EMAIL))
            .putExtra(android.content.Intent.EXTRA_SUBJECT, "مسلم — تقرير عطل")
            .putExtra(android.content.Intent.EXTRA_TEXT, report)
        return runCatching {
            ctx.startActivity(android.content.Intent.createChooser(send, "أرسل التقرير عبر").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
    }
}

/** Where notes and reports go. */
object Feedback {
    const val EMAIL = "gofait25@gmail.com"

    /**
     * Open the user's mail app with [subject] and [body] filled in. They go in the mailto link
     * itself: many mail apps ignore the extras of a mailto intent and opened an empty message.
     */
    fun mail(ctx: Context, subject: String, body: String): Boolean {
        val uri = android.net.Uri.parse("mailto:$EMAIL?subject=${android.net.Uri.encode(subject)}&body=${android.net.Uri.encode(body)}")
        val mail = android.content.Intent(android.content.Intent.ACTION_SENDTO, uri)
            .putExtra(android.content.Intent.EXTRA_SUBJECT, subject)
            .putExtra(android.content.Intent.EXTRA_TEXT, body)
        return runCatching { ctx.startActivity(mail) }.isSuccess
    }
}
