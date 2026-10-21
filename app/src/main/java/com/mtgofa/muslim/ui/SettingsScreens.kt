package com.mtgofa.muslim.ui

import com.mtgofa.muslim.prayer.AlertSound
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.mtgofa.muslim.BuildConfig
import com.mtgofa.muslim.data.Settings
import com.mtgofa.muslim.prayer.AdhanService
import com.mtgofa.muslim.prayer.AsrMadhab
import com.mtgofa.muslim.prayer.CalcMethod
import com.mtgofa.muslim.prayer.Cities
import com.mtgofa.muslim.prayer.Hijri
import com.mtgofa.muslim.prayer.KeepAliveService
import com.mtgofa.muslim.prayer.Locator
import com.mtgofa.muslim.prayer.Muezzins
import com.mtgofa.muslim.prayer.Prayer
import com.mtgofa.muslim.prayer.Reliability
import com.mtgofa.muslim.prayer.Scheduler
import com.mtgofa.muslim.prayer.arabicDigits
import com.mtgofa.muslim.quran.QuranPlayer
import com.mtgofa.muslim.quran.Reciters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

// ============================================================================ main settings

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLocation: () -> Unit,
    onMuezzin: () -> Unit,
    onPrayers: () -> Unit,
    onReliability: () -> Unit,
    onReciters: () -> Unit,
    onAbout: () -> Unit,
    onQiyam: () -> Unit,
) {
    val ctx = LocalContext.current
    val changes by Settings.changes.collectAsState()
    var picker by remember { mutableStateOf<String?>(null) }
    key(changes) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            TopBar("الإعدادات", onBack)
            SectionTitle("مواقيت الصلاة")
            Card(Modifier.padding(horizontal = 16.dp), padding = PaddingValues(vertical = 4.dp)) {
                SettingRow("الموقع", Settings.locationName + if (Settings.locationSource == "gps") " (تلقائي)" else "", Icons.Rounded.LocationOn, onLocation) { Chevron() }
                RowDivider()
                SettingRow("طريقة الحساب", Settings.method.ar, Icons.Rounded.Calculate, { picker = "method" }) { Chevron() }
                RowDivider()
                SettingRow("صلاة العصر", Settings.madhab.ar, Icons.Rounded.Tune, { picker = "madhab" }) { Chevron() }
                RowDivider()
                SettingRow("التاريخ الهجري", Hijri.of(LocalDate.now()).toString(), Icons.Rounded.CalendarMonth) {
                    Stepper(Settings.hijriAdjust, -2..2, format = { if (it == 0) "بدون" else (if (it > 0) "+" else "−") + arabicDigits(kotlin.math.abs(it)) + " يوم" }) {
                        Settings.hijriAdjust = it
                    }
                }
            }
            SectionTitle("الأذان والتنبيهات")
            Card(Modifier.padding(horizontal = 16.dp), padding = PaddingValues(vertical = 4.dp)) {
                SettingRow("صوت المؤذن", Muezzins.byId(Settings.muezzin).name, Icons.Rounded.RecordVoiceOver, onMuezzin) { Chevron() }
                RowDivider()
                SettingRow("ضبط كل صلاة", "نوع التنبيه، تعديل الوقت، التنبيه قبل الأذان، والإقامة", Icons.Rounded.Alarm, onPrayers) { Chevron() }
                RowDivider()
                SettingRow("تنبيه قيام الليل", com.mtgofa.muslim.data.Qiyam.summary(), Icons.Rounded.Bedtime, onQiyam) { Chevron() }
                RowDivider()
                SwitchRow(
                    "شغّل الأذان حتى لو الموبايل صامت",
                    if (Settings.adhanInSilent) "يعمل على صوت المنبّه دائماً" else "يتبع صوت الرنين، ويسكت مع الصامت والهزاز",
                    Icons.Rounded.VolumeUp, Settings.adhanInSilent,
                ) { Settings.adhanInSilent = it }
                RowDivider()
                SettingRow("صوت التنبيه قبل الأذان", Settings.preSound.ar, Icons.Rounded.Notifications, { picker = "preSound" }) { Chevron() }
                RowDivider()
                SettingRow("صوت تنبيه الإقامة", Settings.iqamaSound.ar, Icons.Rounded.Notifications, { picker = "iqamaSound" }) { Chevron() }
                RowDivider()
                SwitchRow("إيقاف الأذان أثناء المكالمات", null, Icons.Rounded.Notifications, Settings.muteInCall) { Settings.muteInCall = it }
            }
            SectionTitle("ضمان عمل الأذان")
            Card(Modifier.padding(horizontal = 16.dp), padding = PaddingValues(vertical = 4.dp)) {
                SwitchRow("إشعار الصلاة القادمة الدائم", "يُبقي التطبيق يعمل دائماً ولا يغلقه النظام", Icons.Rounded.PushPin, Settings.keepAlive) {
                    Settings.keepAlive = it; KeepAliveService.refresh(ctx)
                }
                RowDivider()
                SettingRow("فحص إعدادات الهاتف", "البطارية، المنبّهات الدقيقة، الإشعارات", Icons.Rounded.Shield, onReliability) { Chevron() }
            }
            SectionTitle("المصحف")
            Card(Modifier.padding(horizontal = 16.dp), padding = PaddingValues(vertical = 4.dp)) {
                SwitchRow("ألوان التجويد", null, Icons.Rounded.MenuBook, Settings.tajweed) { Settings.tajweed = it }
                RowDivider()
                SwitchRow("إبقاء الشاشة مضاءة أثناء القراءة", null, Icons.Rounded.Info, Settings.keepScreenOn) { Settings.keepScreenOn = it }
                RowDivider()
                SettingRow("القرّاء والتلاوات المحمّلة", Reciters.byId(Settings.reciter).name, Icons.Rounded.Download, onReciters) { Chevron() }
            }
            SectionTitle("التطبيق")
            Card(Modifier.padding(horizontal = 16.dp), padding = PaddingValues(vertical = 4.dp)) {
                SettingRow("حول التطبيق والتحديثات", "الإصدار ${BuildConfig.VERSION_NAME}", Icons.Rounded.SystemUpdate, onAbout) { Chevron() }
            }
        }
    }

    when (picker) {
        "method" -> ChoiceSheet("طريقة الحساب", CalcMethod.entries, Settings.method, { it.ar }, { picker = null }) {
            Settings.method = it; Scheduler.schedule(ctx)
        }
        "madhab" -> ChoiceSheet("حساب وقت العصر", AsrMadhab.entries, Settings.madhab, { it.ar }, { picker = null }) {
            Settings.madhab = it; Scheduler.schedule(ctx)
        }
        "preSound" -> SoundSheet("صوت التنبيه قبل الأذان", AlertSound.FOR_ALERTS, Settings.preSound, { picker = null }) { Settings.preSound = it }
        "iqamaSound" -> SoundSheet("صوت تنبيه الإقامة", AlertSound.FOR_IQAMA, Settings.iqamaSound, { picker = null }) { Settings.iqamaSound = it }
    }
}

