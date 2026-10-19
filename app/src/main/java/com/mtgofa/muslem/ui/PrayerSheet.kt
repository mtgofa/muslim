package com.mtgofa.muslem.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mtgofa.muslem.data.AlertMode
import com.mtgofa.muslem.data.Settings
import com.mtgofa.muslem.prayer.AdhanService
import com.mtgofa.muslem.prayer.Prayer
import com.mtgofa.muslem.prayer.Scheduler
import java.time.LocalDate

/** Everything about one prayer: sound mode, time correction, reminder before, iqama after. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerSheet(p: Prayer, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val changes by Settings.changes.collectAsState()
    fun changed() = Scheduler.schedule(ctx)
    // closing the sheet stops a test adhan that is still playing
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { AdhanService.stopPreview(ctx) } }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MaterialTheme.colorScheme.surface) {
        // settings are plain reads; keying on the change counter re-reads them after every edit
        key(changes) {
        val time = Scheduler.today()[p]
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(prayerIcon(p))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (p == Prayer.SUNRISE) "الشروق" else "صلاة ${p.label(LocalDate.now())}", style = MaterialTheme.typography.headlineSmall)
                    Text("${clock(time)} ${ampm(time)}", style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
                }
            }
            Spacer(Modifier.height(20.dp))

            Text(if (p.hasAdhan) "عند دخول الوقت" else "عند الشروق", style = MaterialTheme.typography.labelLarge, color = Palette.Muted)
            Spacer(Modifier.height(8.dp))
            val modes = if (p.hasAdhan) AlertMode.entries else listOf(AlertMode.NOTIFY, AlertMode.SILENT)
            Segmented(modes, Settings.mode(p), { when (it) { AlertMode.ADHAN -> "أذان"; AlertMode.NOTIFY -> "تنبيه"; AlertMode.SILENT -> "صامت" } }) {
                Settings.setMode(p, it); changed()
            }

            Spacer(Modifier.height(18.dp))
            Line("تعديل الوقت", "قدّم أو أخّر الموعد بالدقائق ليطابق مسجدك") {
                Stepper(Settings.offset(p), -30..30, format = ::signedMinutes) { Settings.setOffset(p, it); changed() }
            }
            Line("تنبيه قبل ${if (p.hasAdhan) "الأذان" else "الشروق"}", "«${Settings.preSound.ar}» قبل الموعد بعدد الدقائق") {
                Stepper(Settings.preAlert(p), 0..60, format = { minutesLabel(it) }) { Settings.setPreAlert(p, it); changed() }
            }
            if (p.hasAdhan) Line("تنبيه الإقامة", "بعد الأذان بعدد الدقائق") {
                Stepper(Settings.iqama(p), 0..60, format = { minutesLabel(it) }) { Settings.setIqama(p, it); changed() }
            }

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (p.hasAdhan) OutlinedButton(onClick = { AdhanService.preview(ctx, Settings.muezzinFor(p)) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(4.dp)); Text("جرّب الأذان")
                }
                OutlinedButton(onClick = { AdhanService.preview(ctx, null) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(4.dp)); Text("جرّب التنبيه")
                }
            }
        }
        }
    }
}

@Composable
private fun Line(title: String, sub: String, trailing: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
        }
        Spacer(Modifier.width(8.dp))
        trailing()
    }
}
