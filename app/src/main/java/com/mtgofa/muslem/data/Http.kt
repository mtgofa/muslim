package com.mtgofa.muslem.data

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

object Http {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /** Download [url] into [out] atomically (via a .part file). Returns true on success. */
    fun download(url: String, out: File, progress: (Float) -> Unit = {}): Boolean = try {
        out.parentFile?.mkdirs()
        val tmp = File(out.path + ".part")
        client.newCall(Request.Builder().url(url).header("User-Agent", "Muslem-Android").build()).execute().use { resp ->
            if (!resp.isSuccessful) return false
            val body = resp.body ?: return false
            val total = body.contentLength()
            var done = 0L
            body.byteStream().use { input ->
                tmp.outputStream().use { o ->
                    val buf = ByteArray(32 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        o.write(buf, 0, n)
                        done += n
                        if (total > 0) progress(done.toFloat() / total)
                    }
                }
            }
        }
        tmp.renameTo(out)
    } catch (e: Exception) {
        false
    }
}
