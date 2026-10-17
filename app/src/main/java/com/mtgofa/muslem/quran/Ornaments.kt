package com.mtgofa.muslem.quran

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/** Colours of the hand-drawn Mushaf ornaments, for the light and the night page. */
class OrnamentColors(night: Boolean) {
    val band = if (night) 0xFF2C5A36.toInt() else 0xFF4E9F5D.toInt()
    val bandDark = if (night) 0xFF1B3B22.toInt() else 0xFF2F7039.toInt()
    val bandLight = if (night) 0xFF4E7F58.toInt() else 0xFFCFE6CC.toInt()
    val vine = if (night) 0xFFB9D8BC.toInt() else 0xFFFFFFFF.toInt()
    val petal = if (night) 0xFFC07A98.toInt() else 0xFFEBA3C1.toInt()
    val petalEdge = if (night) 0xFF8E4466.toInt() else 0xFFC4517F.toInt()
    val leaf = if (night) 0xFF7FB487.toInt() else 0xFFBFE0B6.toInt()
    val cartouche = if (night) 0xFF1D2324.toInt() else 0xFFFFFDF6.toInt()
}

object Ornaments {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }

    /**
     * Surah title band: a green arabesque strip with a rosette on each side and a pale
     * cartouche in the middle that holds the surah name. Returns the cartouche bounds.
     */
    fun surahFrame(c: Canvas, r: RectF, col: OrnamentColors): RectF {
        val h = r.height()
        // band + double border
        fill.color = col.band
        c.drawRect(r, fill)
        stroke.color = col.bandDark; stroke.strokeWidth = h * 0.035f
        c.drawRect(r, stroke)
        stroke.color = col.bandLight; stroke.strokeWidth = h * 0.02f
        val inner = RectF(r.left + h * 0.08f, r.top + h * 0.08f, r.right - h * 0.08f, r.bottom - h * 0.08f)
        c.drawRect(inner, stroke)

        // cartouche
        val cw = r.width() * 0.46f
        val cx = r.centerX(); val cy = r.centerY()
        val box = RectF(cx - cw / 2, r.top + h * 0.12f, cx + cw / 2, r.bottom - h * 0.12f)
        val k = box.height() * 0.62f
        val p = Path().apply {
            moveTo(box.left - k * 0.15f, cy)
            cubicTo(box.left, cy - box.height() * 0.1f, box.left + k * 0.2f, box.top, box.left + k, box.top)
            lineTo(box.right - k, box.top)
            cubicTo(box.right - k * 0.2f, box.top, box.right, cy - box.height() * 0.1f, box.right + k * 0.15f, cy)
            cubicTo(box.right, cy + box.height() * 0.1f, box.right - k * 0.2f, box.bottom, box.right - k, box.bottom)
            lineTo(box.left + k, box.bottom)
            cubicTo(box.left + k * 0.2f, box.bottom, box.left, cy + box.height() * 0.1f, box.left - k * 0.15f, cy)
            close()
        }
        fill.color = col.cartouche
        c.drawPath(p, fill)
        stroke.color = col.bandDark; stroke.strokeWidth = h * 0.03f
        c.drawPath(p, stroke)

        // vines, buds and rosettes on both sides
        val sides = listOf(r.left + h * 0.1f to box.left - k * 0.15f, box.right + k * 0.15f to r.right - h * 0.1f)
        for ((a, b) in sides) {
            val mx = (a + b) / 2
            vines(c, a, b, cy, h, col)
            // four leaves framing the rosette, then the rosette itself
            for ((dx, dy) in listOf(-1 to -1, 1 to -1, -1 to 1, 1 to 1)) {
                c.save()
                val lx = mx + dx * h * 0.36f; val ly = cy + dy * h * 0.2f
                c.rotate(dx * dy * 35f, lx, ly)
                leaf(c, lx, ly, h * 0.11f, col)
                c.restore()
            }
            rosette(c, mx, cy, h * 0.33f, col)
        }
        // pink buds at the cartouche tips
        bud(c, box.left - k * 0.15f - h * 0.08f, cy, h * 0.07f, col)
        bud(c, box.right + k * 0.15f + h * 0.08f, cy, h * 0.07f, col)
        return box
    }

    private fun vines(c: Canvas, a: Float, b: Float, cy: Float, h: Float, col: OrnamentColors) {
        stroke.color = col.vine; stroke.strokeWidth = h * 0.034f
        val w = b - a
        val amp = h * 0.26f
        // two mirrored S-scrolls across the panel
        for (dir in intArrayOf(-1, 1)) {
            val path = Path().apply {
                moveTo(a + w * 0.04f, cy)
                cubicTo(a + w * 0.15f, cy + dir * amp * 1.2f, a + w * 0.3f, cy + dir * amp * 1.2f, a + w * 0.36f, cy + dir * amp * 0.35f)
                moveTo(b - w * 0.04f, cy)
                cubicTo(b - w * 0.15f, cy + dir * amp * 1.2f, b - w * 0.3f, cy + dir * amp * 1.2f, b - w * 0.36f, cy + dir * amp * 0.35f)
            }
            c.drawPath(path, stroke)
            // small curls at the ends of the scrolls
            for (x in floatArrayOf(a + w * 0.2f, b - w * 0.2f)) {
                c.drawCircle(x, cy + dir * amp * 0.95f, h * 0.05f, stroke)
            }
            // leaves
            for (x in floatArrayOf(a + w * 0.11f, b - w * 0.11f)) leaf(c, x, cy + dir * amp * 0.75f, h * 0.09f, col)
        }
        for (x in floatArrayOf(a + w * 0.04f, b - w * 0.04f)) bud(c, x, cy, h * 0.055f, col)
    }

    private fun leaf(c: Canvas, x: Float, y: Float, s: Float, col: OrnamentColors) {
        fill.color = col.leaf
        c.drawOval(RectF(x - s, y - s * 0.55f, x + s, y + s * 0.55f), fill)
    }

    private fun bud(c: Canvas, x: Float, y: Float, s: Float, col: OrnamentColors) {
        fill.color = col.petal
        c.drawCircle(x, y, s, fill)
        stroke.color = col.petalEdge; stroke.strokeWidth = s * 0.35f
        c.drawCircle(x, y, s, stroke)
    }

    /** Eight-petal flower: pink outer petals over pale-green ones, with a dark centre. */
    fun rosette(c: Canvas, cx: Float, cy: Float, r: Float, col: OrnamentColors) {
        for (layer in 0..1) {
            val rr = if (layer == 0) r else r * 0.72f
            fill.color = if (layer == 0) col.leaf else col.petal
            stroke.color = if (layer == 0) col.bandDark else col.petalEdge
            stroke.strokeWidth = r * 0.06f
            for (i in 0 until 8) {
                val ang = Math.toRadians((i * 45.0) + if (layer == 0) 22.5 else 0.0)
                c.save()
                c.rotate(Math.toDegrees(ang).toFloat(), cx, cy)
                val petal = RectF(cx - rr * 0.22f, cy - rr, cx + rr * 0.22f, cy - rr * 0.18f)
                c.drawOval(petal, fill)
                c.drawOval(petal, stroke)
                c.restore()
            }
        }
        fill.color = col.petalEdge
        c.drawCircle(cx, cy, r * 0.17f, fill)
        fill.color = col.cartouche
        c.drawCircle(cx, cy, r * 0.07f, fill)
    }

    /**
     * Page-number badge: a pale rounded box with a leafy flourish on the page's outer side
     * ([outerRight] = right-hand page). Returns the box that holds the number.
     */
    fun pageBadge(c: Canvas, w: Float, h: Float, outerRight: Boolean, col: OrnamentColors): RectF {
        val flourish = h * 1.1f
        val box = if (outerRight) RectF(h * 0.1f, h * 0.12f, w - flourish, h * 0.88f)
        else RectF(flourish, h * 0.12f, w - h * 0.1f, h * 0.88f)
        fill.color = col.bandLight
        c.drawRoundRect(box, h * 0.3f, h * 0.3f, fill)
        stroke.color = col.band; stroke.strokeWidth = h * 0.06f
        c.drawRoundRect(box, h * 0.3f, h * 0.3f, stroke)
        // flourish: vine scroll, two leaves and a small rosette
        val x0 = if (outerRight) box.right else box.left
        val dir = if (outerRight) 1 else -1
        val cy = h / 2
        stroke.color = col.band; stroke.strokeWidth = h * 0.06f
        val p = Path().apply {
            moveTo(x0, cy)
            cubicTo(x0 + dir * flourish * 0.3f, cy - h * 0.45f, x0 + dir * flourish * 0.6f, cy + h * 0.45f, x0 + dir * flourish * 0.85f, cy)
        }
        c.drawPath(p, stroke)
        leaf(c, x0 + dir * flourish * 0.3f, cy + h * 0.22f, h * 0.16f, col)
        leaf(c, x0 + dir * flourish * 0.55f, cy - h * 0.22f, h * 0.16f, col)
        rosette(c, x0 + dir * flourish * 0.8f, cy, h * 0.3f, col)
        return box
    }
}
