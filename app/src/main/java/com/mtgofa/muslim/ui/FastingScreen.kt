package com.mtgofa.muslim.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtgofa.muslim.data.Fasting
import com.mtgofa.muslim.prayer.Hijri
import com.mtgofa.muslim.prayer.arabicDigits
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastingScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val changes by Fasting.changes.collectAsState()
    var picking by remember { mutableStateOf(false) }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun changed() {
        Fasting.schedule(ctx)
        if (Build.VERSION.SDK_INT >= 33 && Fasting.anyOn && Fasting.remindOn) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    key(changes) {
        val days = remember(changes) { Fasting.upcoming() }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item { TopBar("الصيام", onBack) }
            item {
                Card(Modifier.padding(horizontal = 16.dp), padding = PaddingValues(vertical = 4.dp)) {
                    SwitchRow("الاثنين والخميس", "صيام كل اثنين وخميس", Icons.Rounded.CalendarMonth, Fasting.monThu) { Fasting.monThu = it; changed() }
                    RowDivider()
                    SwitchRow("الأيام البيض", "١٣ و١٤ و١٥ من كل شهر هجري", Icons.Rounded.DarkMode, Fasting.whiteDays) { Fasting.whiteDays = it; changed() }
                    RowDivider()
                    SwitchRow(
                        "التذكير قبلها بليلة",
                        if (Fasting.remindOn) "الساعة ${Fasting.remindAt.format(DateTimeFormatter.ofPattern("h:mm", Locale.US))} ${if (Fasting.remindAt.hour < 12) "ص" else "م"} — اضغط على الوقت للتغيير" else "بدون تذكير",
                        Icons.Rounded.NotificationsActive, Fasting.remindOn,
                    ) { Fasting.remindOn = it; changed() }
                    if (Fasting.remindOn) TextButton(onClick = { picking = true }, modifier = Modifier.padding(start = 56.dp)) { Text("تغيير وقت التذكير") }
                }
            }
            item { SectionTitle(if (Fasting.anyOn) "الأيام القادمة" else "فعّل نوعاً من الصيام لتظهر أيامه") }
            if (Fasting.anyOn && days.isEmpty()) item {
                Text("لا توجد أيام صيام تطوّع قريبة (رمضان أو أيام العيد)", color = Palette.Muted, modifier = Modifier.padding(horizontal = 20.dp))
            }
            items(days, key = { it.date.toEpochDay() }) { d -> FastRow(d.date, d.reasons) }
        }
    }
    if (picking) {
        val state = rememberTimePickerState(Fasting.remindAt.hour, Fasting.remindAt.minute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { picking = false },
            confirmButton = { TextButton(onClick = { Fasting.remindAt = LocalTime.of(state.hour, state.minute); picking = false; changed() }) { Text("تم") } },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("إلغاء") } },
            text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state) } },
        )
    }
}

@Composable
private fun FastRow(date: LocalDate, reasons: List<String>) {
    val today = LocalDate.now()
    val label = when (date) { today -> "اليوم"; today.plusDays(1) -> "غداً"; else -> null }
    Row(
        Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth()
            .background(if (label != null) Palette.EmeraldSoft else MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier.size(54.dp).background(if (label != null) Palette.Emerald else Palette.GoldSoft, RoundedCornerShape(16.dp)),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        ) {
            Text(arabicDigits(date.dayOfMonth), fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = Plex, color = if (label != null) Color.White else Color(0xFF8A5E14))
            Text(gregorian(date).split(' ')[2], style = MaterialTheme.typography.labelSmall, color = if (label != null) Color.White else Color(0xFF8A5E14))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text((label?.let { "$it · " } ?: "") + gregorian(date).split(' ').first(), style = MaterialTheme.typography.titleMedium)
            Text(reasons.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = Palette.EmeraldDark)
            Text(Hijri.of(date).toString(), style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
        }
    }
}
