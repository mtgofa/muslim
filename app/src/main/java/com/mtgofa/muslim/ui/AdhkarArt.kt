package com.mtgofa.muslim.ui

import com.mtgofa.muslim.data.Adhkar
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Card colours and drawing for each adhkar category. */
class AdhkarTheme(val bg: Color, val blob: Color, val draw: DrawScope.() -> Unit)

private val CLOUD = Color(0xFFFFFFFF)

fun adhkarTheme(id: Int): AdhkarTheme = when (id) {
    27, Adhkar.MORNING -> AdhkarTheme(Color(0xFFFFF4D6), Color(0xFFFCE3A6)) { sun(); cloud(0.12f, 0.3f, 1f); cloud(0.55f, 0.72f, 0.8f) }
    Adhkar.EVENING -> AdhkarTheme(Color(0xFFE8E4FA), Color(0xFFCFC6F2)) { night(Color(0xFF3B2F7A)); cloud(0.5f, 0.72f, 0.8f) }
    25 -> AdhkarTheme(Color(0xFFE3ECFF), Color(0xFFC9D8FB)) { mosqueArch(); prayerMat() }
    28 -> AdhkarTheme(Color(0xFFD9F6FA), Color(0xFFB4E9F1)) { night(Color(0xFF1E3A6B)); pillow() }
    1 -> AdhkarTheme(Color(0xFFFFE3E0), Color(0xFFF9C9C3)) { sunrise() }
    15 -> AdhkarTheme(Color(0xFFE2F5EA), Color(0xFFC3E8D2)) { minaret(); waves() }
    13, 14 -> AdhkarTheme(Color(0xFFE6F4EF), Color(0xFFC8E6DA)) { mosqueDoor(id == 13) }
    9 -> AdhkarTheme(Color(0xFFE0F2FF), Color(0xFFBDE1FB)) { drops() }
    10, 11 -> AdhkarTheme(Color(0xFFEEE8FF), Color(0xFFD8CCFB)) { house(id == 11) }
    69, 70 -> AdhkarTheme(Color(0xFFFFEEDD), Color(0xFFFBD7B3)) { dates() }
    96 -> AdhkarTheme(Color(0xFFE5F0FF), Color(0xFFC6DCFA)) { suitcase(); cloud(0.15f, 0.25f, 0.8f) }
    129 -> AdhkarTheme(Color(0xFFF1EBFF), Color(0xFFDCD0FA)) { book() }
    34 -> AdhkarTheme(Color(0xFFFFE6EE), Color(0xFFF9C6D7)) { heart() }
    130 -> AdhkarTheme(Color(0xFFFFF3E0), Color(0xFFF7DDB3)) { beads() }
    else -> AdhkarTheme(Color(0xFFEAF5EF), Color(0xFFCFE8DA)) { book() }
}

/** The card's picture: a soft wave on the left with the category drawing on it. */
@Composable
fun AdhkarArt(theme: AdhkarTheme, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val wave = Path().apply {
            moveTo(0f, h * 0.15f)
            cubicTo(w * 0.35f, h * 0.0f, w * 0.55f, h * 0.35f, w * 0.48f, h * 0.6f)
            cubicTo(w * 0.42f, h * 0.85f, w * 0.75f, h * 0.9f, w, h)
            lineTo(0f, h); close()
        }
        drawPath(wave, theme.blob)
        theme.draw(this)
    }
}

// ---- pieces. Coordinates are fractions of the art box (its left ~55% holds the drawing).

private fun DrawScope.p(x: Float, y: Float) = Offset(size.width * x, size.height * y)
private val DrawScope.u get() = size.height

private fun DrawScope.cloud(x: Float, y: Float, s: Float) {
    val c = p(x, y); val r = u * 0.07f * s
    drawCircle(CLOUD, r, c)
    drawCircle(CLOUD, r * 1.3f, Offset(c.x + r * 1.3f, c.y - r * 0.4f))
    drawCircle(CLOUD, r, Offset(c.x + r * 2.6f, c.y))
    drawRoundRect(CLOUD, Offset(c.x, c.y - r * 0.2f), Size(r * 2.6f, r * 1.2f), CornerRadius(r))
}

