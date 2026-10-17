package com.mtgofa.muslem.prayer

import android.content.Context
import android.net.Uri
import com.mtgofa.muslem.R
import com.mtgofa.muslem.data.Http
import java.io.File

/** One adhan voice: bundled in the APK ([raw]) or downloaded once from [url]. */
data class Muezzin(val id: String, val name: String, val raw: Int = 0, val url: String = "") {
    val bundled get() = raw != 0
}

object Muezzins {
    /** voices shipped inside the APK, by id */
    private val BUNDLED = mapOf("abdulbasit" to R.raw.adhan_abdulbasit, "makkah" to R.raw.adhan_makkah)

    @Volatile private var cache: List<Muezzin>? = null
    init { com.mtgofa.muslem.data.Content.onChange { cache = null } }

    /** From content/muezzins.json (updatable from GitHub); bundled voices keep their raw file. */
    val all: List<Muezzin>
        get() = cache ?: synchronized(this) {
            cache ?: run {
                val arr = org.json.JSONArray(com.mtgofa.muslem.data.Content.read(com.mtgofa.muslem.data.Content.MUEZZINS))
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    val id = o.getString("id")
                    Muezzin(id, o.getString("name"), BUNDLED[id] ?: 0, o.optString("url", ""))
                }
            }.also { cache = it }
        }

    fun byId(id: String) = all.firstOrNull { it.id == id } ?: all.first()

    private fun localFile(ctx: Context, m: Muezzin) = File(File(ctx.filesDir, "adhan"), "${m.id}.mp3")

    fun isAvailable(ctx: Context, m: Muezzin) = m.bundled || localFile(ctx, m).length() > 50_000

    /** A playable Uri, falling back to the first bundled voice if this one is not downloaded. */
    fun uri(ctx: Context, m: Muezzin): Uri = when {
        m.bundled -> Uri.parse("android.resource://${ctx.packageName}/${m.raw}")
        isAvailable(ctx, m) -> Uri.fromFile(localFile(ctx, m))
        else -> uri(ctx, all.first())
    }

    /** Blocking download; returns true on success. [progress] gets 0..1. */
    fun download(ctx: Context, m: Muezzin, progress: (Float) -> Unit = {}): Boolean {
        if (m.bundled) return true
        val out = localFile(ctx, m)
        return m.url.isNotBlank() && Http.download(m.url, out, progress)
    }

    fun delete(ctx: Context, m: Muezzin) { if (!m.bundled) localFile(ctx, m).delete() }
}
