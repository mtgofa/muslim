package com.mtgofa.muslem.quran

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File

data class Surah(val id: Int, val name: String, val nameEn: String, val ayahs: Int, val mecca: Boolean, val page: Int)

/** Glyph types in the page layout. */
object GlyphType { const val WORD = 1; const val END = 2; const val HEADER = 6; const val BASMALA = 8 }

data class Glyph(val type: Int, val code: String, val sura: Int, val ayah: Int)

data class MushafLine(val number: Int, val type: Int, val glyphs: List<Glyph>)

data class PageData(val page: Int, val lines: List<MushafLine>) {
    /** surahs that appear on the page (first one first) */
    val suras: List<Int> get() = lines.flatMap { l -> l.glyphs.map { it.sura } }.filter { it > 0 }.distinct()
    val firstAyah: Pair<Int, Int>?
        get() = lines.firstNotNullOfOrNull { l -> l.glyphs.firstOrNull { it.type == GlyphType.WORD || it.type == GlyphType.END } }
            ?.let { it.sura to it.ayah }
}

data class AyahRef(val sura: Int, val ayah: Int)

data class SearchHit(val sura: Int, val ayah: Int, val text: String, val page: Int)

data class Mark(val id: Int, val sura: Int, val ayah: Int, val page: Int)

/** Read-only access to the bundled Quran database (copied out of the APK on first use). */
object QuranDb {
    private const val DB_VERSION = 1
    private lateinit var db: SQLiteDatabase
    lateinit var surahs: List<Surah>; private set
    lateinit var juz: List<Mark>; private set
    lateinit var quarters: List<Mark>; private set

    @Synchronized
    fun init(ctx: Context) {
        if (::db.isInitialized) return
        val file = ctx.getDatabasePath("quran.db")
        val stamp = File(file.parentFile, "quran.version")
        if (!file.exists() || stamp.takeIf { it.exists() }?.readText()?.trim() != "$DB_VERSION") {
            file.parentFile?.mkdirs()
            ctx.assets.open("quran.db").use { i -> file.outputStream().use { o -> i.copyTo(o) } }
            stamp.writeText("$DB_VERSION")
        }
        db = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
        surahs = db.rawQuery("SELECT id,name,name_en,ayahs,mecca,page FROM surah ORDER BY id", null).use { c ->
            buildList { while (c.moveToNext()) add(Surah(c.getInt(0), c.getString(1), c.getString(2), c.getInt(3), c.getInt(4) == 1, c.getInt(5))) }
        }
        juz = marks("juz")
        quarters = marks("quarter")
    }

    private fun marks(table: String) = db.rawQuery("SELECT id,sura,ayah,page FROM $table ORDER BY id", null).use { c ->
        buildList { while (c.moveToNext()) add(Mark(c.getInt(0), c.getInt(1), c.getInt(2), c.getInt(3))) }
    }

    fun surah(id: Int) = surahs[(id - 1).coerceIn(0, 113)]

    private val pageCache = object : LinkedHashMap<Int, PageData>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, PageData>?) = size > 24
    }

    @Synchronized
    fun page(page: Int): PageData = pageCache.getOrPut(page) {
        val rows = db.rawQuery("SELECT line,type,code,sura,ayah FROM glyph WHERE page=? ORDER BY id", arrayOf("$page")).use { c ->
            buildList { while (c.moveToNext()) add(c.getInt(0) to Glyph(c.getInt(1), c.getString(2) ?: "", c.getInt(3), c.getInt(4))) }
        }
        val lines = rows.groupBy({ it.first }, { it.second }).map { (n, gs) ->
            MushafLine(n, gs.first().type.let { if (it == GlyphType.END) GlyphType.WORD else it }, gs)
        }.sortedBy { it.number }
        PageData(page, lines)
    }

    fun pageOf(sura: Int, ayah: Int): Int =
        db.rawQuery("SELECT page FROM ayah WHERE sura=? AND ayah=?", arrayOf("$sura", "$ayah")).use { c ->
            if (c.moveToFirst()) c.getInt(0) else 1
        }

    fun juzOfPage(page: Int): Int = juz.lastOrNull { it.page <= page }?.id ?: 1

    /** hizb quarter number (1..240) that is current at the start of [page] */
    fun quarterOfPage(page: Int): Int = quarters.lastOrNull { it.page <= page }?.id ?: 1

    fun ayahText(sura: Int, ayah: Int): String =
        db.rawQuery("SELECT text FROM ayah WHERE sura=? AND ayah=?", arrayOf("$sura", "$ayah")).use { c ->
            if (c.moveToFirst()) c.getString(0) else ""
        }

    /** Text of every ayah of a surah, in order. */
    fun ayahTexts(sura: Int): List<String> =
        db.rawQuery("SELECT text FROM ayah WHERE sura=? ORDER BY ayah", arrayOf("$sura")).use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0)) }
        }

    /** Ayahs of a page in reading order. */
    fun ayahsOnPage(page: Int): List<AyahRef> = page(page).lines.flatMap { it.glyphs }
        .filter { it.type == GlyphType.END }.map { AyahRef(it.sura, it.ayah) }

    fun search(query: String, limit: Int = 200): List<SearchHit> {
        val q = normalize(query).trim()
        if (q.length < 2) return emptyList()
        return db.rawQuery(
            "SELECT sura,ayah,text,page FROM ayah WHERE plain LIKE ? ORDER BY sura,ayah LIMIT $limit",
            arrayOf("%$q%"),
        ).use { c -> buildList { while (c.moveToNext()) add(SearchHit(c.getInt(0), c.getInt(1), c.getString(2), c.getInt(3))) } }
    }

    private val diacritics = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]")
    fun normalize(t: String): String = diacritics.replace(t, "")
        .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا').replace('ٱ', 'ا')
        .replace('ى', 'ي').replace('ة', 'ه').replace('ؤ', 'و').replace('ئ', 'ي')
}
