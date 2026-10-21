package com.mtgofa.muslim.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtgofa.muslim.data.Adhkar
import com.mtgofa.muslim.data.Settings
import com.mtgofa.muslim.prayer.arabicDigits
import com.mtgofa.muslim.quran.QuranDb
import kotlinx.coroutines.launch

private fun featuredIcon(id: Int): ImageVector = when (id) {
    27 -> Icons.Rounded.WbSunny
    25 -> Icons.Rounded.Mosque
    28 -> Icons.Rounded.Bedtime
    1 -> Icons.Rounded.WbTwilight
    15 -> Icons.Rounded.VolumeUp
    13, 14 -> Icons.Rounded.Mosque
    9 -> Icons.Rounded.WaterDrop
    10, 11 -> Icons.Rounded.Home
    69, 70 -> Icons.Rounded.Restaurant
    96 -> Icons.Rounded.DirectionsCar
    34 -> Icons.Rounded.Favorite
    else -> Icons.Rounded.AutoStories
}

@Composable
fun AdhkarHomeScreen(onBack: () -> Unit, onHisn: () -> Unit, onChapter: (Int) -> Unit) {
    val ctx = LocalContext.current
    val contentVersion by com.mtgofa.muslim.data.Content.version.collectAsState()
    val chapters = remember(contentVersion) { Adhkar.all(ctx) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { TopBar("الأذكار", onBack) }
        item { HisnCard(chapters.size, onHisn) }
        item { SectionTitle("الأذكار اليومية") }
        // the five main categories as large illustrated cards, the rest as smaller ones in pairs
        val (big, small) = Adhkar.featured.partition { it.id in BIG_CARDS }
        items(big, key = { "big${it.id}" }) { f ->
            val n = chapters.firstOrNull { it.id == f.id }?.items?.size ?: 0
            BigAdhkarCard(f.name, n, adhkarTheme(f.id)) { onChapter(f.id) }
        }
        items(small.chunked(2)) { pair ->
            Row(Modifier.padding(horizontal = 16.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { f ->
                    val n = chapters.firstOrNull { it.id == f.id }?.items?.size ?: 0
                    SmallAdhkarCard(f.name, n, adhkarTheme(f.id), Modifier.weight(1f)) { onChapter(f.id) }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private val BIG_CARDS = setOf(Adhkar.MORNING, Adhkar.EVENING, 25, 28, 1, 15)

@Composable
private fun BigAdhkarCard(name: String, count: Int, theme: AdhkarTheme, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .height(124.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(theme.bg)
            .clickable(onClick = onClick),
    ) {
        // drawing on the left (RTL: the end), title on the right
        AdhkarArt(theme, Modifier.align(androidx.compose.ui.AbsoluteAlignment.CenterLeft).fillMaxHeight().fillMaxWidth(0.62f))
        Column(Modifier.align(androidx.compose.ui.AbsoluteAlignment.CenterRight).padding(horizontal = 22.dp)) {
            Text(name, style = MaterialTheme.typography.headlineSmall, color = Palette.Ink)
            Text("${arabicDigits(count)} ذكر", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
        }
    }
}

@Composable
private fun SmallAdhkarCard(name: String, count: Int, theme: AdhkarTheme, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(theme.bg)
            .clickable(onClick = onClick),
    ) {
        AdhkarArt(theme, Modifier.fillMaxWidth().height(78.dp))
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text("${arabicDigits(count)} ذكر", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
        }
    }
}

@Composable
private fun HisnCard(chapters: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF14936F), Color(0xFF0A5A48))))
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("حصن المسلم", color = Color.White, style = MaterialTheme.typography.headlineSmall)
            Text("${arabicDigits(chapters)} باباً من الأذكار والأدعية مع البحث", color = Color(0xD9FFFFFF), style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.Rounded.ChevronLeft, null, tint = Color.White)
    }
}

/** The whole Hisn al-Muslim on its own page: every chapter, searchable. */
@Composable
fun HisnScreen(onBack: () -> Unit, onChapter: (Int) -> Unit) {
    val ctx = LocalContext.current
    val contentVersion by com.mtgofa.muslim.data.Content.version.collectAsState()
    val chapters = remember(contentVersion) { Adhkar.all(ctx) }
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(query, chapters) {
        val q = QuranDb.normalize(query.trim())
        if (q.isEmpty()) chapters else chapters.filter { QuranDb.normalize(it.title).contains(q) }
    }
    Column(Modifier.fillMaxSize()) {
        TopBar("حصن المسلم", onBack)
        OutlinedTextField(
            query, { query = it },
            placeholder = { Text("ابحث في الأبواب") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            singleLine = true, shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(filtered, key = { it.id }) { c ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onChapter(c.id) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NumberOrnament(c.id, Palette.Emerald)
                    Spacer(Modifier.width(12.dp))
                    Text(c.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(arabicDigits(c.items.size), style = MaterialTheme.typography.labelMedium, color = Palette.Muted)
                    Icon(Icons.Rounded.ChevronLeft, null, tint = Palette.Muted)
                }
            }
        }
    }
}

/** One chapter: each dhikr is a card; tapping it counts down its repeats. */
@Composable
fun AdhkarChapterScreen(id: Int, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val contentVersion by com.mtgofa.muslim.data.Content.version.collectAsState()
    val chapter = remember(id, contentVersion) { Adhkar.chapter(ctx, id) } ?: run { onBack(); return }
    val left = remember(chapter) { mutableStateListOf<Int>().apply { addAll(chapter.items.map { it.repeat }) } }
    val list = rememberLazyListState()
    val done = left.count { it == 0 }
    val title = Adhkar.featured.firstOrNull { it.id == id }?.name ?: chapter.title

    Column(Modifier.fillMaxSize()) {
        TopBar(title, onBack) {
            if (id == Adhkar.MORNING || id == Adhkar.EVENING) ReminderBell(id)
            IconButton(onClick = { chapter.items.forEachIndexed { i, d -> left[i] = d.repeat } }) {
                Icon(Icons.Rounded.RestartAlt, "إعادة")
            }
        }
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                { done / chapter.items.size.toFloat() },
                Modifier.weight(1f).height(6.dp).clip(CircleShape),
                color = Palette.Emerald, trackColor = Palette.EmeraldSoft,
            )
            Spacer(Modifier.width(12.dp))
            Text("${arabicDigits(done)} من ${arabicDigits(chapter.items.size)}", style = MaterialTheme.typography.labelMedium, color = Palette.Muted)
        }
        LazyColumn(state = list, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(chapter.items) { i, d ->
                val finished = left[i] == 0
                val bg by animateColorAsState(if (finished) Palette.EmeraldSoft else MaterialTheme.colorScheme.surface, label = "bg")
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(bg)
                        .border(1.dp, if (finished) Palette.Emerald.copy(alpha = 0.3f) else Palette.Line, RoundedCornerShape(20.dp))
                        .clickable(enabled = !finished) {
                            left[i] = left[i] - 1
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            // move on to the next dhikr once this one is complete
                            if (left[i] == 0 && i + 1 < chapter.items.size) scope.launch { list.animateScrollToItem(i + 1) }
                        }
                        .padding(16.dp),
                ) {
                    DhikrBody(d.text, finished)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            when { d.repeat == 1 -> "مرة واحدة"; d.repeat == 2 -> "مرتان"; d.repeat <= 10 -> "${arabicDigits(d.repeat)} مرات"; else -> "${arabicDigits(d.repeat)} مرة" },
                            style = MaterialTheme.typography.labelMedium, color = Palette.Muted, modifier = Modifier.weight(1f),
                        )
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (finished) Palette.Emerald else Palette.GoldSoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (finished) Icon(Icons.Rounded.Check, null, tint = Color.White)
                            else Text(arabicDigits(left[i]), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF8A5E14))
                        }
                    }
                }
            }
        }
    }
}

