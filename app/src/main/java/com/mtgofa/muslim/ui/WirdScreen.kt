package com.mtgofa.muslim.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtgofa.muslim.data.Khatma
import com.mtgofa.muslim.data.Settings
import com.mtgofa.muslim.data.Wird
import com.mtgofa.muslim.data.WirdUnit
import com.mtgofa.muslim.prayer.arabicDigits
import com.mtgofa.muslim.quran.QuranDb
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val AR_DAYS_W = listOf("الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد")
private val AR_MONTHS_W = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")
fun wirdDate(d: LocalDate) = "${AR_DAYS_W[d.dayOfWeek.value - 1]} ${arabicDigits(d.dayOfMonth)} ${AR_MONTHS_W[d.monthValue - 1]}"

private fun timeLabel(t: LocalTime) = t.format(DateTimeFormatter.ofPattern("h:mm", Locale.US)) + if (t.hour < 12) " ص" else " م"

private sealed interface WirdPage {
    data object List : WirdPage
    data object Add : WirdPage
    data class Edit(val id: Long) : WirdPage
    data class Detail(val id: Long) : WirdPage
}

/** All khatmas, adding one, and each khatma's progress. */
@Composable
fun WirdScreen(onBack: () -> Unit, onRead: (Khatma) -> Unit) {
    val changes by Wird.changes.collectAsState()
    var page by remember { mutableStateOf<WirdPage>(if (Wird.all.isEmpty()) WirdPage.Add else WirdPage.List) }
    fun back() {
        when (val p = page) {
            WirdPage.List -> onBack()
            is WirdPage.Edit -> page = WirdPage.Detail(p.id)
            else -> if (Wird.all.isEmpty()) onBack() else page = WirdPage.List
        }
    }
    androidx.activity.compose.BackHandler(page != WirdPage.List) { back() }
    key(changes) {
        Column(Modifier.fillMaxSize()) {
            TopBar(
                when (page) { WirdPage.Add -> "إضافة ختمة"; is WirdPage.Edit -> "تعديل الختمة"; is WirdPage.Detail -> "الختمة"; else -> "الورد اليومي" },
                ::back,
            )
            when (val p = page) {
                WirdPage.List -> KhatmaList(onOpen = { page = WirdPage.Detail(it) }, onAdd = { page = WirdPage.Add }, onRead = onRead)
                WirdPage.Add -> KhatmaSetup(null) { id -> page = WirdPage.Detail(id) }
                is WirdPage.Edit -> KhatmaSetup(Wird.byId(p.id)) { page = WirdPage.Detail(p.id) }
                is WirdPage.Detail -> {
                    val k = Wird.byId(p.id)
                    if (k == null) page = WirdPage.List
                    else KhatmaDetail(k, onRead = { onRead(k) }, onEdit = { page = WirdPage.Edit(k.id) }) {
                        page = if (Wird.all.isEmpty()) WirdPage.Add else WirdPage.List
                    }
                }
            }
        }
    }
}

@Composable
private fun KhatmaList(onOpen: (Long) -> Unit, onAdd: () -> Unit, onRead: (Khatma) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(Wird.all, key = { it.id }) { k -> KhatmaCard(k, { onOpen(k.id) }, { onRead(k) }) }
        item {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).border(1.5.dp, Palette.Emerald, RoundedCornerShape(20.dp))
                    .clickable(onClick = onAdd).padding(16.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Add, null, tint = Palette.Emerald)
                Spacer(Modifier.width(8.dp))
                Text("إضافة ختمة", style = MaterialTheme.typography.titleMedium, color = Palette.Emerald)
            }
        }
    }
}

