package com.mtgofa.muslem.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The adhkar texts mix three kinds of words: the dhikr itself (quoted ((…)) or (…)), the
 * instructions around it ("يبدأ برجله اليمنى، ويقول:"), and Quran (﴿…﴾), plus notes such as
 * "(ثلاث مرات)". They are split here so each gets its own look: the dhikr in full ink, the
 * instructions muted, the Quran in its own framed block, and the notes as small tags.
 */
sealed interface DhikrBlock {
    /** dhikr and instructions, as one paragraph */
    data class Words(val runs: List<Run>) : DhikrBlock
    /** Quran, with the basmala above it when the text gives one */
    data class Quran(val text: String, val basmala: Boolean) : DhikrBlock
}

enum class RunKind { DHIKR, NARRATION, OPTIONAL, QURAN }

data class Run(val kind: RunKind, val text: String)

class ParsedDhikr(val blocks: List<DhikrBlock>, val notes: List<String>)

private val HARAKAT = Regex("[ً-ٰٟـ]")

/** letters only: no harakat, one form of alef / ta marbuta / ya, so wording can be compared */
private fun plain(s: String) = HARAKAT.replace(s, "")
    .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا').replace('ة', 'ه').replace('ى', 'ي')

private val COUNT_WORDS = setOf(
    "ثلاث", "ثلاثا", "سبع", "عشر", "مايه", "مائه", "اربع", "اربعا", "خمس", "مره", "مرات", "مرتين", "واحده",
    "ثلاثا", "وثلاثين", "ثلاثين", "اربعا", "يفعل", "ذلك",
)
private val NOTE_HINTS = listOf("مره", "مرات", "مرتين", "ثلاثا", "اذا اصبح", "اذا امسي", "عند ", "في اليوم", "بعد صلاه", "يفعل ذلك")

private fun isNote(inner: String): Boolean {
    val p = plain(inner).trim()
    return p.length <= 45 && NOTE_HINTS.any { p.contains(it) }
}

/** "(يبدأ برجله اليمنى)", "(يقرأ …)": what to do, not what to say */
private fun isInstruction(inner: String): Boolean {
    val p = plain(inner).trim()
    return listOf("يبدا", "يقرا", "يتحول", "يقوم", "يقول", "لا يحدث").any { p.startsWith(it) || p.startsWith("و$it") }
}

/** a note that only repeats the count the counter already shows */
private fun countOnly(note: String) =
    plain(note).split(Regex("[\\s،.,]+")).filter { it.isNotBlank() }.all { it in COUNT_WORDS }

private val BASMALA = plain("بسم الله الرحمن الرحيم")

fun parseDhikr(raw: String): ParsedDhikr {
    val runs = ArrayList<Run>()
    val notes = ArrayList<String>()
    val s = raw.trim()
    val buf = StringBuilder()
    var kind = RunKind.NARRATION
    fun flush() {
        if (buf.isNotEmpty()) runs += Run(kind, buf.toString())
        buf.clear()
    }
    var i = 0
    while (i < s.length) {
        val c = s[i]
        when {
            s.startsWith("((", i) && kind == RunKind.NARRATION -> { flush(); kind = RunKind.DHIKR; i += 2; continue }
            s.startsWith("))", i) && kind == RunKind.DHIKR -> { flush(); kind = RunKind.NARRATION; i += 2; continue }
            c == '﴿' -> { val k = kind; flush(); val end = s.indexOf('﴾', i).takeIf { it > 0 } ?: s.length
                runs += Run(RunKind.QURAN, s.substring(i + 1, end)); kind = k; i = end + 1; continue }
            c == '[' -> { val k = kind; flush(); val end = s.indexOf(']', i).takeIf { it > 0 } ?: s.length
                runs += Run(RunKind.OPTIONAL, s.substring(i + 1, end)); kind = k; i = end + 1; continue }
            // "أستغفر الله (ثلاثاً) اللهم…": a count inside the dhikr stays in place, as a muted word
            c == '(' && kind == RunKind.DHIKR && s.indexOf(')', i).let { e -> e > 0 && isNote(s.substring(i + 1, e)) } -> {
                val e = s.indexOf(')', i)
                flush(); runs += Run(RunKind.NARRATION, s.substring(i + 1, e)); kind = RunKind.DHIKR
                i = e + 1; continue
            }
            c == '(' && kind == RunKind.NARRATION -> {
                // a single-bracket group: a note like (ثلاث مرات), or the dhikr itself
                var depth = 0; var j = i
                while (j < s.length) { if (s[j] == '(') depth++ else if (s[j] == ')') { depth--; if (depth == 0) break }; j++ }
                val inner = s.substring(i + 1, minOf(j, s.length))
                flush()
                when {
                    isNote(inner) -> notes += inner.trim()
                    isInstruction(inner) -> runs += Run(RunKind.NARRATION, inner)
                    else -> runs += Run(RunKind.DHIKR, inner)
                }
                i = j + 1; continue
            }
        }
        buf.append(c); i++
    }
    flush()

    // a trailing count written without brackets: "… )). ثلاث مرات." → a note (a count in the
    // middle stays where it is)
    val lastWords = runs.indexOfLast { r -> r.text.any { it.isLetter() } }
    var or = false
    val cleaned = runs.mapIndexed { idx, r ->
        when {
            idx == lastWords && r.kind == RunKind.NARRATION && isNote(r.text) && countOnly(r.text.trim(' ', '.', '،')) -> {
                notes += r.text.trim(' ', '.', '،'); r.copy(text = "")
            }
            // "…قدير، أو (مرة واحدة عند الكسل)": the "or" belongs with the note
            r.kind == RunKind.NARRATION && plain(r.text).trim(' ', '.', '،') == "او" -> { or = true; r.copy(text = "") }
            else -> r
        }
    }.filter { it.text.isNotBlank() && it.text.any { ch -> ch.isLetter() } }

    // text with no quoting at all is all dhikr
    val runs2 = if (cleaned.none { it.kind == RunKind.DHIKR || it.kind == RunKind.QURAN })
        cleaned.map { if (it.kind == RunKind.NARRATION) it.copy(kind = RunKind.DHIKR) else it } else cleaned

    val blocks = ArrayList<DhikrBlock>()
    val words = ArrayList<Run>()
    var basmala = false
    for (r in runs2) {
        if (r.kind == RunKind.QURAN && r.text.length > 24) {
            if (words.isNotEmpty()) { blocks += DhikrBlock.Words(words.toList()); words.clear() }
            blocks += DhikrBlock.Quran(r.text.trim().replace(Regex("\\s*\\*\\s*"), " ۝ ").trim(' ', '۝'), basmala)
            basmala = false
        } else if (r.kind != RunKind.QURAN && plain(r.text).trim(' ', '.', '،', ':') == BASMALA) {
            if (words.isNotEmpty()) { blocks += DhikrBlock.Words(words.toList()); words.clear() }
            basmala = true
        } else words += r
    }
    if (words.isNotEmpty()) blocks += DhikrBlock.Words(words.toList())
    val kept = notes.filterNot { countOnly(it) }.map { it.trim(' ', '.', '،') }
    return ParsedDhikr(blocks, if (or && kept.isNotEmpty()) kept.dropLast(1) + "أو ${kept.last()}" else kept)
}

