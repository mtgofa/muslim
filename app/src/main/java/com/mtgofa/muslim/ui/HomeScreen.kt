package com.mtgofa.muslim.ui

import androidx.compose.foundation.background
import androidx.compose.runtime.key
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.mtgofa.muslim.data.AlertMode
import com.mtgofa.muslim.data.Settings
import com.mtgofa.muslim.prayer.Hijri
import com.mtgofa.muslim.prayer.Prayer
import com.mtgofa.muslim.prayer.Reliability
import com.mtgofa.muslim.prayer.Scheduler
import com.mtgofa.muslim.prayer.arabicDigits
import com.mtgofa.muslim.quran.QuranDb
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

fun prayerIcon(p: Prayer): ImageVector = when (p) {
    Prayer.FAJR -> Icons.Rounded.WbTwilight
    Prayer.SUNRISE -> Icons.Rounded.WbSunny
    Prayer.DHUHR -> Icons.Rounded.LightMode
    Prayer.ASR -> Icons.Rounded.Cloud
    Prayer.MAGHRIB -> Icons.Rounded.WbTwilight
    Prayer.ISHA -> Icons.Rounded.Bedtime
}

private val TIME = DateTimeFormatter.ofPattern("h:mm", Locale.US)
private val AMPM = DateTimeFormatter.ofPattern("a", Locale.US)

/** Times use Western digits (12:05), like a digital clock. */
fun clock(t: ZonedDateTime): String = t.format(TIME)
fun ampm(t: ZonedDateTime) = if (t.format(AMPM) == "AM") "ص" else "م"

private val AR_DAYS = listOf("الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد")
private val AR_MONTHS = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")
fun gregorian(d: LocalDate) = "${AR_DAYS[d.dayOfWeek.value - 1]} ${arabicDigits(d.dayOfMonth)} ${AR_MONTHS[d.monthValue - 1]} ${arabicDigits(d.year)}"

/** How long after an adhan the header keeps counting up from it. */
private val SINCE_WINDOW: Duration = Duration.ofMinutes(20)

fun countdown(d: Duration): String {
    val s = d.seconds.coerceAtLeast(0)
    return "%02d:%02d:%02d".format(java.util.Locale.US, s / 3600, (s / 60) % 60, s % 60)
}