private fun DrawScope.sun() {
    val c = p(0.3f, 0.48f); val r = u * 0.2f
    for (i in 0 until 8) {
        val a = i * PI / 4
        drawLine(
            Color(0xFFF7C430), Offset(c.x + (r * 1.35f * cos(a)).toFloat(), c.y + (r * 1.35f * sin(a)).toFloat()),
            Offset(c.x + (r * 1.75f * cos(a)).toFloat(), c.y + (r * 1.75f * sin(a)).toFloat()), u * 0.035f, StrokeCap.Round,
        )
    }
    drawCircle(Color(0xFFFFC72C), r, c)
    drawCircle(Color(0xFFFFD95A), r * 0.75f, Offset(c.x - r * 0.15f, c.y - r * 0.15f))
}

private fun DrawScope.sunrise() {
    val c = p(0.3f, 0.72f); val r = u * 0.2f
    for (i in 0..6) {
        val a = PI + i * PI / 6
        drawLine(
            Color(0xFFF59E5B), Offset(c.x + (r * 1.3f * cos(a)).toFloat(), c.y + (r * 1.3f * sin(a)).toFloat()),
            Offset(c.x + (r * 1.65f * cos(a)).toFloat(), c.y + (r * 1.65f * sin(a)).toFloat()), u * 0.03f, StrokeCap.Round,
        )
    }
    drawArc(Color(0xFFF7894A), 180f, 180f, true, Offset(c.x - r, c.y - r), Size(r * 2, r * 2))
    drawRoundRect(Color(0xFFE76F51), Offset(c.x - r * 1.9f, c.y), Size(r * 3.8f, u * 0.025f), CornerRadius(u))
    // two birds
    for ((bx, by) in listOf(0.12f to 0.3f, 0.2f to 0.22f)) {
        val b = p(bx, by); val k = u * 0.04f
        drawLine(Color(0xFF8C4A3C), b, Offset(b.x + k, b.y + k * 0.6f), u * 0.012f, StrokeCap.Round)
        drawLine(Color(0xFF8C4A3C), Offset(b.x + k, b.y + k * 0.6f), Offset(b.x + k * 2, b.y), u * 0.012f, StrokeCap.Round)
    }
}

private fun DrawScope.crescentAt(c: Offset, r: Float, color: Color) {
    val moon = Path().apply { addOval(Rect(c, r)) }
    val bite = Path().apply { addOval(Rect(Offset(c.x + r * 0.45f, c.y - r * 0.2f), r * 0.9f)) }
    drawPath(Path().apply { op(moon, bite, PathOperation.Difference) }, color)
}

