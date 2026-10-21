package com.mtgofa.muslim.quran

import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import android.content.Context
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

/** One glyph placed on the page. [x] is the left of the run, [rect] its tap/highlight box. */
/** One glyph placed on the page; [wordIndex] is its position in the ayah (-1 for ayah marks). */
class Placed(val g: Glyph, val x: Float, val baseline: Float, val rect: RectF, val wordIndex: Int = -1)

class Frame(val rect: RectF, val title: String, val titleX: Float, val baseline: Float)

class PageLayout(
    val page: Int,
    val textSize: Float,
    val headerSize: Float,
    val pageFont: Typeface,
    val headerFont: Typeface,
    val words: List<Placed>,
    val frames: List<Frame>,
    /** basmala runs: text (logical order, drawn right-to-left), x, baseline */
    val basmalas: List<Triple<String, Float, Float>>,
    val basmalaSize: Float,
    /** drawn height: the box height, or more when the text is enlarged and reflowed */
    val height: Float,
)

/** Text sizes the reader can choose, as a factor of the printed page's size. */
val MUSHAF_ZOOM = floatArrayOf(1f, 1.25f, 1.5f, 1.8f)

private const val LINES = 15
/** Surah name and basmala sizes, as a fraction of the line height. */
private const val TITLE_SIZE = 0.38f
/** Extra stroke weight for the surah names, as a fraction of their size. */
private const val TITLE_WEIGHT = 0.008f
private val TITLE_SPREAD = listOf(-1f to -1f, 1f to -1f, -1f to 1f, 1f to 1f, 0f to 0f)

/** Pixel offsets of the passes that give the page text its printed weight. */
private val WEIGHT_SPREAD = floatArrayOf(0f, 0f, 0.45f, 0f, 0f, 0.45f)
/** Landscape only: the words move down by this fraction of a line, to free a stray mark on page 42. */
private const val LANDSCAPE_NUDGE = 0.015f

/** Basmala width as a fraction of the page width. */
private const val BASMALA_WIDTH = 0.62f
/** The long-form basmala in the title font, whose colour layers carry the tajweed. */
private const val BASMALA_WORDS = "\uFE9D\uFEA1\uFEA5\uFEA9"

/**
 * Measure and place every glyph of [page] into a [w] x [h] pixel box. With [zoom] above 1 the
 * words are larger than the printed line allows, so they are reflowed: the page's words, in
 * order, are broken into new justified lines and the page grows taller (it then scrolls).
 */
