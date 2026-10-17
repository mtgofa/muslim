package com.mtgofa.muslem.quran

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.LruCache
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Colours of the hand-drawn Mushaf ornaments, for the light and the night page. */
class OrnamentColors(val night: Boolean) {
    val border = if (night) 0xFF183A20.toInt() else 0xFF246B30.toInt()
    val stripe = if (night) 0xFF3F6E47.toInt() else 0xFFA6D29C.toInt()
    val field = if (night) 0xFF2A5E33.toInt() else 0xFF3E9849.toInt()
    val scroll = if (night) 0xFFC9E2CC.toInt() else 0xFFFFFFFF.toInt()
    val leafFill = if (night) 0xFF4F8A59.toInt() else 0xFF8CCB84.toInt()
    val petal = if (night) 0xFFC77E9E.toInt() else 0xFFEE8FB7.toInt()
    val petalLight = if (night) 0xFFE0B1C5.toInt() else 0xFFF9CFE0.toInt()
    val petalEdge = if (night) 0xFF8E4466.toInt() else 0xFFC23F78.toInt()
    val roundel = if (night) 0xFF5D8F63.toInt() else 0xFFD5ECCD.toInt()
    val cartouche = if (night) 0xFF1D2324.toInt() else 0xFFFFFFFF.toInt()
    val box = if (night) 0xFF2B3A2E.toInt() else 0xFFDCEDD6.toInt()
    val boxEdge = if (night) 0xFF5E9B67.toInt() else 0xFF4AA157.toInt()
}

/**
 * Arabesque ornaments for the Mushaf page, drawn with paths (no artwork shipped) and cached as
 * bitmaps per size, since they appear on every page.
 */
object Ornaments {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val cache = LruCache<String, Bitmap>(8)

    // ============================================================== surah title band

    /** Draw the band into [r] on [c]; the surah name is drawn on top by the caller. */
    @Synchronized
    fun surahFrame(c: Canvas, r: RectF, col: OrnamentColors) {
        val w = r.width().toInt().coerceAtLeast(1); val h = r.height().toInt().coerceAtLeast(1)
        val key = "band:$w:$h:${col.night}"
        val bmp = cache.get(key) ?: Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also {
            drawBand(Canvas(it), w.toFloat(), h.toFloat(), col)
            cache.put(key, it)
        }
        c.drawBitmap(bmp, r.left, r.top, null)
    }

    private fun drawBand(c: Canvas, w: Float, h: Float, col: OrnamentColors) {
        val cy = h / 2
        // frame: dark border, light stripe, dark rule, green field
        fill.color = col.border; c.drawRect(0f, 0f, w, h, fill)
        val s1 = h * 0.045f
        fill.color = col.stripe; c.drawRect(s1, s1, w - s1, h - s1, fill)
        val s2 = h * 0.085f
        fill.color = col.border; c.drawRect(s2, s2, w - s2, h - s2, fill)
        val s3 = h * 0.1f
        fill.color = col.field; c.drawRect(s3, s3, w - s3, h - s3, fill)
        val top = s3; val bot = h - s3

        val cw = w * 0.44f
        val cl = (w - cw) / 2; val cr = cl + cw
        val tip = h * 0.34f

        // the two side panels first, the cartouche on top
        panel(c, s3, cl - tip * 0.5f, top, bot, h, col)
        panel(c, cr + tip * 0.5f, w - s3, top, bot, h, col)

        val ct = top + h * 0.05f; val cb = bot - h * 0.05f
        fill.color = col.border; c.drawPath(cartouchePath(cl, cr, ct, cb, cy, tip, h * 0.085f), fill)
        fill.color = col.scroll; c.drawPath(cartouchePath(cl, cr, ct, cb, cy, tip, h * 0.06f), fill)
        val cart = cartouchePath(cl, cr, ct, cb, cy, tip, 0f)
        fill.color = col.cartouche; c.drawPath(cart, fill)
        stroke.color = col.border; stroke.strokeWidth = h * 0.03f; c.drawPath(cart, stroke)
        stroke.color = col.stripe; stroke.strokeWidth = h * 0.012f
        c.drawPath(cartouchePath(cl, cr, ct, cb, cy, tip, -h * 0.06f), stroke)

        // buds pointing at the cartouche tips
        bud(c, cl - tip - h * 0.2f, cy, h * 0.075f, 90f, col)
        bud(c, cr + tip + h * 0.2f, cy, h * 0.075f, -90f, col)
    }