private val QuranInk = Color(0xFF173B30)

/** The dhikr's text: words, Quran blocks and condition notes, each styled for its role. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DhikrBody(text: String, finished: Boolean) {
    val parsed = remember(text) { parseDhikr(text) }
    val ink = if (finished) Palette.Muted else Palette.Ink
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        parsed.blocks.forEach { b ->
            when (b) {
                is DhikrBlock.Words -> Text(
                    words(b.runs, ink), fontFamily = Plex, fontSize = 20.sp, lineHeight = 38.sp,
                    textAlign = TextAlign.Justify, modifier = Modifier.fillMaxWidth(),
                )
                is DhikrBlock.Quran -> Column(
                    Modifier.fillMaxWidth()
                        .background(if (finished) Color.Transparent else Palette.Paper, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    if (b.basmala) Text(
                        "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ", fontFamily = FontFamily.Serif, fontSize = 17.sp,
                        color = if (finished) Palette.Muted else Palette.Emerald, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    )
                    Text(
                        quran(b.text, finished), fontFamily = FontFamily.Serif, fontSize = 22.sp, lineHeight = 44.sp,
                        color = if (finished) Palette.Muted else QuranInk, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        if (parsed.notes.isNotEmpty()) androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            parsed.notes.forEach { n ->
                Text(
                    n, fontFamily = Plex, fontSize = 13.sp, color = Color(0xFF8A5E14),
                    modifier = Modifier.background(Palette.GoldSoft, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
        }
    }
}

private fun words(runs: List<Run>, ink: Color): AnnotatedString = buildAnnotatedString {
    runs.forEachIndexed { i, r ->
        val t = r.text.trim().let { if (i > 0) " $it" else it }
        when (r.kind) {
            RunKind.DHIKR -> withStyle(SpanStyle(color = ink)) { append(t) }
            RunKind.NARRATION -> withStyle(SpanStyle(color = Palette.Muted, fontSize = 16.sp)) { append(t) }
            RunKind.OPTIONAL -> withStyle(SpanStyle(color = Palette.Muted)) { append(t) }
            // a few words of Quran inside the text, e.g. "يقرأ ﴿الم﴾ تنزيل السجدة"
            RunKind.QURAN -> withStyle(SpanStyle(color = Palette.Emerald, fontWeight = FontWeight.SemiBold)) { append(" ﴿${r.text.trim()}﴾") }
        }
    }
}

private val Gold = Color(0xFFB8893A)

private fun quran(text: String, finished: Boolean): AnnotatedString = buildAnnotatedString {
    val mark = if (finished) Palette.Muted else Gold
    withStyle(SpanStyle(color = mark)) { append("﴿ ") }
    // the ayah-end sign between ayat in gold
    text.split(" ۝ ").forEachIndexed { i, part ->
        if (i > 0) withStyle(SpanStyle(color = mark, fontSize = 15.sp)) { append(" ۝ ") }
        append(part.trim())
    }
    withStyle(SpanStyle(color = mark)) { append(" ﴾") }
}
