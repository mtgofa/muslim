package com.mtgofa.muslem.ui

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtgofa.muslem.data.Settings
import com.mtgofa.muslem.data.Tasbeeh
import com.mtgofa.muslem.prayer.arabicDigits
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val BEADS = 33

@Composable
fun TasbeehScreen(onBack: (() -> Unit)? = null) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val changes by Settings.changes.collectAsState()
    var phraseId by remember { mutableStateOf(Settings.tasbeehPhrase) }
    val phrase = Tasbeeh.byId(phraseId)
    var count by remember(phraseId) { mutableIntStateOf(Settings.tasbeehCount(phraseId)) }
    var total by remember { mutableLongStateOf(Settings.tasbeehTotal) }
    var confirmReset by remember { mutableStateOf(false) }
    var style by remember { mutableIntStateOf(Settings.tasbeehStyle) }
    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 70) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { tone?.release() } }
    val vibrator = remember { ctx.getSystemService(Vibrator::class.java) }

    val target = phrase.target
    // free counting shows a ring of 100
    val ring = if (target > 0) target else 100
    val inRound = count % ring
    val round = count / ring + 1
    val progress = if (count > 0 && inRound == 0) 1f else inRound / ring.toFloat()

    fun tap() {
        count += 1; total += 1
        Settings.setTasbeehCount(phraseId, count); Settings.tasbeehTotal = total
        val roundDone = target > 0 && count % target == 0
        val hundred = count % 100 == 0
        if (Settings.tasbeehVibrate) {
            if (roundDone || hundred) {
                com.mtgofa.muslem.data.Haptics.buzz(ctx, 220)
            } else view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
        when (Settings.tasbeehSound) {
            1 -> if (roundDone || (target == 0 && hundred)) tone?.startTone(ToneGenerator.TONE_PROP_ACK, 250)
            2 -> if (hundred) tone?.startTone(ToneGenerator.TONE_PROP_ACK, 250)
        }
    }

    Column(Modifier.fillMaxSize()) {
        // as a tab the bottom bar names the screen; opened on its own it gets a title and back
        if (onBack != null) TopBar("السبحة", onBack) else Spacer(Modifier.statusBarsPadding().height(12.dp))
        // phrase chooser
        val listState = rememberLazyListState(Tasbeeh.phrases.indexOfFirst { it.id == phraseId }.coerceAtLeast(0))
        LazyRow(state = listState, contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Tasbeeh.phrases, key = { it.id }) { p ->
                val on = p.id == phraseId
                Text(
                    p.text.split('،').first().take(28),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) Color.White else Palette.EmeraldDark,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (on) Palette.Emerald else Palette.EmeraldSoft)
                        .clickable { phraseId = p.id; Settings.tasbeehPhrase = p.id }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
        Box(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Segmented(listOf(1, 0), style, { if (it == 1) "زر" else "سبحة" }) { style = it; Settings.tasbeehStyle = it }
        }
        Text(
            phrase.text, textAlign = TextAlign.Center, fontSize = if (phrase.text.length > 40) 20.sp else 26.sp,
            lineHeight = 38.sp, fontWeight = FontWeight.Bold, fontFamily = Plex, color = Palette.EmeraldDark,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        )
        if (style == 0) {
            // a string of large beads: a tap or a downward swipe moves one bead across
            BeadString(count, Modifier.weight(1f).fillMaxWidth(), onCount = ::tap)
            Text(
                if (target > 0) "${arabicDigits(inRound.takeIf { it > 0 || count == 0 } ?: ring)} من ${arabicDigits(target)} · الدورة ${arabicDigits(round)}" else "عدّ حرّ",
                style = MaterialTheme.typography.bodyMedium, color = Palette.Muted,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 6.dp),
            )
        } else {
            // button style: big and low on the screen, within reach of the thumb
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.fillMaxWidth().padding(bottom = 18.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // in RTL the first child sits on the right: keep a matching gap there so the
                // button stays centred, and the target box lands on its left
                Spacer(Modifier.width(56.dp))
                TasbeehButton(count, progress, onTap = ::tap)
                Spacer(Modifier.width(8.dp))
                TargetBox(target, Modifier.width(56.dp))
            }
        }

        // options
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val sound = remember(changes) { Settings.tasbeehSound }
            Option(Icons.Rounded.Vibration, "الاهتزاز", if (Settings.tasbeehVibrate) "مفعّل" else "متوقف", Settings.tasbeehVibrate, Modifier.weight(1f)) {
                Settings.tasbeehVibrate = !Settings.tasbeehVibrate
            }
            Option(
                if (sound == 0) Icons.Rounded.MusicOff else Icons.Rounded.MusicNote, "الصوت",
                when (sound) { 0 -> "متوقف"; 1 -> "عند إتمام العدد"; else -> "كل ١٠٠" }, sound != 0, Modifier.weight(1f),
            ) { Settings.tasbeehSound = (sound + 1) % 3 }
            Option(Icons.Rounded.RestartAlt, "تصفير", "العدّاد", false, Modifier.weight(1f)) { confirmReset = true }
        }
        Text(
            "إجمالي تسبيحاتك: ${arabicDigits(total.toString())}",
            style = MaterialTheme.typography.bodySmall, color = Palette.Muted,
            modifier = Modifier.align(Alignment.CenterHorizontally)
                .then(if (onBack != null) Modifier.navigationBarsPadding() else Modifier).padding(vertical = 12.dp),
        )
    }

    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text("تصفير العدّاد") },
        text = { Text("سيعود عدّاد «${phrase.text.split('،').first()}» إلى الصفر.") },
        confirmButton = { TextButton(onClick = { count = 0; Settings.setTasbeehCount(phraseId, 0); confirmReset = false }) { Text("تصفير") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("إلغاء") } },
    )
}