fun layoutPage(ctx: Context, data: PageData, w: Float, h: Float, style: MushafStyle, zoom: Float = 1f, headerScale: Float = 1f, headerGap: Float = 0f, nudge: Float = 0f): PageLayout {
    val pageFont = MushafFonts.page(ctx, data.page, style)
    val headerFont = MushafFonts.header(ctx, style)
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = pageFont; textSize = 100f }

    fun adv(s: String, paint: Paint) = paint.getRunAdvance(s, 0, s.length, 0, s.length, true, s.length)

    val wordLines = data.lines.filter { it.type == GlyphType.WORD }
    val natural = wordLines.associate { l -> l.number to l.glyphs.map { adv(it.code, p) } }
    val widest = natural.values.maxOfOrNull { it.sum() + (it.size - 1) * 4f } ?: 1f

    val special = data.page <= 2
    val lineH = h / LINES
    var size = 100f * w / widest
    size = min(size, lineH * (if (special) 0.95f else 1.3f))
    if (special) size *= 0.86f
    if (zoom > 1f) return reflow(ctx, data, w, lineH, size * zoom, zoom, special, pageFont, headerFont, natural)
    p.textSize = size
    val fm = p.fontMetrics
    val scale = size / 100f

    // the two opening pages are a short, centred block in the middle of the page
    val usedLines = data.lines.maxOfOrNull { it.number } ?: LINES
    val top = if (special) (h - usedLines * lineH * 1.08f) / 2f else 0f
    val lh = if (special) lineH * 1.08f else lineH
    // rows top to bottom; a surah band may take a taller row (shared cards only)
    val rowY = HashMap<Int, Float>()
    run {
        var acc = top
        for (l in data.lines) { rowY[l.number] = acc; acc += if (l.type == GlyphType.HEADER) lh * (headerScale + headerGap) else lh }
    }

    val words = ArrayList<Placed>()
    val frames = ArrayList<Frame>()
    val basmalas = ArrayList<Triple<String, Float, Float>>()
    val hp = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = headerFont }
    val bp = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = headerFont; textSize = 100f }
    val basmalaSize = 100f * (w * BASMALA_WIDTH) / adv(BASMALA_WORDS, bp)
    bp.textSize = basmalaSize

    // word positions inside each ayah; an ayah begun on an earlier page continues its count
    val wordCount = HashMap<Pair<Int, Int>, Int>()
    data.firstAyah?.let { (s, a) -> wordCount[s to a] = QuranDb.wordsBefore(data.page, s, a) }

    for (line in data.lines) {
        val y0 = rowY[line.number] ?: (top + (line.number - 1) * lh)
        val rh = if (line.type == GlyphType.HEADER) lh * headerScale else lh
        val cy = y0 + rh / 2f
        when (line.type) {
            GlyphType.HEADER -> {
                hp.textSize = rh * TITLE_SIZE
                val code = line.glyphs.first().code
                // centre on the glyph's drawn outline: the font metrics include room for marks
                // the title never uses, which pushed it low in the cartouche
                val ink = android.graphics.Rect().also { hp.getTextBounds(code, 0, code.length, it) }
                val rect = RectF(w * 0.01f, y0 + rh * 0.06f, w * 0.99f, y0 + rh * 0.94f)
                frames += Frame(rect, code, w / 2f - ink.exactCenterX(), rect.centerY() - ink.exactCenterY())
            }
            GlyphType.BASMALA -> {
                val ink = android.graphics.Rect().also { bp.getTextBounds(BASMALA_WORDS, 0, BASMALA_WORDS.length, it) }
                val bw = adv(BASMALA_WORDS, bp)
                basmalas += Triple(BASMALA_WORDS, (w - bw) / 2f, cy - ink.exactCenterY())
            }
            else -> {
                val adv100 = natural[line.number] ?: continue
                val ws = adv100.map { it * scale }
                val total = ws.sum()
                val n = ws.size
                val justify = !special && total > w * 0.72f && n > 1
                val gap = if (justify) (w - total) / (n - 1) else size * 0.14f
                val lineW = total + gap * (n - 1)
                var x = if (justify) w else (w + lineW) / 2f
                // nudge: a fraction of the line height, pushing the words down (landscape only)
                val baseline = cy - (fm.ascent + fm.descent) / 2f + nudge * lh
                line.glyphs.forEachIndexed { i, g ->
                    val gw = ws[i]
                    x -= gw
                    val idx = if (g.type == GlyphType.WORD) {
                        val key = g.sura to g.ayah
                        val n = wordCount[key] ?: 0
                        wordCount[key] = n + 1
                        n
                    } else -1
                    words += Placed(g, x, baseline, RectF(x - gap / 2f, y0 + lh * 0.04f, x + gw + gap / 2f, y0 + lh * 0.98f), idx)
                    x -= gap
                }
            }
        }
    }
    return PageLayout(data.page, size, lh * headerScale * TITLE_SIZE, pageFont, headerFont, words, frames, basmalas, basmalaSize, h)
}

