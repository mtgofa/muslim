package com.mtgofa.muslem.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtgofa.muslem.data.Settings
import com.mtgofa.muslem.data.Wird
import com.mtgofa.muslem.prayer.arabicDigits
import com.mtgofa.muslem.quran.QuranDb
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val AR_DAYS_W = listOf("الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد")
private val AR_MONTHS_W = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")
fun wirdDate(d: LocalDate) = "${AR_DAYS_W[d.dayOfWeek.value - 1]} ${arabicDigits(d.dayOfMonth)} ${AR_MONTHS_W[d.monthValue - 1]}"

private fun timeLabel(t: LocalTime) = t.format(DateTimeFormatter.ofPattern("h:mm", Locale.US)) + if (t.hour < 12) " ص" else " م"

@Composable
fun WirdScreen(onBack: () -> Unit, onRead: (Int) -> Unit) {
    val ctx = LocalContext.current
    val changes by Wird.changes.collectAsState()
    var editing by remember { mutableStateOf(false) }
    key(changes) {
        Column(Modifier.fillMaxSize()) {
            TopBar("الورد اليومي", onBack)
            if (!Wird.active || editing) WirdSetup(editing = editing && Wird.active) { editing = false }
            else WirdProgress(onRead = { onRead(Wird.next.coerceAtMost(Wird.LAST_PAGE)) }, onEdit = { editing = true }) {
                Wird.stop(ctx)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WirdSetup(editing: Boolean, onDone: () -> Unit) {
    val ctx = LocalContext.current
    var perDay by remember { mutableIntStateOf(if (editing) Wird.pagesPerDay else 5) }
    var fromStart by remember { mutableStateOf(true) }
    var remindOn by remember { mutableStateOf(if (editing) Wird.reminder != null else true) }
    var remindAt by remember { mutableStateOf(Wird.reminder ?: LocalTime.of(20, 0)) }
    var picking by remember { mutableStateOf(false) }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    val from = when {
        editing -> Wird.next
        fromStart -> 1
        else -> Settings.lastPage
    }
    val remaining = Wird.LAST_PAGE - from + 1
    val (days, end) = Wird.estimate(remaining, perDay)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Text(
            if (editing) "تعديل الورد" else "حدّد وردك اليومي من القرآن، ويتابع التطبيق تقدّمك تلقائياً كلما قلبت الصفحة.",
            style = MaterialTheme.typography.bodyMedium, color = Palette.Muted, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        )
        Spacer(Modifier.height(10.dp))
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("عدد الأوجه يومياً", style = MaterialTheme.typography.titleMedium)
                    Text("الجزء = ٢٠ وجهاً تقريباً", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
                }
                Stepper(perDay, 1..60, format = { arabicDigits(it) }) { perDay = it }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((n, label) in listOf(2 to "٢", 5 to "٥", 10 to "١٠", 20 to "جزء")) {
                    val on = perDay == n
                    Text(
                        label, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge,
                        color = if (on) Color.White else Palette.EmeraldDark,
                        modifier = Modifier.weight(1f).background(if (on) Palette.Emerald else Palette.EmeraldSoft, RoundedCornerShape(12.dp))
                            .clickableNoRipple { perDay = n }.padding(vertical = 8.dp),
                    )
                }
            }
        }
        if (!editing) {
            Spacer(Modifier.height(10.dp))
            Card {
                Text("البداية", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                val page = Settings.lastPage
                val sura = remember(page) { QuranDb.page(page).suras.firstOrNull()?.let { QuranDb.surah(it).name } ?: "" }
                Segmented(listOf(true, false), fromStart, { if (it) "من أول المصحف" else "من صفحة ${arabicDigits(page)} ($sura)" }) { fromStart = it }
            }
        }
        Spacer(Modifier.height(10.dp))
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("تذكير يومي", style = MaterialTheme.typography.titleMedium)
                    Text(if (remindOn) "الساعة ${timeLabel(remindAt)} — اضغط للتغيير" else "بدون تذكير", style = MaterialTheme.typography.bodySmall, color = Palette.Muted,
                        modifier = Modifier.clickableNoRipple { if (remindOn) picking = true })
                }
                Switch(remindOn, { remindOn = it }, colors = SwitchDefaults.colors(checkedTrackColor = Palette.Emerald))
            }
        }
        Spacer(Modifier.height(14.dp))
        // the estimate, live
        Column(
            Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF14936F), Color(0xFF0A5A48))), RoundedCornerShape(22.dp)).padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("ستختم القرآن بإذن الله خلال", color = Color(0xD9FFFFFF), style = MaterialTheme.typography.bodyMedium)
            Text("${arabicDigits(days)} ${if (days in 3..10) "أيام" else "يوماً"}", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold, fontFamily = Plex)
            Text("يوم ${wirdDate(end)}", color = Color(0xFFF2D59B), style = MaterialTheme.typography.titleMedium)
            Text("${Wird.pages(remaining)} متبقية", color = Color(0xB3FFFFFF), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (remindOn && Build.VERSION.SDK_INT >= 33) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
                if (editing) Wird.update(ctx, perDay, if (remindOn) remindAt else null)
                else Wird.start(ctx, perDay, from, if (remindOn) remindAt else null)
                onDone()
            },
            modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Palette.Emerald),
        ) { Text(if (editing) "حفظ" else "ابدأ الختمة") }
        if (editing) TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("إلغاء") }
        Spacer(Modifier.height(24.dp))
    }

    if (picking) {
        val state = rememberTimePickerState(remindAt.hour, remindAt.minute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { picking = false },
            confirmButton = { TextButton(onClick = { remindAt = LocalTime.of(state.hour, state.minute); picking = false }) { Text("تم") } },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("إلغاء") } },
            text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state) } },
        )
    }
}

