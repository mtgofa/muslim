package com.mtgofa.muslem.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtgofa.muslem.prayer.Prayer
import kotlin.random.Random

/** The sky to paint, picked from the prayer period we are in. */
enum class SkyPeriod(val top: Color, val bottom: Color, val glow: Color) {
    DAWN(Color(0xFF1B2A55), Color(0xFFE8A27A), Color(0xFFFFC98A)),
    DAY(Color(0xFF2D7FC1), Color(0xFFA8D8EE), Color(0xFFFFF4C7)),
    AFTERNOON(Color(0xFF3B7DB3), Color(0xFFF2C779), Color(0xFFFFE2A3)),
    SUNSET(Color(0xFF3A2E6B), Color(0xFFF08A5D), Color(0xFFFFB27A)),
    NIGHT(Color(0xFF070F24), Color(0xFF1B2D52), Color(0xFF9FB4E0));

    val night get() = this == NIGHT || this == DAWN

    companion object {
        /** [current] is the last prayer time that has passed today (null = before Fajr). */
        fun of(current: Prayer?): SkyPeriod = when (current) {
            null, Prayer.ISHA -> NIGHT
            Prayer.FAJR -> DAWN
            Prayer.SUNRISE, Prayer.DHUHR -> DAY
            Prayer.ASR -> AFTERNOON
            Prayer.MAGHRIB -> SUNSET
        }
    }
}

/** Sky, sun or moon and stars, and a mosque skyline along the bottom. */
@Composable
fun SkyScene(period: SkyPeriod, modifier: Modifier = Modifier) {
    val stars = remember { List(60) { Random(it * 7919).let { r -> Triple(r.nextFloat(), r.nextFloat() * 0.6f, r.nextFloat()) } } }
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(period.top, period.bottom)))
        if (period.night) {
            for ((x, y, s) in stars) {
                drawCircle(Color.White.copy(alpha = 0.35f + s * 0.5f), radius = 0.8.dp.toPx() + s * 1.2.dp.toPx(), center = Offset(x * size.width, y * size.height))
            }
            if (period == SkyPeriod.NIGHT) crescent(Offset(size.width * 0.86f, size.height * 0.36f), 24.dp.toPx())
        } else {
            // the sun with a soft halo, lower in the sky toward evening
            val y = when (period) { SkyPeriod.DAY -> 0.32f; SkyPeriod.AFTERNOON -> 0.44f; else -> 0.6f }
            val c = Offset(size.width * 0.86f, size.height * y)
            drawCircle(Brush.radialGradient(listOf(period.glow.copy(alpha = 0.7f), Color.Transparent), center = c, radius = 90.dp.toPx()), radius = 90.dp.toPx(), center = c)
            drawCircle(period.glow, radius = 18.dp.toPx(), center = c)
        }
        // glow along the horizon
        drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, period.glow.copy(alpha = 0.35f)), startY = size.height * 0.55f, endY = size.height),
            topLeft = Offset(0f, size.height * 0.55f), size = Size(size.width, size.height * 0.45f),
        )
        skyline(Color(0xE60A2A21))
    }
}

private fun DrawScope.crescent(c: Offset, r: Float) {
    drawCircle(Brush.radialGradient(listOf(Color(0x55FFF3C4), Color.Transparent), center = c, radius = r * 3), radius = r * 3, center = c)
    val moon = Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(c, r))
    }
    val bite = Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(Offset(c.x + r * 0.45f, c.y - r * 0.2f), r * 0.9f))
    }
    val shape = Path().apply { op(moon, bite, androidx.compose.ui.graphics.PathOperation.Difference) }
    drawPath(shape, Color(0xFFFFF1C1))
}

