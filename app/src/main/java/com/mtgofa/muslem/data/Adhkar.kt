package com.mtgofa.muslem.data

import android.content.Context
import org.json.JSONArray

data class Dhikr(val text: String, val repeat: Int)

data class AdhkarChapter(val id: Int, val title: String, val items: List<Dhikr>)

/** A chapter shown as a card on the adhkar home, with its own short name. */
data class Featured(val id: Int, val name: String)

/** The Hisn al-Muslim collection bundled in assets/adhkar.json (built by tools/build_adhkar.py). */
object Adhkar {
    @Volatile private var chapters: List<AdhkarChapter>? = null

    val featured = listOf(
        Featured(27, "أذكار الصباح والمساء"),
        Featured(25, "أذكار بعد الصلاة"),
        Featured(28, "أذكار النوم"),
        Featured(1, "أذكار الاستيقاظ"),
        Featured(15, "أذكار الأذان"),
        Featured(13, "دخول المسجد"),
        Featured(14, "الخروج من المسجد"),
        Featured(9, "بعد الوضوء"),
        Featured(10, "الخروج من المنزل"),
        Featured(11, "دخول المنزل"),
        Featured(69, "قبل الطعام"),
        Featured(70, "بعد الطعام"),
        Featured(96, "دعاء السفر"),
        Featured(129, "الاستغفار والتوبة"),
        Featured(34, "الهم والحزن"),
        Featured(130, "فضل التسبيح"),
    )

    init { Content.onChange { chapters = null } }

    fun all(@Suppress("UNUSED_PARAMETER") ctx: Context): List<AdhkarChapter> = chapters ?: synchronized(this) {
        chapters ?: load().also { chapters = it }
    }

    fun chapter(ctx: Context, id: Int) = all(ctx).firstOrNull { it.id == id }

    private fun load(): List<AdhkarChapter> {
        val arr = JSONArray(Content.read(Content.ADHKAR))
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            val items = o.getJSONArray("items")
            AdhkarChapter(
                o.getInt("id"),
                o.getString("title").trim(),
                List(items.length()) { k ->
                    val d = items.getJSONObject(k)
                    Dhikr(clean(d.getString("text")), d.optInt("repeat", 1).coerceAtLeast(1))
                },
            )
        }
    }

    /** The source wraps sayings in (( )); the reader shows plain text. */
    private fun clean(t: String) = t.replace("((", "").replace("))", "").replace(Regex("\\s+"), " ").trim()
}

/** A phrase for the tasbeeh counter, with the count of one round. */
data class TasbeehPhrase(val id: String, val text: String, val target: Int)

object Tasbeeh {
    @Volatile private var cache: List<TasbeehPhrase>? = null
    init { Content.onChange { cache = null } }

    val phrases: List<TasbeehPhrase>
        get() = cache ?: synchronized(this) {
            cache ?: run {
                val arr = JSONArray(Content.read(Content.TASBEEH))
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    TasbeehPhrase(o.getString("id"), o.getString("text"), o.optInt("target", 0))
                }
            }.also { cache = it }
        }

    fun byId(id: String) = phrases.firstOrNull { it.id == id } ?: phrases.first()
}
