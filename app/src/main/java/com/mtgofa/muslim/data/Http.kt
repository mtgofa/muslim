package com.mtgofa.muslim.data

import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Plain HttpURLConnection: the platform client is enough here and keeps the app small. */
object Http {
    class Response(val code: Int, val body: String, private val headers: Map<String, List<String>>) {
        val isSuccessful get() = code in 200..299
        fun header(name: String): String? = headers.entries.firstOrNull { it.key.equals(name, true) }?.value?.firstOrNull()
    }

    private fun open(url: String, headers: Map<String, String>): HttpURLConnection {
        var u = url
        // follow redirects by hand: HttpURLConnection will not cross http↔https on its own
        repeat(6) {
            val c = (URL(u).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 60_000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "Muslim-Android")
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
            }
            val code = c.responseCode
            if (code !in 300..399 || code == 304) return c
            val next = c.getHeaderField("Location") ?: return c
            c.disconnect()
            u = URL(URL(u), next).toString()
        }
        throw java.io.IOException("too many redirects")
    }

    /** GET [url] as text. */
    fun get(url: String, headers: Map<String, String> = emptyMap()): Response {
        val c = open(url, headers)
        try {
            val code = c.responseCode
            val body = if (code in 200..299) c.inputStream.bufferedReader().use { it.readText() } else ""
            return Response(code, body, c.headerFields.filterKeys { it != null })
        } finally {
            c.disconnect()
        }
    }

    /** Download [url] into [out] atomically (via a .part file). Returns true on success. */
    fun download(url: String, out: File, progress: (Float) -> Unit = {}): Boolean = try { downloadOrThrow(url, out, progress) } catch (e: Exception) { false }

    private fun downloadOrThrow(url: String, out: File, progress: (Float) -> Unit): Boolean {
        out.parentFile?.mkdirs()
        val tmp = File(out.path + ".part")
        val c = open(url, emptyMap())
        try {
            if (c.responseCode !in 200..299) return false
            val total = c.contentLengthLong
            var done = 0L
            c.inputStream.use { input ->
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
        } finally {
            c.disconnect()
        }
        return tmp.renameTo(out)
    }
}