@Composable
fun HomeScreen(
    onLocation: () -> Unit,
    onPrayer: (Prayer) -> Unit,
    onOpenQuran: (Int) -> Unit,
    onReliability: () -> Unit,
    onQibla: () -> Unit,
    onQuran: () -> Unit,
    onAdhkar: () -> Unit,
    onTasbeeh: () -> Unit,
    onHisn: () -> Unit,
    onSettings: () -> Unit,
    onWird: () -> Unit,
    onReadWird: (com.mtgofa.muslim.data.Khatma) -> Unit,
    onFasting: () -> Unit,
    onQiyam: () -> Unit,
) {
    val ctx = LocalContext.current
    val changes by Settings.changes.collectAsState()
    var now by remember { mutableStateOf(ZonedDateTime.now(Settings.zone)) }
    // tick once a second, on the second, only while this screen is visible: a ticker that
    // keeps running in the background wakes the CPU every second and drains the battery
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            while (true) {
                now = ZonedDateTime.now(Settings.zone)
                delay(1000 - System.currentTimeMillis() % 1000)
            }
        }
    }
    val today = now.toLocalDate()
    val times = remember(today, changes) { Scheduler.times(today) }
    val (next, nextAt) = remember(now.withSecond(0).withNano(0), changes) { Scheduler.nextPrayer(now) }
    var healthy by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) { healthy = Reliability.allGood(ctx); onPauseOrDispose { } }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            // the previous prayer (possibly yesterday's Isha) to measure progress toward the next
            val (prevPrayer, prev) = remember(today, changes, next) {
                val all = Scheduler.times(today.minusDays(1)).times.entries + Scheduler.times(today).times.entries
                all.filter { !it.value.isAfter(now) }.maxByOrNull { it.value }?.toPair() ?: (Prayer.ISHA to now.minusHours(6))
            }
            val total = Duration.between(prev, nextAt).seconds.coerceAtLeast(1)
            val since = Duration.between(prev, now)
            // for a while after an adhan the header counts up ("الظهر منذ") before moving on
            val justCalled = prevPrayer.hasAdhan && since < SINCE_WINDOW
            PrayerHeader(
                period = remember(now.withSecond(0).withNano(0), changes) { SkyPeriod.of(Scheduler.currentPrayer(now)) },
                location = Settings.locationName,
                hijri = Hijri.of(today).toString(),
                date = gregorian(today),
                label = if (justCalled) "أُذّن للصلاة" else "الصلاة القادمة",
                nextName = when {
                    justCalled -> "${prevPrayer.label(prev.toLocalDate())} منذ"
                    next == Prayer.SUNRISE -> "الشروق"
                    else -> next.label(nextAt.toLocalDate())
                },
                nextTime = if (justCalled) "${clock(prev)} ${ampm(prev)}" else "${clock(nextAt)} ${ampm(nextAt)}",
                remaining = if (justCalled) countdown(since) else countdown(Duration.between(now, nextAt)),
                progress = if (justCalled) since.seconds.toFloat() / SINCE_WINDOW.seconds
                    else 1f - Duration.between(now, nextAt).seconds.toFloat() / total,
                onLocation = onLocation,
            )
        }
        // the phone keeps another country's time than the chosen city: likely the wrong place
        // (a visitor still on the default Cairo), so the times would be off by the difference
        val phoneZone = java.time.ZoneId.systemDefault()
        val placeZone = Settings.zone
        val zonePair = "${phoneZone.id}|${placeZone.id}"
        if (phoneZone.rules != placeZone.rules && Settings.zoneWarnDismissed != zonePair) item {
            Card(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), color = Palette.GoldSoft, onClick = onLocation) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.WarningAmber, null, tint = Color(0xFF9A6B12))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("هل أنت في ${Settings.locationName}؟", style = MaterialTheme.typography.titleSmall, color = Color(0xFF6B4A12))
                            Text("توقيت هاتفك يختلف عن توقيت ${Settings.locationName}، فقد تكون المواقيت لمكان آخر. حدّد موقعك.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF8A6420))
                        }
                        Icon(Icons.Rounded.ChevronLeft, null, tint = Color(0xFF9A6B12))
                    }
                    androidx.compose.material3.TextButton(onClick = { Settings.zoneWarnDismissed = zonePair }, modifier = Modifier.align(Alignment.End)) {
                        Text("نعم، أنا هنا", color = Color(0xFF6B4A12))
                    }
                }
            }
        }
        if (!healthy) item {
            Card(
                Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                color = Palette.GoldSoft,
                onClick = onReliability,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.WarningAmber, null, tint = Color(0xFF9A6B12))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("اضبط الهاتف ليعمل الأذان في وقته", style = MaterialTheme.typography.titleSmall, color = Color(0xFF6B4A12))
                        Text("بعض أذونات الإشعارات أو البطارية غير مفعّلة", style = MaterialTheme.typography.bodySmall, color = Color(0xFF8A6420))
                    }
                    Icon(Icons.Rounded.ChevronLeft, null, tint = Color(0xFF9A6B12))
                }
            }
        }
        item {
            // the six times side by side; tap one for its settings
            Card(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), padding = PaddingValues(horizontal = 2.dp, vertical = 12.dp)) {
                Row {
                    Prayer.entries.forEach { p ->
                        PrayerColumn(
                            p, times[p], isNext = p == next && nextAt.toLocalDate() == today, date = today, rev = changes,
                            modifier = Modifier.weight(1f),
                        ) { onPrayer(p) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                val night = remember(today, changes) { nightMarks(today) }
                Row(Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NightChip("منتصف الليل", night.first, Modifier.weight(1f))
                    NightChip("الثلث الأخير", night.second, Modifier.weight(1f), alarm = Settings.qiyamOn, onClick = onQiyam)
                }
            }
        }
        item {
            val page = Settings.lastPage
            val sura = remember(page) { runCatching { QuranDb.page(page).suras.firstOrNull()?.let { QuranDb.surah(it).name } }.getOrNull() }
            ContinueReading(sura, page) { onOpenQuran(page) }
        }
        item {
            val wirdRev by com.mtgofa.muslim.data.Wird.changes.collectAsState()
            key(wirdRev) {
                val list = com.mtgofa.muslim.data.Wird.active
                if (list.isNotEmpty()) KhatmaSlider(list, onWird, onReadWird)
            }
        }
        // everything else as square tiles, three per row (this replaces the bottom bar)
        item {
            val tiles = listOf(
                Tile("القرآن الكريم", Icons.Rounded.MenuBook, Color(0xFF0F7A62), Color(0xFFE3F2EC), onQuran),
                Tile("الأذكار", Icons.Rounded.AutoStories, Color(0xFF6A55B5), Color(0xFFEEE8FF), onAdhkar),
                Tile("السبحة", null, Color(0xFFB8893A), Color(0xFFFFF3E0), onTasbeeh),
                Tile("القبلة", Icons.Rounded.Explore, Color(0xFF2B7FC1), Color(0xFFE3F0FB), onQibla),
                Tile("الورد اليومي", Icons.Rounded.EventNote, Color(0xFFC0392B), Color(0xFFFDEDEB), onWird),
                Tile("الصيام", Icons.Rounded.NightsStay, Color(0xFF3F51B5), Color(0xFFE8EAF6), onFasting),
                Tile("حصن المسلم", Icons.Rounded.Shield, Color(0xFF1F7A5A), Color(0xFFE6F4EF), onHisn),
                Tile("الإعدادات", Icons.Rounded.Settings, Color(0xFF5E6A64), Color(0xFFEFF1EE), onSettings),
            )
            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                tiles.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { t -> TileView(t, Modifier.weight(1f)) }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        item {
            Text(
                "الإصدار \u2066${com.mtgofa.muslim.BuildConfig.VERSION_NAME}\u2069",
                style = MaterialTheme.typography.bodySmall, color = Palette.Muted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp),
            )
        }
    }
}

