package com.mtgofa.muslim.data

import android.content.Context
import com.mtgofa.muslim.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
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
    const val MEANINGS = "meanings.json"
    private val FILES = listOf(ADHKAR, TASBEEH, RECITERS, MUEZZINS, MEANINGS)
    private const val SYNC_EVERY_MS = 6 * 3600_000L

    lateinit var app: Context; private set

    /** ticks whenever a newer copy of any file was installed, so screens reload */
    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version

    private val listeners = mutableListOf<() -> Unit>()

    fun init(ctx: Context) { app = ctx.applicationContext }

    /**
     * After an app update the bundled files may be newer than what was downloaded for the old
     * version (a new chapter, say), so the downloaded copies are dropped and fetched again only
     * if GitHub has something newer still. Needs the unlocked user (credential storage).
     */
    fun dropStaleDownloads() {
        val p = prefs()
        if (p.getInt("bundle", 0) == BuildConfig.VERSION_CODE) return
        dir().deleteRecursively()
        p.edit().clear().putInt("bundle", BuildConfig.VERSION_CODE).apply()
    }

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
                val resp = Http.get(url(name), p.getString("etag_$name", null)?.let { mapOf("If-None-Match" to it) } ?: emptyMap())
                run {
                    if (resp.code != 200) return@run
                    val body = resp.body
                    // never replace content with an empty or broken file
                    val trimmed = body.trimStart()
                    val size = if (trimmed.startsWith("{")) org.json.JSONObject(body).length() else JSONArray(body).length()
                    if (size == 0) return@run
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
