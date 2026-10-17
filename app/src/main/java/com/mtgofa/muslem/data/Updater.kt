package com.mtgofa.muslem.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.mtgofa.muslem.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

data class AppRelease(val versionCode: Int, val tag: String, val url: String, val sha256: String?, val notes: String)

/**
 * Self-update from the public GitHub repo's latest release.
 *
 * Release contract: name contains "(code N)", one *.apk asset, and optionally a body line
 * "sha256: <hex>" which is verified before the installer opens.
 */
object Updater {
    sealed interface State {
        data object Idle : State
        data object Checking : State
        data object UpToDate : State
        data class Available(val rel: AppRelease) : State
        data class Downloading(val rel: AppRelease, val progress: Float) : State
        data class Failed(val reason: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state

    /** show the "new version" dialog (cleared by "later") */
    val prompt = MutableStateFlow(false)

    private val configured get() = BuildConfig.GITHUB_OWNER.isNotBlank() && BuildConfig.GITHUB_REPO.isNotBlank()

    suspend fun check(silent: Boolean) = withContext(Dispatchers.IO) {
        if (!configured || _state.value is State.Downloading) return@withContext
        if (!silent) _state.value = State.Checking
        val r = fetch()
        if (r is State.Available) prompt.value = true
        if (!silent || r is State.Available) _state.value = r
    }

    private fun fetch(): State = try {
        val req = Request.Builder()
            .url("https://api.github.com/repos/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "Muslem-Android")
            .build()
        Http.client.newCall(req).execute().use { resp ->
            when {
                resp.code == 404 -> State.UpToDate
                !resp.isSuccessful -> State.Failed("HTTP ${resp.code}")
                else -> parse(resp.body?.string().orEmpty())
            }
        }
    } catch (e: Exception) {
        State.Failed("تعذّر الاتصال")
    }

    private fun parse(json: String): State {
        val o = runCatching { JSONObject(json) }.getOrNull() ?: return State.Failed("رد غير صالح")
        val code = Regex("""code\s*(\d+)""", RegexOption.IGNORE_CASE).find(o.optString("name"))
            ?.groupValues?.get(1)?.toIntOrNull() ?: return State.UpToDate
        if (code <= BuildConfig.VERSION_CODE) return State.UpToDate
        val assets = o.optJSONArray("assets")
        var url = ""
        if (assets != null) for (i in 0 until assets.length()) {
            val u = assets.getJSONObject(i).optString("browser_download_url")
            if (u.endsWith(".apk")) { url = u; break }
        }
        if (url.isBlank()) return State.UpToDate
        val body = o.optString("body", "")
        val sha = Regex("""sha256[:=]\s*([0-9a-fA-F]{64})""").find(body)?.groupValues?.get(1)?.lowercase()
        val notes = body.lines().filterNot { it.contains("sha256", true) }.joinToString("\n").trim()
        return State.Available(AppRelease(code, o.optString("tag_name"), url, sha, notes))
    }

    suspend fun install(ctx: Context, rel: AppRelease) = withContext(Dispatchers.IO) {
        _state.value = State.Downloading(rel, 0f)
        val file = File(File(ctx.cacheDir, "updater"), "update.apk")
        val ok = Http.download(rel.url, file) { _state.value = State.Downloading(rel, it) }
        if (!ok) { _state.value = State.Failed("فشل تنزيل التحديث"); return@withContext }
        if (rel.sha256 != null && sha256(file) != rel.sha256) {
            file.delete(); _state.value = State.Failed("فشل التحقق من سلامة الملف"); return@withContext
        }
        _state.value = State.Available(rel)
        prompt.value = false
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", file)
        val i = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        withContext(Dispatchers.Main) { runCatching { ctx.startActivity(i) } }
    }

    private fun sha256(f: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { s -> val b = ByteArray(64 * 1024); while (true) { val n = s.read(b); if (n < 0) break; md.update(b, 0, n) } }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
