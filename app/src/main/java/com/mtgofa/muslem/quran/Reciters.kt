package com.mtgofa.muslem.quran

import android.content.Context
import android.net.Uri
import com.mtgofa.muslem.data.Http
import java.io.File

/** A reciter with one audio file per ayah, so ranges and repeats are exact. */
data class Reciter(val id: String, val name: String, val style: String = "مرتّل")

object Reciters {
    private const val BASE = "https://everyayah.com/data/"

    @Volatile private var cache: List<Reciter>? = null
    init { com.mtgofa.muslem.data.Content.onChange { cache = null } }

    /** Every-ayah reciters, from content/reciters.json (updatable from GitHub). */
    val all: List<Reciter>
        get() = cache ?: synchronized(this) {
            cache ?: run {
                val arr = org.json.JSONArray(com.mtgofa.muslem.data.Content.read(com.mtgofa.muslem.data.Content.RECITERS))
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    Reciter(o.getString("id"), o.getString("name"), o.optString("style", "مرتّل"))
                }
            }.also { cache = it }
        }

    fun byId(id: String) = all.firstOrNull { it.id == id } ?: all.first()

    private fun name(sura: Int, ayah: Int) = "%03d%03d.mp3".format(sura, ayah)

    fun url(r: Reciter, sura: Int, ayah: Int) = BASE + r.id + "/" + name(sura, ayah)

    fun dir(ctx: Context, r: Reciter) = File(ctx.getExternalFilesDir(null) ?: ctx.filesDir, "audio/${r.id}")

    fun local(ctx: Context, r: Reciter, sura: Int, ayah: Int) = File(dir(ctx, r), name(sura, ayah))

    /** Local file when downloaded, otherwise the stream URL. */
    fun uri(ctx: Context, r: Reciter, sura: Int, ayah: Int): Uri {
        val f = local(ctx, r, sura, ayah)
        return if (f.length() > 0) Uri.fromFile(f) else Uri.parse(url(r, sura, ayah))
    }

    fun isSurahDownloaded(ctx: Context, r: Reciter, sura: Int): Boolean {
        val n = QuranDb.surah(sura).ayahs
        return (1..n).all { local(ctx, r, sura, it).length() > 0 }
    }

    /** Download every ayah of a surah (plus the basmala clip). */
    fun downloadSurah(ctx: Context, r: Reciter, sura: Int, progress: (Float) -> Unit): Boolean {
        val n = QuranDb.surah(sura).ayahs
        val list = buildList { add(1 to 1); for (a in 1..n) add(sura to a) }.distinct()
        list.forEachIndexed { i, (s, a) ->
            val f = local(ctx, r, s, a)
            if (f.length() == 0L && !Http.download(url(r, s, a), f)) return false
            progress((i + 1f) / list.size)
        }
        return true
    }

    fun downloadedSize(ctx: Context, r: Reciter): Long =
        dir(ctx, r).listFiles()?.sumOf { it.length() } ?: 0L

    fun deleteAll(ctx: Context, r: Reciter) { dir(ctx, r).deleteRecursively() }
}