/** The bell in the morning / evening chapter's bar: lit when its daily reminder is on. */
@Composable
private fun ReminderBell(which: Int) {
    val changes by Settings.changes.collectAsState()
    var open by remember { mutableStateOf(false) }
    val on = remember(changes) { if (which == Adhkar.MORNING) Settings.adhkarMorningOn else Settings.adhkarEveningOn }
    IconButton(onClick = { open = true }) {
        Icon(
            if (on) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff, "التذكير",
            tint = if (on) Palette.Emerald else Palette.Muted,
        )
    }
    if (open) AdhkarReminderSheet(which) { open = false }
}

/** This chapter's daily reminder: on/off, its time, and a sound or vibration only. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun AdhkarReminderSheet(which: Int, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val changes by Settings.changes.collectAsState()
    var picking by remember { mutableStateOf(false) }
    val notifPerm = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }
    val morning = which == Adhkar.MORNING
    fun changed(turnedOn: Boolean = false) {
        com.mtgofa.muslim.data.AdhkarReminder.schedule(ctx)
        if (turnedOn && android.os.Build.VERSION.SDK_INT >= 33) notifPerm.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        androidx.compose.runtime.key(changes) {
            val on = if (morning) Settings.adhkarMorningOn else Settings.adhkarEveningOn
            val at = if (morning) Settings.adhkarMorningAt else Settings.adhkarEveningAt
            Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
                Text(
                    if (morning) "تذكير أذكار الصباح" else "تذكير أذكار المساء",
                    style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(8.dp))
                SwitchRow("تذكير كل يوم", if (on) "اضغط على الوقت لتغييره" else "بدون تذكير", Icons.Rounded.NotificationsActive, on) {
                    if (morning) Settings.adhkarMorningOn = it else Settings.adhkarEveningOn = it
                    changed(it)
                }
                if (on) {
                    TimeChip(at) { picking = true }
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        Text("طريقة التذكير", style = MaterialTheme.typography.labelLarge, color = Palette.Muted)
                        Spacer(Modifier.height(6.dp))
                        Segmented(listOf(false, true), Settings.adhkarVibrate, { if (it) "اهتزاز فقط" else "إشعار بصوت" }) { Settings.adhkarVibrate = it }
                        androidx.compose.material3.TextButton(onClick = { com.mtgofa.muslim.data.AdhkarReminder.show(ctx, which) }) { Text("جرّب التذكير") }
                    }
                }
            }
            if (picking) {
                val state = androidx.compose.material3.rememberTimePickerState(at / 60, at % 60, is24Hour = false)
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { picking = false },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = {
                            val v = state.hour * 60 + state.minute
                            if (morning) Settings.adhkarMorningAt = v else Settings.adhkarEveningAt = v
                            picking = false; changed()
                        }) { Text("تم") }
                    },
                    dismissButton = { androidx.compose.material3.TextButton(onClick = { picking = false }) { Text("إلغاء") } },
                    text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { androidx.compose.material3.TimePicker(state) } },
                )
            }
        }
    }
}

private fun clockText(min: Int): String {
    val h = (min / 60) % 12
    return "%d:%02d %s".format(java.util.Locale.US, if (h == 0) 12 else h, min % 60, if (min < 12 * 60) "ص" else "م")
}

@Composable
private fun TimeChip(min: Int, onClick: () -> Unit) {
    Text(
        clockText(min), fontFamily = Plex, fontSize = 18.sp, color = Palette.EmeraldDark,
        modifier = Modifier.padding(start = 56.dp, bottom = 8.dp)
            .clip(RoundedCornerShape(12.dp)).background(Palette.EmeraldSoft).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}