/** A khatma's summary card, also used on the home screen. */
@Composable
fun KhatmaCard(k: Khatma, onOpen: () -> Unit, onRead: () -> Unit) {
    val late = k.behind > 0
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color(0xFFFFF4E6)).clickable(onClick = onOpen).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val st = 6.dp.toPx(); val inset = st / 2
                val sz = Size(size.width - st, size.height - st)
                drawArc(Color(0xFFF6DDB8), 0f, 360f, false, Offset(inset, inset), sz, style = Stroke(st))
                drawArc(if (k.complete) Palette.Emerald else Color(0xFFC0392B), -90f, 360f * k.progress.coerceIn(0f, 1f), false, Offset(inset, inset), sz,
                    style = Stroke(st, cap = StrokeCap.Round))
            }
            Text("${arabicDigits((k.progress * 100).toInt())}٪", style = MaterialTheme.typography.labelMedium, color = Color(0xFF8A2A1F))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(k.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(
                k.status(), style = MaterialTheme.typography.bodyMedium,
                color = when { k.complete || k.onTrack -> Palette.Emerald; late -> Color(0xFFC0392B); else -> Palette.Ink },
            )
            if (!k.complete) Text("الختام ${wirdDate(k.expectedEnd())}", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
        }
        if (!k.complete) Text(
            "اقرأ", style = MaterialTheme.typography.labelLarge, color = Color.White,
            modifier = Modifier.clip(CircleShape).background(Color(0xFFC0392B)).clickable(onClick = onRead).padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KhatmaSetup(editing: Khatma?, onDone: (Long) -> Unit) {
    val ctx = LocalContext.current
    var unit by remember { mutableStateOf(editing?.unit ?: WirdUnit.HIZB) }
    var amount by remember { mutableIntStateOf(editing?.amount ?: 1) }
    var startPage by remember { mutableIntStateOf(editing?.startPage ?: 1) }
    var remindOn by remember { mutableStateOf(editing == null || editing.reminder != null) }
    var remindAt by remember { mutableStateOf(editing?.reminder ?: LocalTime.of(20, 0)) }
    var picking by remember { mutableStateOf<String?>(null) }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    // estimate from where it starts (or, when editing, from where the reader is now)
    val probe = remember(unit, startPage, editing) {
        Khatma(0, unit, 1, editing?.startPage ?: startPage, editing?.next ?: startPage, -1, 0, 0, 0, -1, 0)
    }
    val units = probe.totalUnits - probe.readUnits
    val (days, end) = Khatma.estimate(units, amount)
    val maxAmount = when (unit) { WirdUnit.PAGE -> 60; WirdUnit.QUARTER -> 40; WirdUnit.HIZB -> 20; WirdUnit.JUZ -> 10 }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Text("حدّد قراءتك اليومية", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 6.dp))
        Card {
            Text("الوحدة", style = MaterialTheme.typography.labelLarge, color = Palette.Muted)
            Spacer(Modifier.height(6.dp))
            Segmented(WirdUnit.entries, unit, { it.singular }) { unit = it; amount = amount.coerceAtMost(maxAmount) }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("القدر يومياً", style = MaterialTheme.typography.labelLarge, color = Palette.Muted, modifier = Modifier.weight(1f))
                Stepper(amount, 1..maxAmount, format = { arabicDigits(it) }) { amount = it }
            }
            Text(unit.count(amount) + " كل يوم", style = MaterialTheme.typography.bodyMedium, color = Palette.EmeraldDark, modifier = Modifier.padding(top = 6.dp))
        }
        if (editing == null) {
            Spacer(Modifier.height(10.dp))
            Card {
                Text("من أي مكان في المصحف تبدأ الختمة؟", style = MaterialTheme.typography.labelLarge, color = Palette.Muted)
                Spacer(Modifier.height(8.dp))
                SelectBox(startLabel(startPage)) { picking = "start" }
            }
        }
        Spacer(Modifier.height(10.dp))
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("تذكير يومي", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (remindOn) "الساعة ${timeLabel(remindAt)} — اضغط للتغيير" else "بدون تذكير",
                        style = MaterialTheme.typography.bodySmall, color = Palette.Muted,
                        modifier = Modifier.clickable { if (remindOn) picking = "time" },
                    )
                }
                Switch(remindOn, { remindOn = it }, colors = SwitchDefaults.colors(checkedTrackColor = Palette.Emerald))
            }
        }
        Spacer(Modifier.height(14.dp))
        Column(
            Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF14936F), Color(0xFF0A5A48))), RoundedCornerShape(22.dp)).padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("المدة المتوقعة لإنهاء الختمة", color = Color(0xD9FFFFFF), style = MaterialTheme.typography.bodyMedium)
            Text("${arabicDigits(days)} ${if (days in 3..10) "أيام" else "يوماً"}", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold, fontFamily = Plex)
            Text("يوم ${wirdDate(end)}", color = Color(0xFFF2D59B), style = MaterialTheme.typography.titleMedium)
            Text("معدّل وردك: ${unit.count(amount)} يومياً", color = Color(0xB3FFFFFF), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (remindOn && Build.VERSION.SDK_INT >= 33) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
                val at = if (remindOn) remindAt else null
                if (editing != null) { Wird.update(ctx, editing.id, unit, amount, at); onDone(editing.id) }
                else onDone(Wird.add(ctx, unit, amount, startPage, at).id)
            },
            modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Palette.Emerald),
        ) { Text(if (editing != null) "حفظ" else "ابدأ الختمة") }
        Spacer(Modifier.height(24.dp))
    }

    when (picking) {
        "time" -> {
            val state = rememberTimePickerState(remindAt.hour, remindAt.minute, is24Hour = false)
            AlertDialog(
                onDismissRequest = { picking = null },
                confirmButton = { TextButton(onClick = { remindAt = LocalTime.of(state.hour, state.minute); picking = null }) { Text("تم") } },
                dismissButton = { TextButton(onClick = { picking = null }) { Text("إلغاء") } },
                text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state) } },
            )
        }
        "start" -> AlertDialog(
            onDismissRequest = { picking = null },
            confirmButton = { TextButton(onClick = { picking = null }) { Text("إغلاق") } },
            title = { Text("بداية الختمة") },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    item {
                        val here = Settings.lastPage
                        StartRow("الصفحة الحالية — ${startLabel(here)}", startPage == here) { startPage = here; picking = null }
                    }
                    items(QuranDb.juz, key = { it.id }) { j ->
                        StartRow("الجزء ${JUZ_NAMES[j.id - 1]}", startPage == j.page) { startPage = j.page; picking = null }
                    }
                }
            },
        )
    }
}