    /** Cartouche: flat top and bottom with ogee ends that meet at a point. */
    private fun cartouchePath(l: Float, r: Float, t: Float, b: Float, cy: Float, tip: Float, grow: Float): Path {
        val L = l - grow; val R = r + grow; val T = t - grow; val B = b + grow; val tp = tip + grow * 0.5f
        val notch = (B - T) * 0.22f
        return Path().apply {
            moveTo(L + notch, T)
            lineTo(R - notch, T)
            quadTo(R - notch * 0.2f, T, R, T + notch * 0.6f)
            cubicTo(R + tp * 0.25f, cy - (B - T) * 0.18f, R + tp * 0.6f, cy - (B - T) * 0.05f, R + tp, cy)
            cubicTo(R + tp * 0.6f, cy + (B - T) * 0.05f, R + tp * 0.25f, cy + (B - T) * 0.18f, R, B - notch * 0.6f)
            quadTo(R - notch * 0.2f, B, R - notch, B)
            lineTo(L + notch, B)
            quadTo(L + notch * 0.2f, B, L, B - notch * 0.6f)
            cubicTo(L - tp * 0.25f, cy + (B - T) * 0.18f, L - tp * 0.6f, cy + (B - T) * 0.05f, L - tp, cy)
            cubicTo(L - tp * 0.6f, cy - (B - T) * 0.05f, L - tp * 0.25f, cy - (B - T) * 0.18f, L, T + notch * 0.6f)
            quadTo(L + notch * 0.2f, T, L + notch, T)
            close()
        }
    }

    /** One side panel: interlaced white scrolls with curls, leaves, buds and a central roundel. */
    private fun panel(c: Canvas, a: Float, b: Float, top: Float, bot: Float, h: Float, col: OrnamentColors) {
        val cy = (top + bot) / 2
        val mid = (a + b) / 2
        val rr = h * 0.33f
        val amp = (bot - top) * 0.3f
        c.save()
        c.clipRect(a, top, b, bot)
        // two interlaced vines (sine and inverted sine) running across the panel
        for (sign in intArrayOf(1, -1)) {
            val vine = Path()
            val steps = 80
            for (i in 0..steps) {
                val t = i / steps.toFloat()
                val x = a + (b - a) * t
                val y = cy + sign * amp * sin(t * 2 * PI * 2).toFloat()
                if (i == 0) vine.moveTo(x, y) else vine.lineTo(x, y)
            }
            stroke.color = col.border; stroke.strokeWidth = h * 0.06f; c.drawPath(vine, stroke)
            stroke.color = col.scroll; stroke.strokeWidth = h * 0.032f; c.drawPath(vine, stroke)
        }
        // curls, leaves and buds at the crests and troughs
        for (k in 0 until 4) {
            val x = a + (b - a) * (k * 0.25f + 0.125f)
            val up = k % 2 == 0
            if (abs(x - mid) < rr * 1.25f) continue
            val y = cy + (if (up) -1 else 1) * amp
            curl(c, x, y + (if (up) h * 0.13f else -h * 0.13f), h * 0.1f, up, col, h)
            curl(c, x, y, h * 0.06f, !up, col, h)
            leaf(c, x + (b - a) * 0.07f, cy, h * 0.12f, if (up) -40f else 40f, col, h)
            leaf(c, x - (b - a) * 0.07f, cy, h * 0.12f, if (up) 40f else -40f, col, h)
            bud(c, x, if (up) bot - h * 0.09f else top + h * 0.09f, h * 0.05f, if (up) 180f else 0f, col)
        }
        // roundel with a flower
        fill.color = col.border; c.drawCircle(mid, cy, rr * 1.12f, fill)
        fill.color = col.roundel; c.drawCircle(mid, cy, rr * 1.04f, fill)
        stroke.color = col.scroll; stroke.strokeWidth = h * 0.02f; c.drawCircle(mid, cy, rr * 0.98f, stroke)
        flower(c, mid, cy, rr * 0.9f, col)
        c.restore()
    }

    /** A spiral tendril. */
    private fun curl(c: Canvas, x: Float, y: Float, r: Float, clockwise: Boolean, col: OrnamentColors, h: Float) {
        val p = spiral(x, y, r, clockwise, 1.6)
        stroke.color = col.border; stroke.strokeWidth = h * 0.05f; c.drawPath(p, stroke)
        stroke.color = col.scroll; stroke.strokeWidth = h * 0.026f; c.drawPath(p, stroke)
    }