/** The enlarged page: same words and order, new line breaks; surah bands and basmala as printed. */
private fun reflow(
    ctx: Context, data: PageData, w: Float, baseLineH: Float, size: Float, zoom: Float, special: Boolean,
    pageFont: Typeface, headerFont: Typeface, natural: Map<Int, List<Float>>,
): PageLayout {
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = pageFont; textSize = size }
    val fm = p.fontMetrics
    val scale = size / 100f
    val lh = baseLineH * zoom
    val minGap = size * 0.14f
    // a little room at the sides: the larger strokes reach past their advance at the line ends
    val inset = size * 0.08f
    val words = ArrayList<Placed>()
    val frames = ArrayList<Frame>()
    val basmalas = ArrayList<Triple<String, Float, Float>>()
    val hp = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = headerFont; textSize = baseLineH * TITLE_SIZE }
    val bp = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = headerFont; textSize = 100f }
    val basmalaSize = 100f * (w * BASMALA_WIDTH) / bp.getRunAdvance(BASMALA_WORDS, 0, BASMALA_WORDS.length, 0, BASMALA_WORDS.length, true, BASMALA_WORDS.length)
    bp.textSize = basmalaSize

    val wordCount = HashMap<Pair<Int, Int>, Int>()
    data.firstAyah?.let { (s, a) -> wordCount[s to a] = QuranDb.wordsBefore(data.page, s, a) }
    var y = 0f
    val para = ArrayList<Pair<Glyph, Float>>()

    /** break the words gathered since the last band into full-width lines */
    fun flush() {
        var i = 0
        while (i < para.size) {
            var j = i; var used = 0f
            while (j < para.size && (j == i || used + minGap + para[j].second <= w - 2 * inset)) { used += (if (j > i) minGap else 0f) + para[j].second; j++ }
            val line = para.subList(i, j)
            val total = line.sumOf { it.second.toDouble() }.toFloat()
            val last = j == para.size
            val justify = !special && !last && line.size > 1
            val gap = if (justify) (w - 2 * inset - total) / (line.size - 1) else minGap
            val lineW = total + gap * (line.size - 1)
            var x = if (special) (w + lineW) / 2f else w - inset
            val baseline = y + lh / 2f - (fm.ascent + fm.descent) / 2f
            for ((g, gw) in line) {
                x -= gw
                val idx = if (g.type == GlyphType.WORD) {
                    val key = g.sura to g.ayah
                    val n = wordCount[key] ?: 0
                    wordCount[key] = n + 1
                    n
                } else -1
                words += Placed(g, x, baseline, RectF(x - gap / 2f, y + lh * 0.04f, x + gw + gap / 2f, y + lh * 0.98f), idx)
                x -= gap
            }
            y += lh
            i = j
        }
        para.clear()
    }

    for (line in data.lines) {
        when (line.type) {
            GlyphType.HEADER -> {
                flush()
                val code = line.glyphs.first().code
                val ink = android.graphics.Rect().also { hp.getTextBounds(code, 0, code.length, it) }
                val rect = RectF(w * 0.01f, y + baseLineH * 0.06f, w * 0.99f, y + baseLineH * 0.94f)
                frames += Frame(rect, code, w / 2f - ink.exactCenterX(), rect.centerY() - ink.exactCenterY())
                y += baseLineH
            }
            GlyphType.BASMALA -> {
                flush()
                val ink = android.graphics.Rect().also { bp.getTextBounds(BASMALA_WORDS, 0, BASMALA_WORDS.length, it) }
                val bw = bp.getRunAdvance(BASMALA_WORDS, 0, BASMALA_WORDS.length, 0, BASMALA_WORDS.length, true, BASMALA_WORDS.length)
                basmalas += Triple(BASMALA_WORDS, (w - bw) / 2f, y + baseLineH / 2f - ink.exactCenterY())
                y += baseLineH
            }
            else -> {
                val adv100 = natural[line.number] ?: continue
                line.glyphs.forEachIndexed { i, g -> para += g to adv100[i] * scale }
            }
        }
    }
    flush()
    return PageLayout(data.page, size, baseLineH * TITLE_SIZE, pageFont, headerFont, words, frames, basmalas, basmalaSize, y + baseLineH * 0.3f)
}

/**
 * A single Mushaf page drawn with its own font. [highlight] tints one ayah (playing or
 * selected); taps report the ayah under the finger.
 */
