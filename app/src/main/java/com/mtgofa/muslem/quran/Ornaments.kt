package com.mtgofa.muslem.quran

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.LruCache
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Mushaf ornament colours: deep emerald and gold on cream. */
private object Orn {
    const val EMERALD = 0xFF175E43.toInt()
    const val EMERALD_LIGHT = 0xFF2A7A59.toInt()
    const val GOLD = 0xFFC49A45.toInt()
    const val GOLD_LIGHT = 0xFFE7CF95.toInt()
    const val CREAM = 0xFFFFFBEF.toInt()
}

/**
 * Geometric Islamic ornaments for the Mushaf page (eight-point stars and a star lattice),
 * drawn with paths and cached as bitmaps per size, since they appear on every page.
 */
object Ornaments {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeJoin = Paint.Join.MITER; strokeCap = Paint.Cap.BUTT
    }
    private val cache = LruCache<String, Bitmap>(8)

    private fun cached(key: String, w: Int, h: Int, draw: (Canvas) -> Unit): Bitmap =
        cache.get(key) ?: Bitmap.createBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1), Bitmap.Config.ARGB_8888).also {
            draw(Canvas(it)); cache.put(key, it)
        }

    /** Eight-point star made of two overlapping squares (the rub el hizb figure). */
    private fun star8(cx: Float, cy: Float, r: Float): Path {
        val p = Path()
        for (i in 0 until 16) {
            val ang = -PI / 2 + i * PI / 8
            val rad = if (i % 2 == 0) r else r * 0.76f
            val x = cx + (rad * cos(ang)).toFloat(); val y = cy + (rad * sin(ang)).toFloat()
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        return p
    }

    // ============================================================== surah title band

    /** Draw the band into [r]; the surah name is drawn on top by the caller. */
    @Synchronized
    fun surahFrame(c: Canvas, r: RectF) {
        val w = r.width().toInt(); val h = r.height().toInt()
        c.drawBitmap(cached("band:$w:$h", w, h) { drawBand(it, w.toFloat(), h.toFloat()) }, r.left, r.top, null)
    }

    private fun drawBand(c: Canvas, w: Float, h: Float) {
        val cy = h / 2
        // frame: gold rule, emerald band, inner gold rule
        fill.color = Orn.GOLD; c.drawRoundRect(RectF(0f, 0f, w, h), h * 0.08f, h * 0.08f, fill)
        val i1 = h * 0.05f
        fill.color = Orn.EMERALD; c.drawRoundRect(RectF(i1, i1, w - i1, h - i1), h * 0.06f, h * 0.06f, fill)
        val i2 = h * 0.12f
        stroke.color = Orn.GOLD; stroke.strokeWidth = h * 0.018f
        c.drawRect(i2, i2, w - i2, h - i2, stroke)

        val cw = w * 0.46f
        val cl = (w - cw) / 2; val cr = cl + cw
        val tip = h * 0.3f
        val medal = h * 0.36f

        // star lattice in the two side fields, then the medallions over their inner ends
        lattice(c, i2, cl - tip - medal * 0.5f, i2, h - i2, h)
        lattice(c, cr + tip + medal * 0.5f, w - i2, i2, h - i2, h)
        for (x in floatArrayOf(cl - tip - h * 0.42f, cr + tip + h * 0.42f)) medallion(c, x, cy, medal, h)

        // cartouche: cream with a gold edge and an emerald inner rule
        val ct = h * 0.17f; val cb = h - h * 0.17f
        fill.color = Orn.GOLD; c.drawPath(cartouche(cl, cr, ct, cb, cy, tip, h * 0.045f), fill)
        fill.color = Orn.CREAM; c.drawPath(cartouche(cl, cr, ct, cb, cy, tip, 0f), fill)
        stroke.color = Orn.EMERALD_LIGHT; stroke.strokeWidth = h * 0.012f
        c.drawPath(cartouche(cl, cr, ct, cb, cy, tip, -h * 0.05f), stroke)
        // small stars inside the cartouche points
        fill.color = Orn.GOLD
        c.drawPath(star8(cl - tip * 0.42f, cy, h * 0.07f), fill)
        c.drawPath(star8(cr + tip * 0.42f, cy, h * 0.07f), fill)
    }

    /** Pointed-arch cartouche ends (ogee curves meeting at the tip). */
    private fun cartouche(l: Float, r: Float, t: Float, b: Float, cy: Float, tip: Float, grow: Float): Path {
        val L = l - grow; val R = r + grow; val T = t - grow; val B = b + grow; val tp = tip + grow * 0.6f
        val hh = B - T
        return Path().apply {
            moveTo(L, T); lineTo(R, T)
            cubicTo(R + tp * 0.15f, T, R + tp * 0.55f, cy - hh * 0.22f, R + tp, cy)
            cubicTo(R + tp * 0.55f, cy + hh * 0.22f, R + tp * 0.15f, B, R, B)
            lineTo(L, B)
            cubicTo(L - tp * 0.15f, B, L - tp * 0.55f, cy + hh * 0.22f, L - tp, cy)
            cubicTo(L - tp * 0.55f, cy - hh * 0.22f, L - tp * 0.15f, T, L, T)
            close()
        }
    }

    /** Repeating eight-point stars on a diagonal lattice, gold lines on emerald. */
    private fun lattice(c: Canvas, a: Float, b: Float, top: Float, bot: Float, h: Float) {
        if (b - a < h * 0.3f) return
        c.save(); c.clipRect(a, top, b, bot)
        val cell = bot - top
        val cy = (top + bot) / 2
        stroke.color = Orn.GOLD; stroke.strokeWidth = h * 0.016f
        var x = a - cell
        while (x < b + cell) {
            c.drawLine(x, top, x + cell, bot, stroke)
            c.drawLine(x + cell, top, x, bot, stroke)
            x += cell
        }
        x = a + cell / 2
        while (x < b) {
            fill.color = Orn.EMERALD; c.drawPath(star8(x, cy, cell * 0.3f), fill)
            stroke.color = Orn.GOLD; c.drawPath(star8(x, cy, cell * 0.3f), stroke)
            fill.color = Orn.GOLD_LIGHT; c.drawCircle(x, cy, cell * 0.06f, fill)
            x += cell
        }
        c.restore()
    }

    /** Gold eight-point medallion with an emerald inner star. */
    private fun medallion(c: Canvas, x: Float, y: Float, r: Float, h: Float) {
        fill.color = Orn.GOLD; c.drawPath(star8(x, y, r), fill)
        fill.color = Orn.EMERALD; c.drawPath(star8(x, y, r * 0.72f), fill)
        stroke.color = Orn.GOLD_LIGHT; stroke.strokeWidth = h * 0.014f; c.drawPath(star8(x, y, r * 0.55f), stroke)
        fill.color = Orn.GOLD; c.drawCircle(x, y, r * 0.16f, fill)
    }
}
