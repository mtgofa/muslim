package com.mtgofa.muslem.ui

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
fun TasbeehScreen(onBack: () -> Unit) {
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
                if (Build.VERSION.SDK_INT >= 26) vibrator?.vibrate(VibrationEffect.createOneShot(220, VibrationEffect.DEFAULT_AMPLITUDE))
            } else view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
        when (Settings.tasbeehSound) {
            1 -> if (roundDone || (target == 0 && hundred)) tone?.startTone(ToneGenerator.TONE_PROP_ACK, 250)
            2 -> if (hundred) tone?.startTone(ToneGenerator.TONE_PROP_ACK, 250)
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("السبحة", onBack)
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
            Spacer(Modifier.weight(1f))

        // the misbaha: a ring of beads that fill with each count, tap anywhere on it
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(300.dp)
                .clip(CircleShape)
                .clickable(remember { MutableInteractionSource() }, ripple(color = Palette.Emerald)) { tap() },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2, size.height / 2)
                val r = size.minDimension / 2 - 18.dp.toPx()
                drawCircle(Brush.radialGradient(listOf(Color(0xFFFFFFFF), Color(0xFFEFF6F1)), center = c, radius = r), radius = r - 18.dp.toPx(), center = c)
                // thread
                drawCircle(Color(0x33175E43), radius = r, center = c, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                val filled = (progress * BEADS).toInt()
                val bead = 9.5.dp.toPx()
                for (i in 0 until BEADS) {
                    // start at the top and go clockwise
                    val a = -PI / 2 + i * 2 * PI / BEADS
                    val p = Offset(c.x + (r * cos(a)).toFloat(), c.y + (r * sin(a)).toFloat())
                    val on = i < filled
                    drawCircle(
                        Brush.radialGradient(
                            if (on) listOf(Color(0xFFF6DDA0), Color(0xFFC49A45)) else listOf(Color(0xFFE7F1EA), Color(0xFFB9D3C3)),
                            center = Offset(p.x - bead * 0.3f, p.y - bead * 0.3f), radius = bead * 1.4f,
                        ),
                        radius = bead, center = p,
                    )
                }
                // the long "imam" bead at the bottom
                val bottom = Offset(c.x, c.y + r + 2.dp.toPx())
                drawRoundRect(
                    Brush.verticalGradient(listOf(Color(0xFF2A7A59), Color(0xFF175E43)), startY = bottom.y, endY = bottom.y + 22.dp.toPx()),
                    topLeft = Offset(bottom.x - 6.dp.toPx(), bottom.y), size = Size(12.dp.toPx(), 22.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(count.toString(), fontSize = 64.sp, fontWeight = FontWeight.Bold, fontFamily = Plex, color = Palette.EmeraldDark)
                if (target > 0) {
                    Text(
                        "${arabicDigits(inRound.takeIf { it > 0 || count == 0 } ?: ring)} من ${arabicDigits(target)}",
                        style = MaterialTheme.typography.titleSmall, color = Palette.Muted,
                    )
                    Text("الدورة ${arabicDigits(round)}", style = MaterialTheme.typography.bodySmall, color = Palette.Gold)
                } else Text("عدّ حرّ", style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("اضغط على السبحة للتسبيح", style = MaterialTheme.typography.bodySmall, color = Palette.Muted, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.weight(1f))
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
                Spacer(Modifier.width(68.dp))
                TasbeehButton(count, progress, onTap = ::tap)
                Spacer(Modifier.width(10.dp))
                TargetBox(target, Modifier.width(58.dp))
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
            modifier = Modifier.align(Alignment.CenterHorizontally).navigationBarsPadding().padding(vertical = 14.dp),
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
            .size(224.dp)
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
            val face = r * 0.66f
            drawCircle(Brush.radialGradient(listOf(Color(0xFF2A8A63), Color(0xFF0E4A37)), center = c, radius = face), radius = face, center = c)
            // progress of the current round around the face
            val ring = face + 7.dp.toPx()
            drawArc(
                Color(0x33FFFFFF), 0f, 360f, false, topLeft = Offset(c.x - ring, c.y - ring), size = Size(ring * 2, ring * 2),
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
        Text(count.toString(), fontSize = 58.sp, fontWeight = FontWeight.Bold, fontFamily = Plex, color = Color.White)
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