@Composable
private fun Option(icon: ImageVector, title: String, state: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (on) Palette.EmeraldSoft else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = if (on) Palette.Emerald else Palette.Muted)
        Spacer(Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.labelLarge)
        Text(state, style = MaterialTheme.typography.bodySmall, color = Palette.Muted, maxLines = 1)
    }
}

/** The big tap target: an eight-point star frame with the count in the middle. */
@Composable
private fun TasbeehButton(count: Int, progress: Float, onTap: () -> Unit) {
    Box(
        Modifier
            .size(268.dp)
            .clip(CircleShape)
            .clickable(remember { MutableInteractionSource() }, ripple(color = Palette.Gold)) { onTap() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2, size.height / 2)
            val r = size.minDimension / 2 - 4.dp.toPx()
            fun star(rr: Float, rot: Double): androidx.compose.ui.graphics.Path = androidx.compose.ui.graphics.Path().apply {
                for (i in 0 until 16) {
                    val a = rot + i * PI / 8
                    val rad = if (i % 2 == 0) rr else rr * 0.86f
                    val x = c.x + (rad * cos(a)).toFloat(); val y = c.y + (rad * sin(a)).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            // gold star frame, emerald star, then the face
            drawPath(star(r, -PI / 2), Brush.linearGradient(listOf(Color(0xFFE7CF95), Color(0xFFB8893A))))
            drawPath(star(r * 0.93f, -PI / 2), Brush.linearGradient(listOf(Color(0xFF1E7D57), Color(0xFF0B3A2C))))
            drawPath(star(r * 0.86f, -PI / 2), Color(0xFFC49A45), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
            val face = r * 0.68f
            drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFFF1F6F2)), center = c, radius = face), radius = face, center = c)
            // progress of the current round around the face
            val ring = face + 7.dp.toPx()
            drawArc(
                Color(0x40FFFFFF), 0f, 360f, false, topLeft = Offset(c.x - ring, c.y - ring), size = Size(ring * 2, ring * 2),
                style = androidx.compose.ui.graphics.drawscope.Stroke(4.dp.toPx()),
            )
            drawArc(
                Color(0xFFF2D59B), -90f, 360f * progress, false, topLeft = Offset(c.x - ring, c.y - ring), size = Size(ring * 2, ring * 2),
                style = androidx.compose.ui.graphics.drawscope.Stroke(4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
            // small gold stars at the eight points
            for (i in 0 until 8) {
                val a = -PI / 2 + i * PI / 4
                drawCircle(Color(0xFFF2D59B), radius = 2.5.dp.toPx(), center = Offset(c.x + (r * 0.8f * cos(a)).toFloat(), c.y + (r * 0.8f * sin(a)).toFloat()))
            }
        }
        Text(count.toString(), fontSize = 76.sp, fontWeight = FontWeight.Bold, fontFamily = Plex, color = Palette.EmeraldDark)
    }
}

/** Small box beside the button with the count of one round. */
@Composable
private fun TargetBox(target: Int, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.GoldSoft)
            .border(1.5.dp, Palette.Gold, RoundedCornerShape(16.dp))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(if (target > 0) target.toString() else "∞", fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = Plex, color = Color(0xFF8A5E14))
        Text("العدد", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8A5E14))
    }
}

