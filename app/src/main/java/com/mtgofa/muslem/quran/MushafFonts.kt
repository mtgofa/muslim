package com.mtgofa.muslem.quran

import android.content.Context
import android.graphics.Typeface
import java.io.File
import java.nio.ByteBuffer

/**
 * The page fonts are colour fonts (COLR v0 + CPAL) carrying several palettes: tajweed / plain,
 * each for a light and a dark page. Android always draws palette 0, so the palette we want is
 * made the default by rewriting the CPAL index table, and the result is cached on disk.
 */
enum class MushafStyle(val palette: Int) {
    LIGHT_TAJWEED(0), DARK_TAJWEED(1), LIGHT_PLAIN(3), DARK_PLAIN(4);

    companion object {
        fun of(tajweed: Boolean, night: Boolean) = when {
            tajweed && !night -> LIGHT_TAJWEED
            tajweed -> DARK_TAJWEED
            !night -> LIGHT_PLAIN
            else -> DARK_PLAIN
        }
    }
}

object MushafFonts {
    private val cache = object : LinkedHashMap<String, Typeface>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Typeface>?) = size > 12
    }

    @Synchronized
    fun page(ctx: Context, page: Int, style: MushafStyle): Typeface =
        load(ctx, "p%03d".format(page), style)

    /** Surah-title frames and the basmala. */
    @Synchronized
    fun header(ctx: Context, style: MushafStyle): Typeface = load(ctx, "header", style)

    private fun load(ctx: Context, name: String, style: MushafStyle): Typeface {
        val key = "$name/${style.name}"
        cache[key]?.let { return it }
        val out = File(ctx.cacheDir, "mushaf/v$CACHE_VERSION/${style.name}/$name.ttf")
        if (!out.exists() || out.length() == 0L) {
            val bytes = if (name == "header") ctx.assets.open("mushaf/header.ttf").use { it.readBytes() }
                else File(MushafPack.dir(ctx), "$name.ttf").readBytes()
            preparePalette(bytes, style.palette, hideMarkers = name != "header")
            out.parentFile?.mkdirs()
            val tmp = File(out.path + ".tmp")
            tmp.writeBytes(bytes)
            tmp.renameTo(out)
        }
        val tf = runCatching { Typeface.createFromFile(out) }.getOrElse {
            out.delete(); Typeface.DEFAULT
        }
        cache[key] = tf
        return tf
    }

    private const val CACHE_VERSION = 2

    /** Palette entry the fonts use for hollow layout boxes (tooling marks, never printed). */
    private const val MARKER_ENTRY = 14

    /**
     * Make palette [index] the default one (palette 0) in place, and make its marker-box
     * colour fully transparent.
     */
    private fun preparePalette(font: ByteArray, index: Int, hideMarkers: Boolean) {
        val buf = ByteBuffer.wrap(font)
        val numTables = buf.getShort(4).toInt() and 0xFFFF
        for (i in 0 until numTables) {
            val rec = 12 + 16 * i
            val tag = String(font, rec, 4, Charsets.US_ASCII)
            if (tag != "CPAL") continue
            val off = buf.getInt(rec + 8)
            val numEntries = buf.getShort(off + 2).toInt() and 0xFFFF
            val numPalettes = buf.getShort(off + 4).toInt() and 0xFFFF
            if (index >= numPalettes) return
            // header: version u16, numPaletteEntries u16, numPalettes u16, numColorRecords u16,
            // colorRecordsArrayOffset u32, then colorRecordIndices[numPalettes] (u16 each)
            val indices = off + 12
            val chosen = buf.getShort(indices + 2 * index).toInt() and 0xFFFF
            buf.putShort(indices, chosen.toShort())
            if (hideMarkers && MARKER_ENTRY < numEntries) {
                // colour records are BGRA; byte 3 is alpha
                val records = off + buf.getInt(off + 8)
                font[records + 4 * (chosen + MARKER_ENTRY) + 3] = 0
            }
            return
        }
    }
}