@Composable
fun MushafPageView(
    page: Int,
    style: MushafStyle,
    highlight: AyahRef?,
    modifier: Modifier = Modifier,
    onTap: (AyahRef?) -> Unit = {},
    onLongPress: (AyahRef) -> Unit = {},
    /** meanings mode: words that have a meaning are marked, and tapping one reports it */
    wordMode: Boolean = false,
    onWord: (com.mtgofa.muslim.data.WordMeaning, AyahRef) -> Unit = { _, _ -> },
    /** text size factor, one of [MUSHAF_ZOOM] */
    zoom: Float = 1f,
    /** a two-finger pinch: +1 to enlarge one step, -1 to shrink */
    onPinch: (Int) -> Unit = {},
) {
    val ctx = LocalContext.current
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier.pointerInput(Unit) {
            // two fingers change the text size; one finger is left alone for turning pages
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                var factor = 1f
                do {
                    val e = awaitPointerEvent(PointerEventPass.Initial)
                    if (e.changes.count { it.pressed } >= 2) {
                        factor *= e.calculateZoom()
                        e.changes.forEach { it.consume() }
                    }
                } while (e.changes.any { it.pressed })
                if (factor > 1.15f) onPinch(1) else if (factor < 0.87f) onPinch(-1)
            }
        },
    ) {
        val w = with(density) { maxWidth.toPx() }
        val h = with(density) { maxHeight.toPx() }
        // sideways the page spans the full width in its printed lines and scrolls, rather than
        // its text shrinking to fit the short height. The text is already large there, so the
        // pinch size applies upright only.
        val landscape = w > h
        val pw = w
        val pageH = if (landscape) pw * 1.55f else h
        val z = if (landscape) 1f else zoom
        var layout by remember(page, style, pw, pageH, z) { mutableStateOf<PageLayout?>(null) }
        LaunchedEffect(page, style, pw, pageH, z) {
            layout = withContext(Dispatchers.Default) { layoutPage(ctx, QuranDb.page(page), pw, pageH, style, z, nudge = if (landscape) LANDSCAPE_NUDGE else 0f) }
        }
        val ink = Color(0xFF1A1A1A)
        val hl = Color(0x2A0F7A62)
        val lay = layout ?: return@BoxWithConstraints
        val explained = remember(lay, wordMode) {
            if (!wordMode) emptyMap() else lay.words.filter { it.wordIndex >= 0 }
                .mapNotNull { pl -> com.mtgofa.muslim.data.Meanings.forWord(pl.g.sura, pl.g.ayah, pl.wordIndex)?.let { pl to it } }
                .toMap()
        }

        val scroll = rememberScrollState()
        val tall = lay.height > h + 1f
        // keep the recited / selected ayah in view on a page taller than the screen
        LaunchedEffect(highlight, lay) {
            if (!tall || highlight == null) return@LaunchedEffect
            val rows = lay.words.filter { it.g.sura == highlight.sura && it.g.ayah == highlight.ayah }
            if (rows.isEmpty()) return@LaunchedEffect
            val top = rows.minOf { it.rect.top }; val bottom = rows.maxOf { it.rect.bottom }
            if (top < scroll.value || bottom > scroll.value + h) scroll.animateScrollTo((top - h * 0.2f).toInt().coerceAtLeast(0))
        }
        Box(Modifier.fillMaxWidth().then(if (tall) Modifier.verticalScroll(scroll) else Modifier)) {
        Canvas(
            Modifier
                .align(Alignment.TopCenter)
                .width(with(density) { pw.toDp() })
                .height(with(density) { (if (tall) lay.height else h).toDp() })
                .pointerInput(lay, explained) {
                    fun hit(x: Float, y: Float) = lay.words.firstOrNull { it.rect.contains(x, y) }?.g
                        ?.let { AyahRef(it.sura, it.ayah) }
                    detectTapGestures(
                        onTap = { o ->
                            val pl = lay.words.firstOrNull { it.rect.contains(o.x, o.y) }
                            val m = pl?.let { explained[it] }
                            if (m != null) onWord(m, AyahRef(pl.g.sura, pl.g.ayah)) else onTap(hit(o.x, o.y))
                        },
                        onLongPress = { o -> hit(o.x, o.y)?.let(onLongPress) },
                    )
                },
        ) {
            drawIntoCanvas { c ->
                val nc = c.nativeCanvas
                // ---- highlight behind the active ayah
                if (highlight != null) {
                    val hp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = hl.toArgb() }
                    lay.words.filter { it.g.sura == highlight.sura && it.g.ayah == highlight.ayah }
                        .groupBy { it.rect.top }.values.forEach { row ->
                            val r = RectF(row.minOf { it.rect.left }, row.first().rect.top, row.maxOf { it.rect.right }, row.first().rect.bottom)
                            nc.drawRoundRect(r, 18f, 18f, hp)
                        }
                }
                // ---- words that have a meaning (meanings mode)
                if (explained.isNotEmpty()) {
                    val mp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33C49A45 }
                    val up = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFC49A45.toInt(); strokeWidth = 4f; strokeCap = Paint.Cap.ROUND }
                    for (pl in explained.keys) {
                        val r = pl.rect
                        val box = RectF(r.left + 4f, r.top + r.height() * 0.12f, r.right - 4f, r.bottom - r.height() * 0.04f)
                        nc.drawRoundRect(box, 14f, 14f, mp)
                        nc.drawLine(box.left + 10f, box.bottom - 2f, box.right - 10f, box.bottom - 2f, up)
                    }
                }
                drawMushaf(nc, lay, ink.toArgb())
            }
        }
        }
    }
}