/** Domes and minarets across the bottom of the scene. */
private fun DrawScope.skyline(color: Color) {
    val w = size.width; val h = size.height
    val base = h
    val p = Path()
    fun rect(l: Float, t: Float, r: Float) { p.addRect(androidx.compose.ui.geometry.Rect(l * w, t, r * w, base)) }
    fun dome(cx: Float, rw: Float, top: Float) {
        // onion dome sitting on a drum
        val x = cx * w; val rr = rw * w
        p.addRect(androidx.compose.ui.geometry.Rect(x - rr, top + rr * 0.9f, x + rr, base))
        p.moveTo(x - rr, top + rr * 0.95f)
        p.cubicTo(x - rr * 1.1f, top + rr * 0.1f, x - rr * 0.2f, top - rr * 0.3f, x, top - rr * 0.75f)
        p.cubicTo(x + rr * 0.2f, top - rr * 0.3f, x + rr * 1.1f, top + rr * 0.1f, x + rr, top + rr * 0.95f)
        p.close()
        // finial
        p.addRect(androidx.compose.ui.geometry.Rect(x - rr * 0.04f, top - rr * 1.15f, x + rr * 0.04f, top - rr * 0.7f))
        p.addOval(androidx.compose.ui.geometry.Rect(Offset(x, top - rr * 1.2f), rr * 0.09f))
    }
    fun minaret(cx: Float, top: Float) {
        val x = cx * w; val mw = w * 0.022f
        p.addRect(androidx.compose.ui.geometry.Rect(x - mw / 2, top, x + mw / 2, base))
        // balconies
        for (k in 1..2) {
            val by = top + (base - top) * 0.22f * k
            p.addRect(androidx.compose.ui.geometry.Rect(x - mw * 0.9f, by, x + mw * 0.9f, by + mw * 0.35f))
        }
        // pointed cap
        p.moveTo(x - mw / 2, top); p.lineTo(x, top - mw * 2.4f); p.lineTo(x + mw / 2, top); p.close()
    }
    // low wall across the whole width
    rect(0f, base - h * 0.1f, 1f)
    dome(0.5f, 0.11f, base - h * 0.42f)
    dome(0.33f, 0.06f, base - h * 0.27f)
    dome(0.67f, 0.06f, base - h * 0.27f)
    dome(0.12f, 0.045f, base - h * 0.2f)
    dome(0.88f, 0.045f, base - h * 0.2f)
    minaret(0.24f, base - h * 0.55f)
    minaret(0.76f, base - h * 0.55f)
    drawPath(p, color)
}

/**
 * Home header: the sky scene with the next prayer in a ring at the centre. The ring fills as
 * the time between the previous and the next prayer runs out.
 */
@Composable
fun PrayerHeader(
    period: SkyPeriod,
    location: String,
    hijri: String,
    date: String,
    nextName: String,
    nextTime: String,
    remaining: String,
    progress: Float,
    onLocation: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)),
    ) {
        SkyScene(period, Modifier.matchParentSize())
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onLocation)
                        .background(Color(0x33000000))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.LocationOn, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(location, color = Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(hijri, color = Color.White, style = MaterialTheme.typography.titleSmall)
                    Text(date, color = Color(0xD9FFFFFF), style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.size(232.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 7.dp.toPx()
                    val inset = stroke / 2
                    drawCircle(Color(0x40000000), radius = size.minDimension / 2 - inset)
                    drawArc(
                        Color(0x40FFFFFF), 0f, 360f, false,
                        topLeft = Offset(inset, inset), size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(stroke),
                    )
                    drawArc(
                        Color(0xFFF2D59B), -90f, 360f * progress.coerceIn(0f, 1f), false,
                        topLeft = Offset(inset, inset), size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("الصلاة القادمة", color = Color(0xD9FFFFFF), style = MaterialTheme.typography.labelLarge)
                    Text(nextName, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = Plex)
                    // hours, minutes and seconds set apart so the clock reads at a glance
                    // always left-to-right (h:mm:ss), with thin gaps around the colons
                    Text(
                        remaining.replace(":", "\u2009:\u2009"), color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold,
                        fontFamily = Plex, letterSpacing = 1.sp, maxLines = 1,
                        style = androidx.compose.ui.text.TextStyle(textDirection = androidx.compose.ui.text.style.TextDirection.Ltr),
                    )
                    Text(nextTime, color = Color(0xFFF2D59B), style = MaterialTheme.typography.titleSmall)
                }
            }
            Spacer(Modifier.height(36.dp))
        }
    }
}
