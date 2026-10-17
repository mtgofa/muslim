package com.mtgofa.muslem.ui

import androidx.compose.foundation.background
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
import com.mtgofa.muslem.data.AlertMode
import com.mtgofa.muslem.data.Settings
import com.mtgofa.muslem.prayer.Hijri
import com.mtgofa.muslem.prayer.Prayer
import com.mtgofa.muslem.prayer.Reliability
import com.mtgofa.muslem.prayer.Scheduler
import com.mtgofa.muslem.prayer.arabicDigits
import com.mtgofa.muslem.quran.QuranDb
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
) {
    val ctx = LocalContext.current
    val changes by Settings.changes.collectAsState()
    var now by remember { mutableStateOf(ZonedDateTime.now(Settings.zone)) }
    LaunchedEffect(Unit) { while (true) { now = ZonedDateTime.now(Settings.zone); delay(1000) } }
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
            val prev = remember(today, changes, next) {
                val all = Scheduler.times(today.minusDays(1)).times.values + Scheduler.times(today).times.values
                all.filter { !it.isAfter(now) }.maxOrNull() ?: now.minusHours(6)
            }
            val total = Duration.between(prev, nextAt).seconds.coerceAtLeast(1)
            PrayerHeader(
                period = SkyPeriod.of(Scheduler.currentPrayer(now)),
                location = Settings.locationName,
                hijri = Hijri.of(today).toString(),
                date = gregorian(today),
                nextName = if (next == Prayer.SUNRISE) "الشروق" else next.label(nextAt.toLocalDate()),
                nextTime = "${clock(nextAt)} ${ampm(nextAt)}",
                remaining = countdown(Duration.between(now, nextAt)),
                progress = 1f - Duration.between(now, nextAt).seconds.toFloat() / total,
                onLocation = onLocation,
            )
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
            Card(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), padding = PaddingValues(vertical = 6.dp)) {
                Prayer.entries.forEachIndexed { i, p ->
                    PrayerRow(p, times[p], isNext = p == next && nextAt.toLocalDate() == today, date = today, rev = changes) { onPrayer(p) }
                    if (i < Prayer.entries.lastIndex) RowDivider()
                }
            }
        }
        item {
            val page = Settings.lastPage
            val sura = remember(page) { runCatching { QuranDb.page(page).suras.firstOrNull()?.let { QuranDb.surah(it).name } }.getOrNull() }
            Card(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), onClick = { onOpenQuran(page) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.MenuBook, tint = Color(0xFF8A5E14), bg = Palette.GoldSoft)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("متابعة القراءة", style = MaterialTheme.typography.titleMedium)
                        Text(
                            (sura?.let { "سورة $it · " } ?: "") + "صفحة ${arabicDigits(page)}",
                            style = MaterialTheme.typography.bodySmall, color = Palette.Muted,
                        )
                    }
                    Icon(Icons.Rounded.ChevronLeft, null, tint = Palette.Muted)
                }
            }
        }
    }
}

@Composable
@Suppress("UNUSED_PARAMETER") // rev: re-read settings after edits
private fun PrayerRow(p: Prayer, t: ZonedDateTime, isNext: Boolean, date: LocalDate, rev: Int, onClick: () -> Unit) {
    val mode = Settings.mode(p)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isNext) Palette.EmeraldSoft else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(prayerIcon(p), tint = if (isNext) Color.White else Palette.Emerald, bg = if (isNext) Palette.Emerald else Palette.EmeraldSoft, size = 38.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(p.label(date), style = MaterialTheme.typography.titleMedium, color = if (isNext) Palette.EmeraldDark else Palette.Ink)
            val extra = buildList {
                val pre = Settings.preAlert(p); if (pre > 0) add("تنبيه قبلها ${arabicDigits(pre)} د")
                val iq = Settings.iqama(p); if (iq > 0 && p.hasAdhan) add("الإقامة بعد ${arabicDigits(iq)} د")
            }
            if (extra.isNotEmpty()) Text(extra.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
        }
        Text(clock(t), style = MaterialTheme.typography.titleLarge, color = if (isNext) Palette.EmeraldDark else Palette.Ink)
        Spacer(Modifier.width(4.dp))
        Text(ampm(t), style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
        Spacer(Modifier.width(12.dp))
        Icon(
            when (mode) { AlertMode.ADHAN -> Icons.Rounded.VolumeUp; AlertMode.NOTIFY -> Icons.Rounded.NotificationsActive; AlertMode.SILENT -> Icons.Rounded.NotificationsOff },
            mode.ar,
            tint = if (mode == AlertMode.SILENT) Palette.Muted.copy(alpha = 0.5f) else Palette.Gold,
            modifier = Modifier.size(20.dp),
        )
    }
}