    private fun spiral(x: Float, y: Float, r: Float, clockwise: Boolean, turns: Double): Path {
        val p = Path()
        val n = 40
        for (i in 0..n) {
            val t = i / n.toDouble()
            val ang = (if (clockwise) 1 else -1) * t * turns * 2 * PI
            val rad = r * (1 - 0.8 * t)
            val px = x + (rad * cos(ang)).toFloat(); val py = y + (rad * sin(ang)).toFloat()
            if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
        }
        return p
    }

    /** Almond leaf with a white outline and a centre vein. */
    private fun leaf(c: Canvas, x: Float, y: Float, s: Float, angle: Float, col: OrnamentColors, h: Float) {
        c.save(); c.rotate(angle, x, y)
        val p = Path().apply {
            moveTo(x - s, y)
            quadTo(x, y - s * 0.75f, x + s, y)
            quadTo(x, y + s * 0.75f, x - s, y)
            close()
        }
        fill.color = col.leafFill; c.drawPath(p, fill)
        stroke.color = col.scroll; stroke.strokeWidth = h * 0.02f; c.drawPath(p, stroke)
        c.drawLine(x - s * 0.7f, y, x + s * 0.7f, y, stroke)
        c.restore()
    }

    /** Tear-drop bud pointing toward [angle] (0 = up). */
    private fun bud(c: Canvas, x: Float, y: Float, s: Float, angle: Float, col: OrnamentColors) {
        c.save(); c.rotate(angle, x, y)
        val p = Path().apply {
            moveTo(x, y - s * 1.6f)
            cubicTo(x + s * 1.2f, y - s * 0.4f, x + s, y + s, x, y + s)
            cubicTo(x - s, y + s, x - s * 1.2f, y - s * 0.4f, x, y - s * 1.6f)
            close()
        }
        fill.color = col.petal; c.drawPath(p, fill)
        stroke.color = col.petalEdge; stroke.strokeWidth = s * 0.25f; c.drawPath(p, stroke)
        c.restore()
    }

    /** Eight-petal flower: pink petals with light hearts over green ones, pink centre. */
    fun flower(c: Canvas, cx: Float, cy: Float, r: Float, col: OrnamentColors) {
        for (i in 0 until 8) {
            c.save(); c.rotate(i * 45f + 22.5f, cx, cy)
            val g = RectF(cx - r * 0.2f, cy - r * 0.95f, cx + r * 0.2f, cy - r * 0.2f)
            fill.color = col.leafFill; c.drawOval(g, fill)
            stroke.color = col.border; stroke.strokeWidth = r * 0.05f; c.drawOval(g, stroke)
            c.restore()
        }
        for (i in 0 until 8) {
            c.save(); c.rotate(i * 45f, cx, cy)
            val p = Path().apply {
                moveTo(cx, cy - r * 0.15f)
                cubicTo(cx + r * 0.38f, cy - r * 0.35f, cx + r * 0.3f, cy - r * 0.85f, cx, cy - r)
                cubicTo(cx - r * 0.3f, cy - r * 0.85f, cx - r * 0.38f, cy - r * 0.35f, cx, cy - r * 0.15f)
                close()
            }
            fill.color = col.petal; c.drawPath(p, fill)
            stroke.color = col.petalEdge; stroke.strokeWidth = r * 0.05f; c.drawPath(p, stroke)
            fill.color = col.petalLight
            c.drawOval(RectF(cx - r * 0.09f, cy - r * 0.75f, cx + r * 0.09f, cy - r * 0.38f), fill)
            c.restore()
        }
        fill.color = col.petalEdge; c.drawCircle(cx, cy, r * 0.22f, fill)
        fill.color = col.petalLight; c.drawCircle(cx, cy, r * 0.1f, fill)
    }

    // ============================================================== page-number badge