@Composable
fun Chevron() = Icon(Icons.Rounded.ChevronLeft, null, tint = Palette.Muted)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ChoiceSheet(title: String, options: List<T>, selected: T, label: (T) -> String, onDismiss: () -> Unit, onPick: (T) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            options.forEach { o ->
                Row(
                    Modifier.fillMaxWidth().clickable { onPick(o); onDismiss() }.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(o == selected, { onPick(o); onDismiss() })
                    Text(label(o), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

/** Pick an alert sound; tapping one selects it and plays it, so they can be compared. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundSheet(title: String, options: List<AlertSound>, selected: AlertSound, onDismiss: () -> Unit, onPick: (AlertSound) -> Unit) {
    val ctx = LocalContext.current
    val sounding by AdhanService.sounding.collectAsState()
    var current by remember { mutableStateOf(selected) }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { AdhanService.stopPreview(ctx) } }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            options.forEach { o ->
                val playing = sounding?.prayer == null && sounding?.alarm == false && sounding?.title == o.ar
                Row(
                    Modifier.fillMaxWidth().clickable { current = o; onPick(o); AdhanService.preview(ctx, null, o) }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(o == current, { current = o; onPick(o); AdhanService.preview(ctx, null, o) })
                    Text(o.ar, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = { if (playing) AdhanService.stop(ctx) else AdhanService.preview(ctx, null, o) }) {
                        Box(Modifier.size(36.dp).background(Palette.EmeraldSoft, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(if (playing) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, "استماع", tint = Palette.Emerald)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================ per-prayer list

@Composable
fun PrayersScreen(onBack: () -> Unit) {
    val changes by Settings.changes.collectAsState()
    var open by remember { mutableStateOf<Prayer?>(null) }
    key(changes) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            TopBar("ضبط كل صلاة", onBack)
            Text(
                "اختر الصلاة لتحديد الأذان أو تنبيه قصير أو الصمت، وتعديل الوقت، ومدة التنبيه قبل الأذان، وموعد الإقامة.",
                style = MaterialTheme.typography.bodyMedium, color = Palette.Muted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
            Card(Modifier.padding(horizontal = 16.dp), padding = PaddingValues(vertical = 4.dp)) {
                val today = Scheduler.today()
                Prayer.entries.forEachIndexed { i, p ->
                    val parts = buildList {
                        add(Settings.mode(p).ar)
                        Settings.offset(p).takeIf { it != 0 }?.let { add("تعديل ${signedMinutes(it)}") }
                        Settings.preAlert(p).takeIf { it > 0 }?.let { add("قبلها ${arabicDigits(it)} د") }
                        if (p.hasAdhan) Settings.iqama(p).takeIf { it > 0 }?.let { add("إقامة ${arabicDigits(it)} د") }
                    }
                    SettingRow(
                        p.label(LocalDate.now()) + "  ·  " + clock(today[p]) + " " + ampm(today[p]),
                        parts.joinToString(" · "), prayerIcon(p), { open = p },
                    ) { Chevron() }
                    if (i < Prayer.entries.lastIndex) RowDivider()
                }
            }
        }
    }
    open?.let { PrayerSheet(it) { open = null } }
}

// ============================================================================ muezzin

@Composable
fun MuezzinScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val changes by Settings.changes.collectAsState()
    val sounding by AdhanService.sounding.collectAsState()
    val progress = remember { mutableStateMapOf<String, Float>() }
    var rev by remember { mutableStateOf(0) }
    var fajrMode by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // a preview must not outlive the page (it would only stop from the notification)
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { AdhanService.stopPreview(ctx) } }

    key(changes, rev) {
        Column(Modifier.fillMaxSize()) {
            TopBar("صوت المؤذن", onBack)
            VibrateOnlyCard()
            Spacer(Modifier.height(10.dp))
            Box(Modifier.padding(horizontal = 16.dp).alpha(if (Settings.vibrateOnly) 0.45f else 1f)) {
                Segmented(listOf(false, true), fajrMode, { if (it) "أذان الفجر" else "باقي الصلوات" }) { fajrMode = it }
            }
            if (fajrMode) Text(
                "يمكنك اختيار صوت مختلف لأذان الفجر، أو تركه مثل باقي الصلوات.",
                style = MaterialTheme.typography.bodySmall, color = Palette.Muted, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) }
            LazyColumn(Modifier.alpha(if (Settings.vibrateOnly) 0.45f else 1f), contentPadding = PaddingValues(16.dp)) {
                if (fajrMode) item {
                    MuezzinRow("مثل باقي الصلوات (بدون «الصلاة خير من النوم»)", Settings.fajrMuezzin.isEmpty(), true, null, false, null, {}, { Settings.fajrMuezzin = "" }, null)
                }
                items(Muezzins.all, key = { it.id }) { m ->
                    val available = Muezzins.isAvailable(ctx, m)
                    val selected = if (fajrMode) Settings.fajrMuezzin == m.id else Settings.muezzin == m.id
                    val playing = sounding != null && sounding?.prayer == null && sounding?.title == m.name
                    MuezzinRow(
                        m.name, selected, available, progress[m.id], playing,
                        if (m.bundled) "مدمج في التطبيق" else if (available) "محمّل" else "يحتاج تنزيل مرة واحدة",
                        onPreview = { if (playing) AdhanService.stop(ctx) else AdhanService.preview(ctx, m.id) },
                        onSelect = {
                            fun pick() { if (fajrMode) Settings.fajrMuezzin = m.id else Settings.muezzin = m.id }
                            if (available) pick() else if (progress[m.id] == null) {
                                progress[m.id] = 0f; error = null
                                scope.launch {
                                    val ok = withContext(Dispatchers.IO) { Muezzins.download(ctx, m) { progress[m.id] = it } }
                                    progress.remove(m.id)
                                    if (ok) pick() else error = "تعذّر التنزيل — تأكّد من الاتصال بالإنترنت"
                                    rev++
                                }
                            }
                        },
                        onDelete = if (!m.bundled && available && !selected) ({ Muezzins.delete(ctx, m); rev++ }) else null,
                    )
                }
            }
        }
    }
}

/** Vibration instead of any sound, with a button to feel the adhan's pattern. */
@Composable
private fun VibrateOnlyCard() {
    val ctx = LocalContext.current
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { com.mtgofa.muslim.data.Haptics.cancel(ctx) } }
    Card(Modifier.padding(horizontal = 16.dp), padding = PaddingValues(vertical = 4.dp)) {
        SwitchRow(
            "اهتزاز فقط بدون صوت",
            if (Settings.vibrateOnly) "الأذان والتنبيهات تهتز بنمط خاص بكل منها" else "يحل الاهتزاز محل صوت الأذان والتنبيهات",
            Icons.Rounded.Vibration, Settings.vibrateOnly,
        ) { Settings.vibrateOnly = it }
        if (Settings.vibrateOnly) Row(Modifier.padding(start = 48.dp, bottom = 6.dp)) {
            TextButton(onClick = { com.mtgofa.muslim.data.Haptics.pattern(ctx, *com.mtgofa.muslim.data.Haptics.ADHAN) }) { Text("جرّب اهتزاز الأذان") }
            TextButton(onClick = { com.mtgofa.muslim.data.Haptics.pattern(ctx, *com.mtgofa.muslim.data.Haptics.IQAMA) }) { Text("الإقامة") }
            TextButton(onClick = { com.mtgofa.muslim.data.Haptics.pattern(ctx, *com.mtgofa.muslim.data.Haptics.REMINDER) }) { Text("التنبيه") }
        }
    }
}

@Composable
private fun MuezzinRow(
    name: String, selected: Boolean, available: Boolean, progress: Float?, playing: Boolean, note: String?,
    onPreview: () -> Unit, onSelect: () -> Unit, onDelete: (() -> Unit)?,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(if (selected) Palette.EmeraldSoft else MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected, onSelect)
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            if (progress != null) LinearProgressIndicator({ progress }, Modifier.fillMaxWidth().padding(top = 6.dp))
            else if (note != null) Text(note, style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
        }
        if (onDelete != null) IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, "حذف", tint = Palette.Muted) }
        if (note != null && available) IconButton(onClick = onPreview) {
            Box(Modifier.size(36.dp).background(Palette.Emerald, CircleShape), contentAlignment = Alignment.Center) {
                Icon(if (playing) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, "استماع", tint = Color.White)
            }
        } else if (note != null && progress == null) Icon(Icons.Rounded.Download, null, tint = Palette.Emerald, modifier = Modifier.padding(8.dp))
    }
}