private fun startLabel(page: Int): String {
    QuranDb.juz.firstOrNull { it.page == page }?.let { return "الجزء ${JUZ_NAMES[it.id - 1]}" }
    val s = QuranDb.page(page).suras.firstOrNull()?.let { QuranDb.surah(it).name } ?: ""
    return "صفحة ${arabicDigits(page)} · سورة $s"
}

@Composable
private fun StartRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text, style = MaterialTheme.typography.bodyLarge, color = if (selected) Palette.EmeraldDark else Palette.Ink,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (selected) Palette.EmeraldSoft else Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 12.dp),
    )
}

@Composable
private fun SelectBox(text: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Icon(Icons.Rounded.KeyboardArrowDown, null, tint = Palette.Muted)
    }
}

@Composable
private fun KhatmaDetail(k: Khatma, onRead: () -> Unit, onEdit: () -> Unit, onRemoved: () -> Unit) {
    val ctx = LocalContext.current
    var confirm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(k.title, style = MaterialTheme.typography.titleMedium, color = Palette.Muted, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(8.dp))
        Box(Modifier.size(210.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val st = 14.dp.toPx(); val inset = st / 2
                drawArc(Palette.EmeraldSoft, 0f, 360f, false, Offset(inset, inset), Size(size.width - st, size.height - st), style = Stroke(st))
                drawArc(Palette.Emerald, -90f, 360f * k.progress.coerceIn(0f, 1f), false, Offset(inset, inset), Size(size.width - st, size.height - st), style = Stroke(st, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${arabicDigits((k.progress * 100).toInt())}٪", fontSize = 40.sp, fontWeight = FontWeight.Bold, fontFamily = Plex, color = Palette.EmeraldDark)
                Text("${arabicDigits(k.readUnits)} من ${k.unit.count(k.totalUnits)}", style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
            }
        }
        Spacer(Modifier.height(10.dp))
        if (k.complete) {
            Text("ما شاء الله، أتممت الختمة 🌙", style = MaterialTheme.typography.headlineSmall, color = Palette.EmeraldDark)
            Text("تقبّل الله منك", style = MaterialTheme.typography.bodyLarge, color = Palette.Muted)
            Spacer(Modifier.height(12.dp))
        } else {
            Card {
                Text("ورد اليوم", style = MaterialTheme.typography.titleMedium)
                Text(
                    k.status(), style = MaterialTheme.typography.headlineSmall,
                    color = when { k.onTrack -> Palette.Emerald; k.behind > 0 -> Color(0xFFC0392B); else -> Palette.Ink },
                )
                Text("قرأت اليوم ${WirdUnit.PAGE.count(k.pagesToday)}", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
            }
            Spacer(Modifier.height(10.dp))
            Card {
                val sura = remember(k.next) { QuranDb.page(k.next).suras.firstOrNull()?.let { QuranDb.surah(it).name } ?: "" }
                InfoLine("تكمل من", "صفحة ${arabicDigits(k.next)} · سورة $sura")
                InfoLine("الختام المتوقّع", wirdDate(k.expectedEnd()))
                InfoLine("التذكير", k.reminder?.let { timeLabel(it) } ?: "بدون")
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRead, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Palette.Emerald)) { Text("اقرأ وردك") }
            Spacer(Modifier.height(8.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!k.complete) OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("تعديل") }
            OutlinedButton(onClick = { confirm = true }, modifier = Modifier.weight(1f)) { Text("حذف الختمة") }
        }
        Spacer(Modifier.height(24.dp))
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("حذف الختمة") },
        text = { Text("ستُحذف هذه الختمة وتذكيرها.") },
        confirmButton = { TextButton(onClick = { confirm = false; Wird.remove(ctx, k.id); onRemoved() }) { Text("حذف") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("إلغاء") } },
    )
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Palette.Muted, modifier = Modifier.width(120.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
    }
}