private fun DrawScope.star(c: Offset, r: Float, color: Color) {
    val path = Path()
    for (i in 0 until 10) {
        val a = -PI / 2 + i * PI / 5
        val rad = if (i % 2 == 0) r else r * 0.45f
        val x = c.x + (rad * cos(a)).toFloat(); val y = c.y + (rad * sin(a)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close(); drawPath(path, color)
}

private fun DrawScope.night(sky: Color) {
    drawCircle(sky.copy(alpha = 0.12f), u * 0.36f, p(0.3f, 0.45f))
    crescentAt(p(0.22f, 0.32f), u * 0.13f, Color(0xFFFFCF4A))
    star(p(0.42f, 0.22f), u * 0.045f, Color(0xFFFFCF4A))
    star(p(0.1f, 0.6f), u * 0.035f, Color(0xFFFFCF4A))
}

private fun DrawScope.pillow() {
    val tl = p(0.17f, 0.58f)
    drawRoundRect(Color(0xFF4FB3C9), tl, Size(u * 0.62f, u * 0.28f), CornerRadius(u * 0.12f))
    drawRoundRect(Color(0xFF7FCFDF), Offset(tl.x + u * 0.05f, tl.y + u * 0.04f), Size(u * 0.52f, u * 0.1f), CornerRadius(u * 0.05f))
    // z z z
    var x = tl.x + u * 0.5f; var y = tl.y - u * 0.12f; var s = u * 0.06f
    repeat(3) {
        val z = Path().apply { moveTo(x, y); lineTo(x + s, y); lineTo(x, y + s); lineTo(x + s, y + s) }
        drawPath(z, Color(0xFFE76F51), style = Stroke(u * 0.018f, cap = StrokeCap.Round))
        x += s * 1.1f; y -= s * 1.1f; s *= 0.8f
    }
}

private fun DrawScope.mosqueArch() {
    val base = p(0.3f, 0.92f); val w = u * 0.55f; val h = u * 0.62f
    val arch = Path().apply {
        moveTo(base.x - w / 2, base.y)
        lineTo(base.x - w / 2, base.y - h * 0.55f)
        cubicTo(base.x - w / 2, base.y - h * 0.9f, base.x - w * 0.1f, base.y - h * 0.92f, base.x, base.y - h)
        cubicTo(base.x + w * 0.1f, base.y - h * 0.92f, base.x + w / 2, base.y - h * 0.9f, base.x + w / 2, base.y - h * 0.55f)
        lineTo(base.x + w / 2, base.y); close()
    }
    drawPath(arch, Color(0xFF3D4F9F))
    crescentAt(Offset(base.x - w * 0.05f, base.y - h * 0.7f), u * 0.06f, Color(0xFFFFCF4A))
    // two lanterns
    for (dx in floatArrayOf(-0.75f, 0.75f)) {
        val lx = base.x + w * dx
        drawLine(Color(0xFF8A7BCB), Offset(lx, base.y - h), Offset(lx, base.y - h * 0.8f), u * 0.008f)
        drawRoundRect(Color(0xFFFFB84C), Offset(lx - u * 0.025f, base.y - h * 0.8f), Size(u * 0.05f, u * 0.08f), CornerRadius(u * 0.02f))
    }
}

private fun DrawScope.prayerMat() {
    val c = p(0.3f, 0.86f)
    val mat = Path().apply {
        moveTo(c.x - u * 0.38f, c.y + u * 0.06f); lineTo(c.x - u * 0.2f, c.y - u * 0.06f)
        lineTo(c.x + u * 0.38f, c.y - u * 0.06f); lineTo(c.x + u * 0.2f, c.y + u * 0.06f); close()
    }
    drawPath(mat, Color(0xFF1F5FBF))
    drawPath(mat, Color(0xFFFFCF4A), style = Stroke(u * 0.012f))
}

private fun DrawScope.minaret() {
    val x = p(0.28f, 0f).x; val w = u * 0.1f
    drawRect(Color(0xFF2E8B57), Offset(x - w / 2, u * 0.32f), Size(w, u * 0.6f))
    drawRect(Color(0xFF1F6B42), Offset(x - w * 0.8f, u * 0.42f), Size(w * 1.6f, u * 0.04f))
    val cap = Path().apply { moveTo(x - w / 2, u * 0.32f); lineTo(x, u * 0.12f); lineTo(x + w / 2, u * 0.32f); close() }
    drawPath(cap, Color(0xFF1F6B42))
    crescentAt(Offset(x, u * 0.08f), u * 0.035f, Color(0xFFFFCF4A))
}

private fun DrawScope.waves() {
    val c = p(0.3f, 0.4f)
    for (i in 1..3) {
        val r = u * (0.12f + i * 0.08f)
        drawArc(Color(0xFF2E8B57).copy(alpha = 0.9f - i * 0.2f), -40f, 80f, false, Offset(c.x - r, c.y - r), Size(r * 2, r * 2), style = Stroke(u * 0.02f, cap = StrokeCap.Round))
    }
}

private fun DrawScope.mosqueDoor(enter: Boolean) {
    val c = p(0.3f, 0.95f); val w = u * 0.36f; val h = u * 0.62f
    val door = Path().apply {
        moveTo(c.x - w / 2, c.y); lineTo(c.x - w / 2, c.y - h * 0.6f)
        quadraticBezierTo(c.x - w / 2, c.y - h, c.x, c.y - h)
        quadraticBezierTo(c.x + w / 2, c.y - h, c.x + w / 2, c.y - h * 0.6f)
        lineTo(c.x + w / 2, c.y); close()
    }
    drawPath(door, Color(0xFF1F7A5A))
    val inner = Path().apply {
        moveTo(c.x - w * 0.32f, c.y); lineTo(c.x - w * 0.32f, c.y - h * 0.55f)
        quadraticBezierTo(c.x - w * 0.32f, c.y - h * 0.85f, c.x, c.y - h * 0.86f)
        quadraticBezierTo(c.x + w * 0.32f, c.y - h * 0.85f, c.x + w * 0.32f, c.y - h * 0.55f)
        lineTo(c.x + w * 0.32f, c.y); close()
    }
    drawPath(inner, Color(0xFFFFE7A3))
    // arrow in or out
    val ay = c.y - h * 0.3f; val dir = if (enter) 1 else -1
    val ax = c.x + dir * w * 0.95f
    drawLine(Color(0xFFE76F51), Offset(ax, ay), Offset(ax - dir * u * 0.14f, ay), u * 0.025f, StrokeCap.Round)
    val head = Path().apply {
        moveTo(ax - dir * u * 0.14f, ay); lineTo(ax - dir * u * 0.08f, ay - u * 0.05f); lineTo(ax - dir * u * 0.08f, ay + u * 0.05f); close()
    }
    drawPath(head, Color(0xFFE76F51))
}

private fun DrawScope.drops() {
    fun drop(c: Offset, r: Float, color: Color) {
        val d = Path().apply {
            moveTo(c.x, c.y - r * 1.7f)
            cubicTo(c.x + r * 1.2f, c.y - r * 0.4f, c.x + r, c.y + r, c.x, c.y + r)
            cubicTo(c.x - r, c.y + r, c.x - r * 1.2f, c.y - r * 0.4f, c.x, c.y - r * 1.7f)
            close()
        }
        drawPath(d, color)
        drawCircle(Color.White.copy(alpha = 0.6f), r * 0.25f, Offset(c.x - r * 0.35f, c.y - r * 0.1f))
    }
    drop(p(0.3f, 0.55f), u * 0.17f, Color(0xFF2B9AE0))
    drop(p(0.12f, 0.35f), u * 0.08f, Color(0xFF64B8EE))
    drop(p(0.45f, 0.3f), u * 0.06f, Color(0xFF64B8EE))
}

private fun DrawScope.house(enter: Boolean) {
    val c = p(0.3f, 0.92f); val w = u * 0.5f; val h = u * 0.34f
    drawRect(Color(0xFF8E7CC3), Offset(c.x - w / 2, c.y - h), Size(w, h))
    val roof = Path().apply { moveTo(c.x - w * 0.62f, c.y - h); lineTo(c.x, c.y - h - u * 0.26f); lineTo(c.x + w * 0.62f, c.y - h); close() }
    drawPath(roof, Color(0xFF5B4A9E))
    drawRoundRect(if (enter) Color(0xFFFFCF4A) else Color(0xFF5B4A9E), Offset(c.x - u * 0.06f, c.y - u * 0.22f), Size(u * 0.12f, u * 0.22f), CornerRadius(u * 0.05f, u * 0.05f))
    drawRect(Color(0xFFFFE7A3), Offset(c.x + w * 0.22f, c.y - h * 0.75f), Size(u * 0.08f, u * 0.08f))
}

private fun DrawScope.dates() {
    val c = p(0.3f, 0.7f)
    drawOval(Color(0xFFFFFFFF), Offset(c.x - u * 0.36f, c.y - u * 0.08f), Size(u * 0.72f, u * 0.2f))
    drawOval(Color(0xFFE9D9C4), Offset(c.x - u * 0.3f, c.y - u * 0.05f), Size(u * 0.6f, u * 0.13f))
    for ((dx, dy) in listOf(-0.15f to -0.02f, 0f to -0.06f, 0.15f to -0.02f, -0.07f to -0.12f, 0.08f to -0.12f)) {
        drawOval(Color(0xFF8B4513), Offset(c.x + u * dx - u * 0.06f, c.y + u * dy - u * 0.035f), Size(u * 0.12f, u * 0.07f))
        drawOval(Color(0xFFA0522D), Offset(c.x + u * dx - u * 0.04f, c.y + u * dy - u * 0.03f), Size(u * 0.05f, u * 0.025f))
    }
    // a cup beside the plate
    drawRoundRect(Color(0xFFF4A259), Offset(c.x + u * 0.28f, c.y - u * 0.28f), Size(u * 0.12f, u * 0.16f), CornerRadius(u * 0.03f))
}

private fun DrawScope.suitcase() {
    val c = p(0.3f, 0.62f); val w = u * 0.42f; val h = u * 0.32f
    drawRoundRect(Color(0xFF3A7BD5), Offset(c.x - w / 2, c.y - h / 2), Size(w, h), CornerRadius(u * 0.05f))
    drawRoundRect(Color(0xFF2558A8), Offset(c.x - u * 0.08f, c.y - h / 2 - u * 0.08f), Size(u * 0.16f, u * 0.1f), CornerRadius(u * 0.03f), style = Stroke(u * 0.025f))
    drawRect(Color(0xFFFFCF4A), Offset(c.x - w * 0.3f, c.y - h / 2), Size(u * 0.03f, h))
    drawRect(Color(0xFFFFCF4A), Offset(c.x + w * 0.22f, c.y - h / 2), Size(u * 0.03f, h))
}

private fun DrawScope.book() {
    val c = p(0.3f, 0.66f); val w = u * 0.26f; val h = u * 0.3f
    // light rays
    for (i in 0..6) {
        val a = PI + (i + 0.5) * PI / 7
        drawLine(Color(0xFFFFCF4A).copy(alpha = 0.7f), Offset(c.x, c.y - h * 0.4f),
            Offset(c.x + (u * 0.42f * cos(a)).toFloat(), c.y - h * 0.4f + (u * 0.42f * sin(a)).toFloat()), u * 0.015f, StrokeCap.Round)
    }
    for (side in intArrayOf(-1, 1)) {
        val page = Path().apply {
            moveTo(c.x, c.y - h * 0.4f)
            quadraticBezierTo(c.x + side * w * 0.5f, c.y - h * 0.6f, c.x + side * w, c.y - h * 0.45f)
            lineTo(c.x + side * w, c.y + h * 0.45f)
            quadraticBezierTo(c.x + side * w * 0.5f, c.y + h * 0.3f, c.x, c.y + h * 0.5f)
            close()
        }
        drawPath(page, Color.White)
        drawPath(page, Color(0xFF6A55B5), style = Stroke(u * 0.015f))
        for (k in 1..3) drawLine(Color(0xFFBDB2E6), Offset(c.x + side * w * 0.2f, c.y - h * 0.3f + k * h * 0.17f), Offset(c.x + side * w * 0.8f, c.y - h * 0.32f + k * h * 0.17f), u * 0.01f)
    }
}

private fun DrawScope.heart() {
    val c = p(0.3f, 0.55f); val s = u * 0.22f
    val path = Path().apply {
        moveTo(c.x, c.y + s * 0.9f)
        cubicTo(c.x - s * 1.6f, c.y - s * 0.1f, c.x - s * 0.8f, c.y - s * 1.3f, c.x, c.y - s * 0.4f)
        cubicTo(c.x + s * 0.8f, c.y - s * 1.3f, c.x + s * 1.6f, c.y - s * 0.1f, c.x, c.y + s * 0.9f)
        close()
    }
    drawPath(path, Color(0xFFE5577E))
    crescentAt(Offset(c.x, c.y - s * 0.1f), s * 0.35f, Color(0xFFFFF0F4))
    star(p(0.48f, 0.25f), u * 0.04f, Color(0xFFFFCF4A))
}

private fun DrawScope.beads() {
    val c = p(0.3f, 0.48f); val r = u * 0.24f
    for (i in 0 until 16) {
        val a = -PI / 2 + i * 2 * PI / 17
        drawCircle(Brush.radialGradient(listOf(Color(0xFFF6DDA0), Color(0xFFC49A45)), Offset(c.x + (r * cos(a)).toFloat(), c.y + (r * sin(a)).toFloat()), u * 0.05f),
            u * 0.04f, Offset(c.x + (r * cos(a)).toFloat(), c.y + (r * sin(a)).toFloat()))
    }
    rotate(0f, c) { drawRoundRect(Color(0xFF175E43), Offset(c.x - u * 0.025f, c.y + r), Size(u * 0.05f, u * 0.14f), CornerRadius(u * 0.02f)) }
}