// ============================================================================ location

@Composable
fun LocationScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun locate() {
        busy = true; error = null
        scope.launch {
            val loc = Locator.current(ctx)
            if (loc == null) {
                error = "تعذّر تحديد موقعك — فعّل خدمة الموقع (GPS) أو اختر مدينتك من القائمة"
            } else {
                val name = Locator.placeName(ctx, loc.latitude, loc.longitude)
                    ?: ("قرب " + Locator.nearestCity(loc.latitude, loc.longitude).name)
                Settings.setLocation(name, loc.latitude, loc.longitude, null, "gps")
                Scheduler.schedule(ctx)
                onBack()
            }
            busy = false
        }
    }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r.values.any { it }) locate() else error = "لم يُسمح بالوصول للموقع — اختر مدينتك من القائمة"
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("الموقع", onBack)
        Card(Modifier.padding(horizontal = 16.dp), color = Palette.EmeraldSoft, onClick = {
            if (!busy) perm.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
        }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.MyLocation, tint = Color.White, bg = Palette.Emerald)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("تحديد موقعي تلقائياً", style = MaterialTheme.typography.titleMedium, color = Palette.EmeraldDark)
                    Text("باستخدام GPS — أدق مواقيت لمكانك", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
                }
                if (busy) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp)
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
        OutlinedTextField(
            query, { query = it },
            placeholder = { Text("ابحث عن مدينة أو محافظة") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        val list = remember(query) { Cities.all.filter { query.isBlank() || it.name.contains(query) || it.country.contains(query) } }
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
            items(list, key = { it.name + it.country }) { c ->
                val selected = Settings.locationSource == "city" && Settings.locationName == c.name
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            Settings.setLocation(c.name, c.lat, c.lng, c.zone, "city")
                            Settings.method = c.method
                            Scheduler.schedule(ctx)
                            onBack()
                        }
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.LocationOn, null, tint = if (selected) Palette.Emerald else Palette.Muted.copy(alpha = 0.5f))
                    Spacer(Modifier.width(12.dp))
                    Text(c.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(c.country, style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
                }
            }
        }
    }
}

