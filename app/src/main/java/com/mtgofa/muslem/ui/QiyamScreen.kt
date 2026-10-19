package com.mtgofa.muslem.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtgofa.muslem.data.Qiyam
import com.mtgofa.muslem.data.Settings
import com.mtgofa.muslem.prayer.AdhanService
import com.mtgofa.muslem.prayer.Prayer
import com.mtgofa.muslem.prayer.Scheduler
import com.mtgofa.muslem.prayer.arabicDigits
import java.time.LocalDate
import java.time.ZonedDateTime

/** The night prayer alarm: a set hour, or a set time before Fajr; with a sound or vibration only. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiyamScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val changes by Settings.changes.collectAsState()
    var picking by remember { mutableStateOf(false) }
    var pickingSound by remember { mutableStateOf(false) }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    // trying the alarm must not keep ringing after leaving the page
    DisposableEffect(Unit) { onDispose { AdhanService.stop(ctx) } }
    fun changed(turnedOn: Boolean = false) {
        Qiyam.schedule(ctx)
        if (turnedOn && Build.VERSION.SDK_INT >= 33) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    key(changes) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            TopBar("قيام الليل", onBack)
            Text(
                "﴿تَتَجَافَىٰ جُنُوبُهُمْ عَنِ الْمَضَاجِعِ يَدْعُونَ رَبَّهُمْ خَوْفًا وَطَمَعًا﴾",
                fontSize = 19.sp, lineHeight = 34.sp, color = Palette.EmeraldDark, textAlign = TextAlign.Center,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
            )
            Spacer(Modifier.height(10.dp))
            Card(Modifier.padding(horizontal = 16.dp), padding = PaddingValues(vertical = 4.dp)) {
                SwitchRow("تنبيه قيام الليل", Qiyam.summary(), Icons.Rounded.Bedtime, Settings.qiyamOn) {
                    Settings.qiyamOn = it; changed(it)
                }
            }
            if (Settings.qiyamOn) {
                SectionTitle("موعد التنبيه")
                Card(Modifier.padding(horizontal = 16.dp), padding = PaddingValues(16.dp)) {
                    Segmented(listOf(true, false), Settings.qiyamBeforeFajr, { if (it) "قبل الفجر" else "ساعة محددة" }) {
                        Settings.qiyamBeforeFajr = it; changed()
                    }
                    Spacer(Modifier.height(16.dp))
                    if (Settings.qiyamBeforeFajr) {
                        val before = Settings.qiyamBefore
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("ساعات", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Stepper(before / 60, 0..6, format = { arabicDigits(it) }) { Settings.qiyamBefore = it * 60 + before % 60; changed() }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("دقائق", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Stepper(before % 60, 0..55, step = 5, format = { arabicDigits(it) }) { Settings.qiyamBefore = before / 60 * 60 + it; changed() }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            if (before == 0) "مع أذان الفجر" else "قبل أذان الفجر بـ ${Qiyam.beforeText(before)}، ويتحرك مع الفجر على مدار السنة",
                            style = MaterialTheme.typography.bodyMedium, color = Palette.Muted,
                        )
                    } else {
                        Text(
                            clockText(Settings.qiyamAt), fontFamily = Plex, fontSize = 30.sp, color = Palette.EmeraldDark,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().background(Palette.EmeraldSoft, RoundedCornerShape(16.dp))
                                .clickable { picking = true }.padding(vertical = 14.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("اضغط على الوقت لتغييره", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
                    }
                }
                TonightCard()
                SectionTitle("طريقة التنبيه")
                Card(Modifier.padding(horizontal = 16.dp), padding = PaddingValues(16.dp)) {
                    Segmented(listOf(false, true), Settings.qiyamVibrate, { if (it) "اهتزاز فقط" else "صوت" }) { Settings.qiyamVibrate = it }
                    if (!Settings.qiyamVibrate) {
                        Spacer(Modifier.height(6.dp))
                        SettingRow("الصوت", Settings.qiyamSound.ar, null, { pickingSound = true }) { Chevron() }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (Settings.qiyamVibrate) "اهتزاز متدرّج لربع دقيقة تقريباً، دون إزعاج من حولك"
                        else "على صوت المنبّه، يتكرر حتى توقفه أو تضغط زر الصوت",
                        style = MaterialTheme.typography.bodySmall, color = Palette.Muted,
                    )
                    TextButton(onClick = {
                        androidx.core.content.ContextCompat.startForegroundService(
                            ctx, android.content.Intent(ctx, AdhanService::class.java).setAction(AdhanService.ACTION_QIYAM),
                        )
                    }) { Text("جرّب التنبيه") }
                }
            }
        }
    }
    if (pickingSound) SoundSheet("صوت تنبيه قيام الليل", com.mtgofa.muslem.prayer.AlertSound.FOR_ALERTS, Settings.qiyamSound, { pickingSound = false }) {
        Settings.qiyamSound = it
    }
    if (picking) {
        val at = Settings.qiyamAt
        val state = rememberTimePickerState(at / 60, at % 60, is24Hour = false)
        AlertDialog(
            onDismissRequest = { picking = false },
            confirmButton = { TextButton(onClick = { Settings.qiyamAt = state.hour * 60 + state.minute; picking = false; changed() }) { Text("تم") } },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("إلغاء") } },
            text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state) } },
        )
    }
}

/** Tonight's alarm against the night's own marks, so the choice can be judged at a glance. */
@Composable
private fun TonightCard() {
    val next = remember(Settings.qiyamBeforeFajr, Settings.qiyamBefore, Settings.qiyamAt) { Qiyam.next() } ?: return
    val day = next.toLocalDate().minusDays(if (next.hour < 12) 1 else 0)
    val maghrib = Scheduler.times(day)[Prayer.MAGHRIB]
    val fajr = Scheduler.times(day.plusDays(1))[Prayer.FAJR]
    val night = java.time.Duration.between(maghrib, fajr)
    val third = maghrib.plus(night.multipliedBy(2).dividedBy(3))
    fun t(z: ZonedDateTime) = AdhanService.formatTime(z.toInstant().toEpochMilli())
    Card(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), padding = PaddingValues(16.dp)) {
        Text("التنبيه القادم", style = MaterialTheme.typography.labelLarge, color = Palette.Muted)
        Text(t(next), fontFamily = Plex, fontSize = 28.sp, color = Palette.Emerald)
        Spacer(Modifier.height(6.dp))
        Text("بداية الثلث الأخير ${t(third)} · أذان الفجر ${t(fajr)}", style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
        if (next.isAfter(fajr)) Text("هذا الوقت بعد أذان الفجر", style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color(0xFFC0392B))
    }
}

private fun clockText(min: Int): String {
    val h = (min / 60) % 12
    return "%d:%02d %s".format(java.util.Locale.US, if (h == 0) 12 else h, min % 60, if (min < 12 * 60) "ص" else "م")
}