/** Active khatmas side by side, one card in view, with a dot per khatma underneath. */
@Composable
private fun KhatmaSlider(list: List<com.mtgofa.muslim.data.Khatma>, onOpen: () -> Unit, onRead: (com.mtgofa.muslim.data.Khatma) -> Unit) {
    val pager = androidx.compose.foundation.pager.rememberPagerState { list.size }
    Column(Modifier.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        androidx.compose.foundation.pager.HorizontalPager(
            pager,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 10.dp,
            verticalAlignment = Alignment.Top,
        ) { i -> val k = list[i]; KhatmaCard(k, onOpen) { onRead(k) } }
        if (list.size > 1) Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(list.size) { i ->
                val on = i == pager.currentPage
                Box(
                    Modifier.height(7.dp).width(if (on) 18.dp else 7.dp)
                        .background(if (on) Color(0xFFC0392B) else Color(0x33C0392B), CircleShape),
                )
            }
        }
    }
}

private class Tile(val title: String, val icon: ImageVector?, val tint: Color, val bg: Color, val onClick: () -> Unit)

@Composable
private fun TileView(t: Tile, modifier: Modifier) {
    Column(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(22.dp))
            .background(t.bg)
            .clickable(onClick = t.onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(52.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
            if (t.icon != null) Icon(t.icon, null, tint = t.tint, modifier = Modifier.size(28.dp))
            else Icon(androidx.compose.ui.res.painterResource(com.mtgofa.muslim.R.drawable.ic_tasbeeh), null, tint = t.tint, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(t.title, style = MaterialTheme.typography.titleSmall, color = Palette.Ink, maxLines = 1)
    }
}

@Composable
private fun ContinueReading(sura: String?, page: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF14936F), Color(0xFF0A5A48))))
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp).background(Color(0x33FFFFFF), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.MenuBook, null, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("متابعة القراءة", style = MaterialTheme.typography.labelLarge, color = Color(0xD9FFFFFF))
            Text(sura?.let { "سورة $it" } ?: "القرآن الكريم", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Text("صفحة ${arabicDigits(page)} · الجزء ${JUZ_NAMES[QuranDb.juzOfPage(page) - 1]}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFF2D59B))
        }
        Icon(Icons.Rounded.ChevronLeft, null, tint = Color.White)
    }
}

