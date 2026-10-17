package com.mtgofa.muslem.data

import android.content.Context
import com.mtgofa.muslem.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import java.io.File

/**
 * Editable app content (adhkar, tasbeeh phrases, reciters, muezzins). Each file ships inside
 * the APK so everything works offline, and lives in the repo's `content/` folder: when a file
 * there changes, the app downloads it in the background and uses it from then on — no app
 * release needed.
 */
object Content {
    const val ADHKAR = "adhkar.json"
    const val TASBEEH = "tasbeeh.json"
    const val RECITERS = "reciters.json"
    const val MUEZZINS = "muezzins.json"
    private val FILES = listOf(ADHKAR, TASBEEH, RECITERS, MUEZZINS)
    private const val SYNC_EVERY_MS = 6 * 3600_000L

    lateinit var app: Context; private set

    /** ticks whenever a newer copy of any file was installed, so screens reload */
    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version

    private val listeners = mutableListOf<() -> Unit>()

    fun init(ctx: Context) { app = ctx.applicationContext }

    /** Drop a parsed cache when content changes. */
    fun onChange(block: () -> Unit) { synchronized(listeners) { listeners += block } }

    private fun dir() = File(app.filesDir, "content")
    private fun prefs() = app.getSharedPreferences("content", Context.MODE_PRIVATE)

    /** The newest copy of [name]: downloaded if we have one, otherwise the bundled one. */
    fun read(name: String): String {
        val f = File(dir(), name)
        if (f.length() > 0) runCatching { return f.readText() }
        return app.assets.open(name).bufferedReader().use { it.readText() }
    }

    private fun url(name: String) =
        "https://raw.githubusercontent.com/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/main/content/$name"

    /** Check GitHub for newer content (at most every few hours unless [force]). */
    suspend fun sync(force: Boolean = false) = withContext(Dispatchers.IO) {
        if (BuildConfig.GITHUB_OWNER.isBlank()) return@withContext
        val p = prefs()
        if (!force && System.currentTimeMillis() - p.getLong("lastSync", 0) < SYNC_EVERY_MS) return@withContext
        var changed = false
        for (name in FILES) {
            runCatching {
                val req = Request.Builder().url(url(name)).header("User-Agent", "Muslem-Android")
                    .apply { p.getString("etag_$name", null)?.let { header("If-None-Match", it) } }
                    .build()
                Http.client.newCall(req).execute().use { resp ->
                    if (resp.code != 200) return@use
                    val body = resp.body?.string().orEmpty()
                    if (JSONArray(body).length() == 0) return@use // never replace content with an empty list
                    dir().mkdirs()
                    val tmp = File(dir(), "$name.tmp")
                    tmp.writeText(body)
                    tmp.renameTo(File(dir(), name))
                    p.edit().putString("etag_$name", resp.header("ETag")).apply()
                    changed = true
                }
            }
        }
        p.edit().putLong("lastSync", System.currentTimeMillis()).apply()
        if (changed) {
            synchronized(listeners) { listeners.forEach { it() } }
            _version.value++
        }
    }
}
