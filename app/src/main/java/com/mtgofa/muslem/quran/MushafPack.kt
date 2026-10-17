package com.mtgofa.muslem.quran

import android.content.Context
import com.mtgofa.muslem.BuildConfig
import com.mtgofa.muslem.data.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream

/**
 * The 604 page fonts (≈70 MB) are not inside the APK — they are downloaded once from the
 * repo's "mushaf pack" release, so app updates stay small.
 */
object MushafPack {
    const val PAGES = 604
    private const val ZIP_NAME = "mushaf-pack.zip"

    sealed interface State {
        data object Missing : State
        data class Downloading(val progress: Float, val unpacking: Boolean = false) : State
        data object Ready : State
        data class Failed(val reason: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Missing)
    val state: StateFlow<State> = _state

    fun dir(ctx: Context) = File(ctx.filesDir, "mushaf-pack")
    private fun marker(ctx: Context) = File(dir(ctx), ".complete")

    fun pageFile(ctx: Context, page: Int) = File(dir(ctx), "p%03d.ttf".format(page))

    fun refresh(ctx: Context) {
        if (_state.value is State.Downloading) return
        _state.value = if (marker(ctx).exists()) State.Ready else State.Missing
    }

    val url: String
        get() = "https://github.com/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/releases/download/${BuildConfig.MUSHAF_PACK_TAG}/$ZIP_NAME"

    suspend fun download(ctx: Context) = withContext(Dispatchers.IO) {
        if (_state.value is State.Downloading) return@withContext
        _state.value = State.Downloading(0f)
        val zip = File(ctx.cacheDir, ZIP_NAME)
        val ok = Http.download(url, zip) { _state.value = State.Downloading(it) }
        if (!ok) { _state.value = State.Failed("تعذّر التنزيل — تأكّد من الاتصال بالإنترنت"); return@withContext }
        _state.value = State.Downloading(1f, unpacking = true)
        val out = dir(ctx)
        val count = runCatching {
            out.mkdirs()
            var n = 0
            ZipInputStream(zip.inputStream().buffered()).use { z ->
                while (true) {
                    val e = z.nextEntry ?: break
                    val name = File(e.name).name
                    if (e.isDirectory || !name.matches(Regex("p\\d{3}\\.ttf"))) continue
                    File(out, name).outputStream().use { z.copyTo(it) }
                    n++
                }
            }
            n
        }.getOrDefault(0)
        zip.delete()
        if (count >= PAGES) {
            marker(ctx).writeText("$count")
            _state.value = State.Ready
        } else {
            _state.value = State.Failed("الملف غير مكتمل، أعد المحاولة")
        }
    }
}