@Composable
private fun WirdProgress(onRead: () -> Unit, onEdit: () -> Unit, onStop: () -> Unit) {
    var confirmStop by remember { mutableStateOf(false) }
    val total = Wird.totalPages; val read = Wird.readPages
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(8.dp))
        Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val st = 14.dp.toPx(); val inset = st / 2
                drawArc(Palette.EmeraldSoft, 0f, 360f, false, Offset(inset, inset), Size(size.width - st, size.height - st), style = Stroke(st))
                drawArc(Palette.Emerald, -90f, 360f * read / total.coerceAtLeast(1), false, Offset(inset, inset), Size(size.width - st, size.height - st), style = Stroke(st, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${arabicDigits(read * 100 / total.coerceAtLeast(1))}٪", fontSize = 40.sp, fontWeight = FontWeight.Bold, fontFamily = Plex, color = Palette.EmeraldDark)
                Text("${arabicDigits(read)} من ${arabicDigits(total)} وجه", style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
            }
        }
        Spacer(Modifier.height(10.dp))
        if (Wird.complete) {
            Text("ما شاء الله، أتممت الختمة 🌙", style = MaterialTheme.typography.headlineSmall, color = Palette.EmeraldDark)
            Text("تقبّل الله منك", style = MaterialTheme.typography.bodyLarge, color = Palette.Muted)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onStop, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Palette.Emerald)) { Text("ختمة جديدة") }
            return@Column
        }
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("ورد اليوم", style = MaterialTheme.typography.titleMedium)
                    Text(
                        Wird.status().removePrefix("الورد: "),
                        style = MaterialTheme.typography.bodyMedium, color = if (Wird.leftToday == 0) Palette.Emerald else Palette.Muted,
                    )
                }
                Text("${arabicDigits(Wird.readToday)} من ${arabicDigits(Wird.pagesPerDay)}", fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = Plex, color = Palette.EmeraldDark)
            }
        }
        Spacer(Modifier.height(10.dp))
        Card {
            val next = Wird.next
            val sura = remember(next) { QuranDb.page(next).suras.firstOrNull()?.let { QuranDb.surah(it).name } ?: "" }
            InfoLine("تبدأ من", "صفحة ${arabicDigits(next)} · سورة $sura")
            InfoLine("الختام المتوقّع", wirdDate(Wird.expectedEnd()))
            InfoLine("التذكير", Wird.reminder?.let { timeLabel(it) } ?: "بدون")
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRead, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Palette.Emerald)) { Text("اقرأ وردك") }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("تعديل") }
            OutlinedButton(onClick = { confirmStop = true }, modifier = Modifier.weight(1f)) { Text("إنهاء الختمة") }
        }
        Spacer(Modifier.height(24.dp))
    }
    if (confirmStop) AlertDialog(
        onDismissRequest = { confirmStop = false },
        title = { Text("إنهاء الختمة") },
        text = { Text("سيتوقف الورد والتذكير. يمكنك بدء ختمة جديدة بعد ذلك.") },
        confirmButton = { TextButton(onClick = { confirmStop = false; onStop() }) { Text("إنهاء") } },
        dismissButton = { TextButton(onClick = { confirmStop = false }) { Text("إلغاء") } },
    )
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Palette.Muted, modifier = Modifier.width(120.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit) = this.then(Modifier.clickable(onClick = onClick))