/** Colour of each prayer's icon. */
private fun prayerTint(p: Prayer): Color = when (p) {
    Prayer.FAJR -> Color(0xFF2F8FC4)
    Prayer.SUNRISE -> Color(0xFFF08A3C)
    Prayer.DHUHR -> Color(0xFFF2B705)
    Prayer.ASR -> Color(0xFF9A86D9)
    Prayer.MAGHRIB -> Color(0xFFF07B3C)
    Prayer.ISHA -> Color(0xFF2C3E8C)
}

@Composable
@Suppress("UNUSED_PARAMETER") // rev: re-read settings after edits
private fun PrayerColumn(p: Prayer, t: ZonedDateTime, isNext: Boolean, date: LocalDate, rev: Int, modifier: Modifier, onClick: () -> Unit) {
    val mode = Settings.mode(p)
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isNext) Palette.EmeraldSoft else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            p.label(date), style = MaterialTheme.typography.titleSmall, maxLines = 1,
            color = if (isNext) Palette.EmeraldDark else Palette.Ink,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .size(48.dp)
                .background(if (isNext) Color.White else prayerTint(p).copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(prayerIcon(p), null, tint = prayerTint(p), modifier = Modifier.size(28.dp)) }
        Spacer(Modifier.height(6.dp))
        Text(clock(t), fontSize = 19.sp, fontWeight = FontWeight.Bold, fontFamily = Plex, color = if (isNext) Palette.EmeraldDark else Palette.Ink, maxLines = 1)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(ampm(t), style = MaterialTheme.typography.labelMedium, color = Palette.Muted)
            if (mode == AlertMode.SILENT) {
                Spacer(Modifier.width(3.dp))
                Icon(Icons.Rounded.NotificationsOff, mode.ar, tint = Palette.Muted.copy(alpha = 0.6f), modifier = Modifier.size(11.dp))
            }
        }
    }
}

@Composable
private fun NightChip(label: String, t: ZonedDateTime, modifier: Modifier, alarm: Boolean = false, onClick: (() -> Unit)? = null) {
    Row(
        modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text("$label: ", style = MaterialTheme.typography.labelLarge)
        Text("${clock(t)} ${ampm(t)}", style = MaterialTheme.typography.labelLarge, color = Color(0xFF2F8FC4))
        // the night prayer alarm lives behind this chip
        if (onClick != null) Icon(
            if (alarm) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff, "تنبيه قيام الليل",
            tint = if (alarm) Palette.Emerald else Palette.Muted.copy(alpha = 0.45f), modifier = Modifier.padding(start = 4.dp).size(16.dp),
        )
    }
}

/** Middle and start of the last third of the night (Maghrib to the next Fajr). */
private fun nightMarks(today: LocalDate): Pair<ZonedDateTime, ZonedDateTime> {
    val maghrib = Scheduler.times(today)[Prayer.MAGHRIB]
    val fajr = Scheduler.times(today.plusDays(1))[Prayer.FAJR]
    val night = Duration.between(maghrib, fajr)
    return maghrib.plus(night.dividedBy(2)) to maghrib.plus(night.multipliedBy(2).dividedBy(3))
}