/**
 * The page's ink: surah bands, basmala and words, exactly as the Mushaf prints them. Shared by
 * the reader and the shared card, so both look the same.
 */
fun drawMushaf(nc: android.graphics.Canvas, lay: PageLayout, ink: Int) {
    // ---- surah title bands
    val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = lay.headerFont; textSize = lay.headerSize; color = ink }
    for (f in lay.frames) {
        Ornaments.surahFrame(nc, f.rect)
        // the title font has a single weight: a small spread makes the name bolder
        val tw = lay.headerSize * TITLE_WEIGHT
        for ((dx, dy) in TITLE_SPREAD) nc.drawText(f.title, f.titleX + dx * tw, f.baseline + dy * tw, tp)
    }
    // ---- basmala
    val bpaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = lay.headerFont; textSize = lay.basmalaSize; color = ink }
    for ((t, x, y) in lay.basmalas) nc.drawTextRun(t, 0, t.length, 0, t.length, x, y, true, bpaint)
    // ---- words
    // the printed page has heavier strokes than the raw outlines. Each word is drawn three times a
    // fraction of a pixel apart (colour glyphs can't be stroked). Words are snapped to whole pixels
    // first, so every word thickens by exactly the same amount.
    val wp = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = lay.pageFont; textSize = lay.textSize; color = ink }
    for (pl in lay.words) {
        val s = pl.g.code
        val x = kotlin.math.round(pl.x); val y = kotlin.math.round(pl.baseline)
        for (k in WEIGHT_SPREAD.indices step 2) {
            nc.drawTextRun(s, 0, s.length, 0, s.length, x + WEIGHT_SPREAD[k], y + WEIGHT_SPREAD[k + 1], true, wp)
        }
    }
}

/**
 * One ayah as its own little page: its words and ayah-end mark in order, with the surah band and
 * basmala above when it opens the surah. Lines are renumbered so they stack on the card.
 */
fun ayahCardPage(data: PageData, sura: Int, ayah: Int): PageData {
    val out = ArrayList<MushafLine>()
    // the surah's band, drawn from its name glyph (U+F100 + sura − 1), as the Mushaf prints it
    if (ayah != 1) out += MushafLine(0, GlyphType.HEADER, listOf(Glyph(GlyphType.HEADER, (0xF100 + sura - 1).toChar().toString(), sura, 0)))
    var header: MushafLine? = null
    var basmala: MushafLine? = null
    var started = false
    for (l in data.lines) {
        when (l.type) {
            GlyphType.HEADER -> { header = l; basmala = null }
            GlyphType.BASMALA -> basmala = l
            else -> {
                val mine = l.glyphs.filter { it.sura == sura && it.ayah == ayah }
                if (mine.isEmpty()) continue
                if (ayah == 1 && !started) {
                    started = true
                    header?.let { out += it }
                    if (sura != 9) basmala?.let { out += it }
                }
                out += MushafLine(0, l.type, mine)
            }
        }
    }
    return PageData(data.page, out.mapIndexed { i, l -> l.copy(number = i + 1) })
}