// ============================================================================ reliability

@Composable
fun ReliabilityScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var rev by remember { mutableStateOf(0) }
    LifecycleResumeEffect(Unit) { rev++; onPauseOrDispose { } }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { rev++ }

    key(rev) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            TopBar("ضمان عمل الأذان", onBack)
            Text(
                "أنظمة أندرويد وبعض الشركات (شاومي، أوبو، سامسونج…) توقف التطبيقات في الخلفية لتوفير البطارية. فعّل ما يلي ليصلك الأذان في وقته دائماً.",
                style = MaterialTheme.typography.bodyMedium, color = Palette.Muted, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
            Card(Modifier.padding(horizontal = 16.dp), padding = PaddingValues(vertical = 4.dp)) {
                CheckRow("الإشعارات", "لعرض الأذان والتنبيهات", Reliability.notificationsOn(ctx)) {
                    if (Build.VERSION.SDK_INT >= 33) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS) else Reliability.openNotificationSettings(ctx)
                }
                RowDivider()
                CheckRow("تشغيل بلا قيود على البطارية", "حتى لا يؤجّل النظام الأذان", Reliability.batteryUnrestricted(ctx)) { Reliability.askBattery(ctx) }
                RowDivider()
                CheckRow("المنبّهات الدقيقة", "لينطلق الأذان في الدقيقة نفسها", Reliability.exactAlarms(ctx)) { Reliability.openExactAlarm(ctx) }
                RowDivider()
                CheckRow("الإشعار الدائم للصلاة القادمة", "يُبقي التطبيق حيّاً في الخلفية", Settings.keepAlive) {
                    Settings.keepAlive = true; KeepAliveService.refresh(ctx); rev++
                }
            }
            if (Reliability.vendorHasAutostart) {
                SectionTitle("إعدادات الشركة المصنّعة")
                Card(Modifier.padding(horizontal = 16.dp)) {
                    Text("فعّل «التشغيل التلقائي» واختر «بلا قيود» للبطارية لهذا التطبيق، وفي قائمة التطبيقات الحديثة اقفل التطبيق (أيقونة القفل).", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                    Row {
                        Button(onClick = { Reliability.openAutostart(ctx) }) { Text("التشغيل التلقائي") }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { Reliability.openAppInfo(ctx) }) { Text("معلومات التطبيق") }
                    }
                }
            }
            SectionTitle("التنبيه القادم")
            Card(Modifier.padding(horizontal = 16.dp)) {
                val ev = Scheduler.nextEvent()
                Text(
                    if (ev == null) "لا توجد تنبيهات مفعّلة" else {
                        val kind = when (ev.type) {
                            com.mtgofa.muslim.prayer.EventType.PRE -> "تنبيه قبل ${ev.prayer.ar}"
                            com.mtgofa.muslim.prayer.EventType.ADHAN -> if (ev.prayer == Prayer.SUNRISE) "الشروق" else "أذان ${ev.prayer.ar}"
                            com.mtgofa.muslim.prayer.EventType.IQAMA -> "إقامة ${ev.prayer.ar}"
                        }
                        "$kind — الساعة ${AdhanService.formatTime(ev.at)}"
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun CheckRow(title: String, sub: String, ok: Boolean, fix: () -> Unit) {
    SettingRow(title, sub, if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber, if (ok) null else fix) {
        // same-width pill either way, so the status column lines up
        Box(
            Modifier
                .width(84.dp)
                .clip(RoundedCornerShape(50))
                .background(if (ok) Palette.EmeraldSoft else Palette.Emerald)
                .then(if (ok) Modifier else Modifier.clickable(onClick = fix))
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (ok) "مفعّل" else "تفعيل",
                color = if (ok) Palette.EmeraldDark else Color.White,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

// ============================================================================ reciters

@Composable
fun RecitersScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val changes by Settings.changes.collectAsState()
    var rev by remember { mutableStateOf(0) }
    key(changes, rev) {
        Column(Modifier.fillMaxSize()) {
            TopBar("القرّاء", onBack)
            Text(
                "اختر القارئ. التلاوات تعمل مباشرة من الإنترنت، ويمكنك تنزيل أي سورة من مشغّل المصحف للاستماع بدون إنترنت.",
                style = MaterialTheme.typography.bodyMedium, color = Palette.Muted, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                items(Reciters.all, key = { it.id }) { r ->
                    val size = remember(rev) { Reciters.downloadedSize(ctx, r) }
                    val selected = Settings.reciter == r.id
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .background(if (selected) Palette.EmeraldSoft else MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                            .clickable { QuranPlayer.changeReciter(ctx, r.id) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected, { QuranPlayer.changeReciter(ctx, r.id) })
                        Column(Modifier.weight(1f)) {
                            Text(r.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                r.style + if (size > 0) " · محمّل ${arabicDigits("%.1f".format(size / 1048576f))} م.ب" else "",
                                style = MaterialTheme.typography.bodySmall, color = Palette.Muted,
                            )
                        }
                        if (size > 0) IconButton(onClick = { Reciters.deleteAll(ctx, r); rev++ }) { Icon(Icons.Rounded.Delete, "حذف التنزيلات", tint = Palette.Muted) }
                    }
                }
            }
        }
    }
}

// ============================================================================ about / update

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar("حول التطبيق", onBack)
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(84.dp).background(Palette.Emerald, RoundedCornerShape(26.dp)), contentAlignment = Alignment.Center) {
                Icon(androidx.compose.ui.res.painterResource(com.mtgofa.muslim.R.drawable.ic_stat_mosque), null, tint = Color.White, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("مُسلم", style = MaterialTheme.typography.headlineMedium)
            Text("مواقيت الصلاة والأذان والمصحف", style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
            Text("الإصدار ${BuildConfig.VERSION_NAME} (${arabicDigits(BuildConfig.VERSION_CODE)})", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
        }
        Spacer(Modifier.height(24.dp))
    }
}
