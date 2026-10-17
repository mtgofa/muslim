package com.mtgofa.muslem.data

import org.json.JSONObject

/** One difficult word and its meaning (غريب القرآن). */
data class WordMeaning(val word: String, val meaning: String)

/** Word meanings per ayah, from content/meanings.json (updatable from GitHub). */
object Meanings {
    @Volatile private var cache: Map<String, List<WordMeaning>>? = null
    init { Content.onChange { cache = null } }

    private fun all(): Map<String, List<WordMeaning>> = cache ?: synchronized(this) {
        cache ?: run {
            val o = JSONObject(Content.read(Content.MEANINGS))
            buildMap {
                for (key in o.keys()) {
                    val arr = o.getJSONArray(key)
                    put(key, List(arr.length()) { i -> arr.getJSONArray(i).let { WordMeaning(it.getString(0), it.getString(1)) } })
                }
            }
        }.also { cache = it }
    }

    fun of(sura: Int, ayah: Int): List<WordMeaning> = all()["$sura:$ayah"].orEmpty()

    /**
     * The meaning that explains word number [index] of the ayah, matched on the letters (the
     * books spell some words differently from the Mushaf, so marks and letter forms are ignored).
     */
    fun forWord(sura: Int, ayah: Int, index: Int): WordMeaning? {
        val list = of(sura, ayah).ifEmpty { return null }
        val tokens = com.mtgofa.muslem.quran.QuranDb.ayahText(sura, ayah).split(' ')
            .filter { t -> t.any { it in '\u0621'..'\u064A' } }
        val word = tokens.getOrNull(index)?.let(::key) ?: return null
        return list.firstOrNull { m -> m.word.split(' ').any { key(it) == word } }
    }

    private fun key(t: String) = com.mtgofa.muslem.quran.QuranDb.normalize(t).filter { it in '\u0621'..'\u064A' }
        .replace("ا", "")
}