/**
 * Beads strung on a curve across the screen. Counted beads gather at the bottom, the rest wait
 * at the top; each count slides the next bead across the gap with a spring.
 */
@Composable
private fun BeadString(count: Int, modifier: Modifier, onCount: () -> Unit) {
    val anim = remember { androidx.compose.animation.core.Animatable(count.toFloat()) }
    LaunchedEffect(count) {
        if (kotlin.math.abs(anim.value - count) > 3) anim.snapTo(count - 1f)
        anim.animateTo(count.toFloat(), androidx.compose.animation.core.spring(dampingRatio = 0.7f, stiffness = 420f))
    }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val stepPx = with(density) { 70.dp.toPx() }
    var dragged by remember { mutableFloatStateOf(0f) }
    Box(
        modifier
            .pointerInput(Unit) { detectTapGestures { onCount() } }
            .pointerInput(Unit) {
                detectVerticalDragGestures(onDragEnd = { dragged = 0f }) { change, dy ->
                    change.consume()
                    if (dy > 0) {
                        dragged += dy
                        while (dragged >= stepPx) { dragged -= stepPx; onCount() }
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val bead = minOf(w, h * 0.9f) * 0.072f
            // the string: a curve from the upper left, sweeping down to the lower right,
            // sampled by arc length so beads sit evenly along it
            val p0 = Offset(-w * 0.08f, h * 0.14f); val p1 = Offset(w * 0.62f, h * 0.1f); val p2 = Offset(w * 0.92f, h * 1.02f)
            val n = 240
            val pts = Array(n + 1) { i ->
                val t = i / n.toFloat(); val u = 1 - t
                Offset(u * u * p0.x + 2 * u * t * p1.x + t * t * p2.x, u * u * p0.y + 2 * u * t * p1.y + t * t * p2.y)
            }
            val acc = FloatArray(n + 1)
            for (i in 1..n) acc[i] = acc[i - 1] + (pts[i] - pts[i - 1]).getDistance()
            val total = acc[n]
            fun at(d: Float): Offset {
                val x = d.coerceIn(0f, total)
                var i = acc.indexOfFirst { it >= x }.coerceAtLeast(1)
                if (i < 0) i = n
                val f = (x - acc[i - 1]) / (acc[i] - acc[i - 1]).coerceAtLeast(0.001f)
                return pts[i - 1] + (pts[i] - pts[i - 1]) * f
            }
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(pts[0].x, pts[0].y); for (i in 1..n) lineTo(pts[i].x, pts[i].y)
            }
            drawPath(path, Color(0xFF2B2B2B), style = androidx.compose.ui.graphics.drawscope.Stroke(2.5.dp.toPx()))

            // waiting beads end at gapTop, counted beads start at gapBottom
            val spacing = bead * 2.08f
            val gapTop = total * 0.36f; val gapBottom = total * 0.66f
            val v = anim.value
            val first = kotlin.math.floor(v).toInt() - 12
            for (k in first..first + 26) {
                val d = when {
                    k >= v -> gapTop - (k - v) * spacing
                    k <= v - 1 -> gapBottom + (v - 1 - k) * spacing
                    else -> gapTop + (gapBottom - gapTop) * (v - k)
                }
                if (d < -spacing || d > total + spacing) continue
                val p = at(d)
                val dark = k % 2 == 0
                drawCircle(
                    Brush.radialGradient(
                        if (dark) listOf(Color(0xFF2A9A70), Color(0xFF0B4A37)) else listOf(Color(0xFFF6DDA0), Color(0xFFC49A45)),
                        center = Offset(p.x - bead * 0.35f, p.y - bead * 0.35f), radius = bead * 1.5f,
                    ),
                    radius = bead, center = p,
                )
            }
        }
        Text(
            count.toString(), fontSize = 72.sp, fontWeight = FontWeight.Bold, fontFamily = Plex, color = Palette.EmeraldDark,
            modifier = Modifier.align(androidx.compose.ui.AbsoluteAlignment.TopRight).padding(top = 40.dp, end = 24.dp),
        )
        Text(
            "اسحب لأسفل أو اضغط",
            style = MaterialTheme.typography.bodySmall, color = Palette.Muted,
            modifier = Modifier.align(androidx.compose.ui.AbsoluteAlignment.TopLeft).padding(top = 6.dp)
                .then(Modifier.padding(horizontal = 20.dp)),
        )
    }
}