    /**
     * Page-number badge: a pale rounded box with a lotus-and-vine ornament on the page's outer
     * side ([outerRight] = right-hand page). Returns the box that holds the number.
     */
    @Synchronized
    fun pageBadge(c: Canvas, w: Float, h: Float, outerRight: Boolean, col: OrnamentColors): RectF {
        val boxW = w * 0.5f
        val box = if (outerRight) RectF(h * 0.06f, h * 0.18f, h * 0.06f + boxW, h * 0.82f)
        else RectF(w - h * 0.06f - boxW, h * 0.18f, w - h * 0.06f, h * 0.82f)
        val key = "badge:${w.toInt()}:${h.toInt()}:$outerRight:${col.night}"
        val bmp = cache.get(key) ?: Bitmap.createBitmap(w.toInt().coerceAtLeast(1), h.toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888).also {
            drawBadge(Canvas(it), h, box, outerRight, col)
            cache.put(key, it)
        }
        c.drawBitmap(bmp, 0f, 0f, null)
        return box
    }

    private fun drawBadge(c: Canvas, h: Float, box: RectF, outerRight: Boolean, col: OrnamentColors) {
        val dir = if (outerRight) 1f else -1f
        val edge = if (outerRight) box.right else box.left
        val cy = h / 2
        val rad = box.height() * 0.32f
        // vines from the box's top and bottom curling out to the lotus, with leaves
        for (sign in intArrayOf(-1, 1)) {
            val y0 = cy + sign * box.height() * 0.42f
            val p = Path().apply {
                moveTo(edge - dir * rad, y0)
                cubicTo(edge + dir * h * 0.25f, y0 + sign * h * 0.08f, edge + dir * h * 0.4f, cy + sign * h * 0.04f, edge + dir * h * 0.7f, cy)
            }
            stroke.color = col.boxEdge; stroke.strokeWidth = h * 0.07f; c.drawPath(p, stroke)
            stroke.strokeWidth = h * 0.05f
            c.drawPath(spiral(edge + dir * h * 0.22f, cy + sign * h * 0.13f, h * 0.1f, dir * sign > 0, 1.4), stroke)
            val lx = edge + dir * h * 0.5f; val ly = cy + sign * h * 0.33f; val ls = h * 0.25f
            c.save(); c.rotate(dir * sign * 35f, lx, ly)
            val leafPath = Path().apply {
                moveTo(lx - ls, ly); quadTo(lx, ly - ls * 0.6f, lx + ls, ly); quadTo(lx, ly + ls * 0.6f, lx - ls, ly); close()
            }
            fill.color = col.leafFill; c.drawPath(leafPath, fill)
            stroke.color = col.boxEdge; stroke.strokeWidth = h * 0.045f; c.drawPath(leafPath, stroke)
            c.drawLine(lx - ls * 0.6f, ly, lx + ls * 0.6f, ly, stroke)
            c.restore()
        }
        // lotus: three pink petals fanned toward the outside
        val lx = edge + dir * h * 0.72f
        for ((ang, len) in listOf(55f to 0.8f, -55f to 0.8f, 25f to 0.9f, -25f to 0.9f, 0f to 1f)) {
            c.save(); c.rotate(dir * 90f + ang, lx, cy)
            val l = h * 0.5f * len
            val p = Path().apply {
                moveTo(lx, cy)
                cubicTo(lx + l * 0.55f, cy - l * 0.25f, lx + l * 0.45f, cy - l * 0.85f, lx, cy - l)
                cubicTo(lx - l * 0.45f, cy - l * 0.85f, lx - l * 0.55f, cy - l * 0.25f, lx, cy)
                close()
            }
            fill.color = col.petal; c.drawPath(p, fill)
            stroke.color = col.petalEdge; stroke.strokeWidth = h * 0.035f; c.drawPath(p, stroke)
            fill.color = col.petalLight
            c.drawOval(RectF(lx - l * 0.12f, cy - l * 0.75f, lx + l * 0.12f, cy - l * 0.3f), fill)
            c.restore()
        }
        fill.color = col.leafFill; c.drawCircle(lx, cy, h * 0.1f, fill)
        stroke.color = col.boxEdge; stroke.strokeWidth = h * 0.04f; c.drawCircle(lx, cy, h * 0.1f, stroke)

        // the box itself, drawn last so it sits over the vine roots
        val rr = box.height() * 0.38f
        fill.color = col.box; c.drawRoundRect(box, rr, rr, fill)
        stroke.color = col.boxEdge; stroke.strokeWidth = h * 0.07f; c.drawRoundRect(box, rr, rr, stroke)
        val inner = RectF(box.left + h * 0.08f, box.top + h * 0.08f, box.right - h * 0.08f, box.bottom - h * 0.08f)
        stroke.strokeWidth = h * 0.025f; c.drawRoundRect(inner, rr * 0.7f, rr * 0.7f, stroke)
    }
}
