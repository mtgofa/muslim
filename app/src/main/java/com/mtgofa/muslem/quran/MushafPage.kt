package com.mtgofa.muslem.quran

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
class Placed(val g: Glyph, val x: Float, val baseline: Float, val rect: RectF)

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
)

private const val LINES = 15
/** Surah name and basmala sizes, as a fraction of the line height. */
private const val TITLE_SIZE = 0.38f
/** Extra stroke weight for the surah names, as a fraction of their size. */
private const val TITLE_WEIGHT = 0.018f
private val TITLE_SPREAD = listOf(-1f to -1f, 1f to -1f, -1f to 1f, 1f to 1f, 0f to 0f)

/** Extra stroke weight for the page text, as a fraction of the text size. */
private const val WEIGHT = 0.0025f
/** Basmala width as a fraction of the page width. */
private const val BASMALA_WIDTH = 0.62f
/** The long-form basmala in the title font, whose colour layers carry the tajweed. */
private const val BASMALA_WORDS = "\uFE9D\uFEA1\uFEA5\uFEA9"

/** Measure and place every glyph of [page] into a [w] x [h] pixel box. */
fun layoutPage(ctx: Context, data: PageData, w: Float, h: Float, style: MushafStyle): PageLayout {
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
    p.textSize = size
    val fm = p.fontMetrics
    val scale = size / 100f

    // the two opening pages are a short, centred block in the middle of the page
    val usedLines = data.lines.maxOfOrNull { it.number } ?: LINES
    val top = if (special) (h - usedLines * lineH * 1.08f) / 2f else 0f
    val lh = if (special) lineH * 1.08f else lineH

    val words = ArrayList<Placed>()
    val frames = ArrayList<Frame>()
    val basmalas = ArrayList<Triple<String, Float, Float>>()
    val hp = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = headerFont }
    val bp = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = headerFont; textSize = 100f }
    val basmalaSize = 100f * (w * BASMALA_WIDTH) / adv(BASMALA_WORDS, bp)
    bp.textSize = basmalaSize

    for (line in data.lines) {
        val y0 = top + (line.number - 1) * lh
        val cy = y0 + lh / 2f
        when (line.type) {
            GlyphType.HEADER -> {
                hp.textSize = lh * TITLE_SIZE
                val code = line.glyphs.first().code
                // centre on the glyph's drawn outline: the font metrics include room for marks
                // the title never uses, which pushed it low in the cartouche
                val ink = android.graphics.Rect().also { hp.getTextBounds(code, 0, code.length, it) }
                val rect = RectF(w * 0.01f, y0 + lh * 0.06f, w * 0.99f, y0 + lh * 0.94f)
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
                val baseline = cy - (fm.ascent + fm.descent) / 2f
                line.glyphs.forEachIndexed { i, g ->
                    val gw = ws[i]
                    x -= gw
                    words += Placed(g, x, baseline, RectF(x - gap / 2f, y0 + lh * 0.04f, x + gw + gap / 2f, y0 + lh * 0.98f))
                    x -= gap
                }
            }
        }
    }
    return PageLayout(data.page, size, lh * TITLE_SIZE, pageFont, headerFont, words, frames, basmalas, basmalaSize)
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
) {
    val ctx = LocalContext.current
    val density = LocalDensity.current
    BoxWithConstraints(modifier) {
        val w = with(density) { maxWidth.toPx() }
        val h = with(density) { maxHeight.toPx() }
        var layout by remember(page, style, w, h) { mutableStateOf<PageLayout?>(null) }
        LaunchedEffect(page, style, w, h) {
            layout = withContext(Dispatchers.Default) { layoutPage(ctx, QuranDb.page(page), w, h, style) }
        }
        val ink = Color(0xFF1A1A1A)
        val hl = Color(0x2A0F7A62)
        val lay = layout ?: return@BoxWithConstraints

        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(lay) {
                    fun hit(x: Float, y: Float) = lay.words.firstOrNull { it.rect.contains(x, y) }?.g
                        ?.let { AyahRef(it.sura, it.ayah) }
                    detectTapGestures(
                        onTap = { o -> onTap(hit(o.x, o.y)) },
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
                // ---- surah title bands
                val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = lay.headerFont; textSize = lay.headerSize; color = ink.toArgb() }
                for (f in lay.frames) {
                    Ornaments.surahFrame(nc, f.rect)
                    // the title font has a single weight: a small spread makes the name bolder
                    val tw = lay.headerSize * TITLE_WEIGHT
                    for ((dx, dy) in TITLE_SPREAD) nc.drawText(f.title, f.titleX + dx * tw, f.baseline + dy * tw, tp)
                }
                // ---- basmala
                val bpaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = lay.headerFont; textSize = lay.basmalaSize; color = ink.toArgb() }
                for ((t, x, y) in lay.basmalas) nc.drawTextRun(t, 0, t.length, 0, t.length, x, y, true, bpaint)
                // ---- words
                val wp = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = lay.pageFont; textSize = lay.textSize; color = ink.toArgb() }
                // the printed page has heavier strokes than the raw outlines; spreading each word
                // over a sub-pixel square thickens it without touching the tajweed colours
                val d = lay.textSize * WEIGHT
                val spread = floatArrayOf(-d, -d, d, -d, -d, d, d, d, 0f, 0f)
                for (pl in lay.words) {
                    val s = pl.g.code
                    for (k in spread.indices step 2) {
                        nc.drawTextRun(s, 0, s.length, 0, s.length, pl.x + spread[k], pl.baseline + spread[k + 1], true, wp)
                    }
                }
            }
        }
    }
}
