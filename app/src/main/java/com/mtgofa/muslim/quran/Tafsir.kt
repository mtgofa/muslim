package com.mtgofa.muslim.quran

import android.content.Context
import com.mtgofa.muslim.BuildConfig
import com.mtgofa.muslim.data.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.util.zip.GZIPInputStream

/**
 * «التفسير الميسر» for every ayah. Not inside the APK: about 550 KB downloaded once, the first
 * time the tafsir tab is opened, then read from disk one ayah at a time (only an offset index
 * stays in memory). File format: one line per ayah, "<sura>|<ayah>|<text>".
 */
object Tafsir {
    const val NAME = "التفسير الميسر"
    private const val ASSET = "tafsir-muyassar.txt.gz"
    private const val AYAHS = 6236

    sealed interface State {
        data object Missing : State
        data class Downloading(val progress: Float) : State
        data object Ready : State
        data class Failed(val reason: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Missing)
    val state: StateFlow<State> = _state

    private fun file(ctx: Context) = File(ctx.filesDir, "tafsir-muyassar.txt")
    private val url get() = "https://github.com/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/releases/download/${BuildConfig.MUSHAF_PACK_TAG}/$ASSET"

    /** sura * 1000 + ayah → byte offset of the text in the file */
    @Volatile private var index: HashMap<Int, Long>? = null
    private val lock = Mutex()

    fun refresh(ctx: Context) {
        if (_state.value is State.Downloading) return
        _state.value = if (file(ctx).length() > 0) State.Ready else State.Missing
    }

    suspend fun download(ctx: Context) = withContext(Dispatchers.IO) {
        if (_state.value is State.Downloading || file(ctx).length() > 0) { refresh(ctx); return@withContext }
        _state.value = State.Downloading(0f)
        val gz = File(ctx.cacheDir, ASSET)
        if (!Http.download(url, gz) { _state.value = State.Downloading(it) }) {
            _state.value = State.Failed("تعذّر تنزيل التفسير — تأكّد من الاتصال بالإنترنت"); return@withContext
        }
        val part = File(ctx.filesDir, "tafsir-muyassar.part")
        val lines = runCatching {
            GZIPInputStream(gz.inputStream().buffered()).use { i -> part.outputStream().use { i.copyTo(it) } }
            part.bufferedReader().useLines { s -> s.count { it.isNotBlank() } }
        }.getOrDefault(0)
        gz.delete()
        if (lines == AYAHS && part.renameTo(file(ctx))) _state.value = State.Ready
        else { part.delete(); _state.value = State.Failed("الملف غير مكتمل، أعد المحاولة") }
    }

    /** The tafsir of one ayah, or null if it is not downloaded yet. */
    suspend fun of(ctx: Context, sura: Int, ayah: Int): String? = withContext(Dispatchers.IO) {
        val f = file(ctx)
        if (f.length() == 0L) return@withContext null
        val idx = index ?: lock.withLock { index ?: buildIndex(f).also { index = it } }
        val at = idx[sura * 1000 + ayah] ?: return@withContext null
        RandomAccessFile(f, "r").use { r ->
            r.seek(at)
            val out = java.io.ByteArrayOutputStream(1024)
            val buf = ByteArray(4096)
            loop@ while (true) {
                val n = r.read(buf)
                if (n <= 0) break
                for (i in 0 until n) if (buf[i] == '\n'.code.toByte()) { out.write(buf, 0, i); break@loop }
                out.write(buf, 0, n)
            }
            out.toString("UTF-8")
        }
    }

    private fun buildIndex(f: File): HashMap<Int, Long> {
        val map = HashMap<Int, Long>(AYAHS * 2)
        f.inputStream().buffered(64 * 1024).use { input ->
            var pos = 0L
            var lineStart = true
            var field = 0
            var sura = 0; var ayah = 0
            while (true) {
                val b = input.read()
                if (b < 0) break
                pos++
                if (lineStart) { field = 0; sura = 0; ayah = 0; lineStart = false }
                when {
                    b == '\n'.code -> lineStart = true
                    field >= 2 -> Unit
                    b == '|'.code -> { field++; if (field == 2) map[sura * 1000 + ayah] = pos }
                    field == 0 -> sura = sura * 10 + (b - '0'.code)
                    else -> ayah = ayah * 10 + (b - '0'.code)
                }
            }
        }
        return map
    }
}
