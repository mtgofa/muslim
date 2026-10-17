package com.mtgofa.muslem.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.rounded.Search
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Palette as PaletteIcon
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtgofa.muslem.data.Settings
import com.mtgofa.muslem.prayer.arabicDigits
import com.mtgofa.muslem.quran.AyahRef
import com.mtgofa.muslem.quran.MushafPack
import com.mtgofa.muslem.quran.MushafPageView
import com.mtgofa.muslem.quran.MushafStyle
import com.mtgofa.muslem.quran.QuranDb
import com.mtgofa.muslem.quran.QuranPlayer
import com.mtgofa.muslem.quran.Reciters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MushafScreen(startPage: Int, focus: AyahRef?, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val pack by MushafPack.state.collectAsState()
    LaunchedEffect(Unit) { MushafPack.refresh(ctx) }
    if (pack is MushafPack.State.Ready) MushafReader(startPage, focus, onBack) else PackGate(pack, onBack)
}

/** First open: the page fonts are fetched once (≈70 MB) and then work offline forever. */
@Composable
private fun PackGate(st: MushafPack.State, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(Palette.Bg)) {
        TopBar("المصحف", onBack)
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(88.dp).background(Palette.GoldSoft, RoundedCornerShape(28.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Download, null, tint = Palette.Gold, modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("تجهيز المصحف", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "صفحات مصحف المدينة بخطّه الأصلي وألوان التجويد تُنزَّل مرة واحدة فقط (حوالي ٧٠ ميجابايت)، ثم يعمل المصحف بدون إنترنت دائماً.",
                style = MaterialTheme.typography.bodyMedium, color = Palette.Muted, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            when (st) {
                is MushafPack.State.Downloading -> {
                    LinearProgressIndicator({ st.progress }, Modifier.fillMaxWidth().height(8.dp), color = Palette.Emerald)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (st.unpacking) "جاري التجهيز…" else "جاري التنزيل ${arabicDigits((st.progress * 100).toInt())}٪",
                        style = MaterialTheme.typography.labelLarge, color = Palette.Muted,
                    )
                }
                else -> {
                    if (st is MushafPack.State.Failed) {
                        Text(st.reason, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(10.dp))
                    }
                    Button(
                        onClick = { scope.launch { MushafPack.download(ctx) } },
                        modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Palette.Emerald),
                    ) { Text(if (st is MushafPack.State.Failed) "إعادة المحاولة" else "تنزيل المصحف") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MushafReader(startPage: Int, focus: AyahRef?, onBack: () -> Unit) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val changes by Settings.changes.collectAsState()
    val play by QuranPlayer.state.collectAsState()
    val pager = rememberPagerState(initialPage = (startPage - 1).coerceIn(0, 603)) { 604 }
    var chrome by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf(focus) }
    var sheet by remember { mutableStateOf<String?>(null) }
    val style = remember(changes) { MushafStyle.of(Settings.tajweed, Settings.mushafNight) }
    val night = Settings.mushafNight
    val page = pager.currentPage + 1

    // full screen: clock, battery and navigation bar hidden; a tap brings them back with the bars
    val bars = remember(view) {
        (view.context as? android.app.Activity)?.window?.let { androidx.core.view.WindowCompat.getInsetsController(it, view) }
    }
    DisposableEffect(chrome, changes) {
        view.keepScreenOn = Settings.keepScreenOn
        bars?.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        bars?.isAppearanceLightStatusBars = !night
        bars?.isAppearanceLightNavigationBars = !night
        val types = androidx.core.view.WindowInsetsCompat.Type.systemBars()
        if (chrome) bars?.show(types) else bars?.hide(types)
        onDispose {
            view.keepScreenOn = false
            bars?.show(types)
            bars?.isAppearanceLightStatusBars = true
            bars?.isAppearanceLightNavigationBars = true
        }
    }
    LaunchedEffect(pager) { snapshotFlow { pager.currentPage }.collect { Settings.lastPage = it + 1 } }
    // show the bars briefly on open, then give the whole screen to the page
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(2500); chrome = false }
    // follow the recitation across pages
    LaunchedEffect(play.current) {
        val cur = play.current ?: return@LaunchedEffect
        val target = if (cur.ayah == 0) QuranDb.surah(cur.sura).page else QuranDb.pageOf(cur.sura, cur.ayah)
        if (target != pager.currentPage + 1) pager.animateScrollToPage(target - 1)
    }

    val paper = if (night) Palette.PaperNight else Palette.Paper
    val inkSoft = if (night) Color(0xFFB9B3A3) else Color(0xFF8A7A5A)
    val barBg = if (night) Color(0xF2202627) else Color(0xF7FFFFFF)
    val barInk = if (night) Color.White else Palette.Ink

    Box(Modifier.fillMaxSize().background(paper)) {
        // the page always uses the whole screen; bars float over it
        HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1, key = { it }) { idx ->
            val p = idx + 1
            val data = remember(p) { QuranDb.page(p) }
            val suraName = data.suras.firstOrNull()?.let { QuranDb.surah(it).name } ?: ""
            // odd pages are right-hand pages: the spine (binding) is on their left
            val rightHand = p % 2 == 1
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .align(if (rightHand) androidx.compose.ui.AbsoluteAlignment.CenterLeft else androidx.compose.ui.AbsoluteAlignment.CenterRight)
                        .fillMaxHeight()
                        .width(18.dp)
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                if (rightHand) listOf(Color(if (night) 0x55000000 else 0x22000000), Color.Transparent)
                                else listOf(Color.Transparent, Color(if (night) 0x55000000 else 0x22000000)),
                            ),
                        ),
                )
                Column(
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.displayCutout)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Row(Modifier.fillMaxWidth().clickable { chrome = !chrome }.padding(horizontal = 8.dp, vertical = 2.dp)) {
                        Text("سورة $suraName", color = inkSoft, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                        Text("الجزء ${JUZ_NAMES[QuranDb.juzOfPage(p) - 1]}", color = inkSoft, style = MaterialTheme.typography.labelMedium)
                    }
                    val hl = if (play.active && play.current != null) play.current else selected
                    MushafPageView(
                        page = p,
                        style = style,
                        highlight = hl,
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 4.dp),
                        onTap = { a ->
                            if (a == null || selected == a) { selected = null; chrome = !chrome }
                            else { selected = a; sheet = "ayah" }
                        },
                        onLongPress = { a -> selected = a; sheet = "ayah" },
                    )
                    PageBadge(p, night, Modifier.fillMaxWidth().clickable { chrome = !chrome }.padding(horizontal = 4.dp))
                }
            }
        }

        // ---- top bar
        AnimatedVisibility(chrome, enter = fadeIn() + slideInVertically { -it }, exit = fadeOut() + slideOutVertically { -it }) {
            val marked = remember(changes, page) { page in Settings.bookmarks() }
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(barBg)
                    .statusBarsPadding()
                    .height(58.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "رجوع", tint = barInk) }
                Column(Modifier.weight(1f).clickable { sheet = "index" }) {
                    val sura = QuranDb.page(page).suras.firstOrNull()?.let { QuranDb.surah(it).name } ?: ""
                    Text("سورة $sura", style = MaterialTheme.typography.titleMedium, color = barInk)
                    Text("صفحة ${arabicDigits(page)} · الجزء ${arabicDigits(QuranDb.juzOfPage(page))} · الحزب ${arabicDigits((QuranDb.quarterOfPage(page) + 3) / 4)}", style = MaterialTheme.typography.bodySmall, color = inkSoft)
                }
                IconButton(onClick = { sheet = "index" }) { Icon(Icons.Rounded.Search, "الفهرس والبحث", tint = barInk) }
                IconButton(onClick = { Settings.toggleBookmark(page) }) {
                    Icon(if (marked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, "علامة", tint = if (marked) Palette.Gold else barInk)
                }
                IconButton(onClick = { Settings.tajweed = !Settings.tajweed }) {
                    Icon(Icons.Rounded.PaletteIcon, "التجويد", tint = if (Settings.tajweed) Palette.Emerald else inkSoft)
                }
                IconButton(onClick = { Settings.mushafNight = !Settings.mushafNight }) {
                    Icon(Icons.Rounded.DarkMode, "الوضع الليلي", tint = if (night) Palette.Gold else inkSoft)
                }
            }
        }

        // ---- floating player: shown and hidden with the bars, never pushes the page
        Box(Modifier.align(Alignment.BottomCenter)) {
            AnimatedVisibility(chrome, enter = fadeIn() + slideInVertically { it }, exit = fadeOut() + slideOutVertically { it }) {
                PlayerBar(
                    page = page,
                    night = night,
                    onReciter = { sheet = "reciter" },
                    onRepeat = { sheet = "repeat" },
                    onIndex = { sheet = "index" },
                )
            }
        }
        play.error?.let { err ->
            Text(
                err, color = Color.White, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 64.dp, start = 24.dp, end = 24.dp)
                    .background(Color(0xE0B3261E), RoundedCornerShape(12.dp)).padding(10.dp),
            )
        }
    }

    when (sheet) {
        "ayah" -> selected?.let { a ->
            AyahSheet(a, onDismiss = { sheet = null }, onRepeat = { sheet = "repeat" })
        }
        "repeat" -> RepeatSheet(selected ?: QuranDb.page(page).firstAyah?.let { AyahRef(it.first, it.second) } ?: AyahRef(1, 1)) { sheet = null }
        "reciter" -> ReciterSheet(currentSura = QuranDb.page(page).suras.firstOrNull() ?: 1) { sheet = null }
        "index" -> ModalBottomSheet(
            onDismissRequest = { sheet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Palette.Bg,
        ) {
            Box(Modifier.fillMaxWidth().fillMaxHeight(0.92f)) {
                QuranIndexScreen(embedded = true) { target, s, a ->
                    selected = if (s != null && a != null && a > 1) AyahRef(s, a) else null
                    sheet = null
                    chrome = false
                    scope.launch { pager.scrollToPage(target - 1) }
                }
            }
        }
    }
}

@Composable
private fun PlayerBar(page: Int, night: Boolean, onReciter: () -> Unit, onRepeat: () -> Unit, onIndex: () -> Unit) {
    val ctx = LocalContext.current
    val play by QuranPlayer.state.collectAsState()
    val changes by Settings.changes.collectAsState()
    val reciter = remember(changes) { Reciters.byId(Settings.reciter) }
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(10.dp)
            .background(if (night) Color(0xFF232A2B) else Palette.Surface, RoundedCornerShape(24.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides if (night) Color(0xFFE9E6DC) else Palette.Ink,
        ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onReciter)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(reciter.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = androidx.compose.material3.LocalContentColor.current)
                val cur = play.current
                Text(
                    when {
                        cur == null || !play.active -> reciter.style + " · اضغط لتغيير القارئ"
                        cur.ayah == 0 -> "سورة ${QuranDb.surah(cur.sura).name} · البسملة"
                        else -> "سورة ${QuranDb.surah(cur.sura).name} · آية ${arabicDigits(cur.ayah)}"
                    },
                    style = MaterialTheme.typography.bodySmall, color = Palette.Muted, maxLines = 1,
                )
            }
            val repeatOn = Settings.ayahRepeat > 1 || Settings.rangeRepeat != 1
            if (!play.active) IconButton(onClick = onIndex) { Icon(Icons.AutoMirrored.Rounded.List, "الفهرس") }
            IconButton(onClick = onRepeat) {
                Icon(if (Settings.ayahRepeat > 1) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, "التكرار", tint = if (repeatOn) Palette.Emerald else Palette.Muted)
            }
            if (play.active) IconButton(onClick = { QuranPlayer.skip(ctx, forward = false) }) { Icon(Icons.Rounded.SkipNext, "السابق") }
            Box(
                Modifier
                    .size(48.dp)
                    .background(Palette.Emerald, CircleShape)
                    .clickable {
                        if (play.active) QuranPlayer.toggle(ctx)
                        else {
                            // play from the first ayah on this page to the end of its surah, then continue
                            val first = QuranDb.page(page).firstAyah ?: (1 to 1)
                            QuranPlayer.play(ctx, AyahRef(first.first, first.second), AyahRef(first.first, QuranDb.surah(first.first).ayahs), continuous = true)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (play.buffering) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                else Icon(if (play.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "تشغيل", tint = Color.White)
            }
            if (play.active) {
                IconButton(onClick = { QuranPlayer.skip(ctx, forward = true) }) { Icon(Icons.Rounded.SkipPrevious, "التالي") }
                IconButton(onClick = { QuranPlayer.stop(ctx) }) { Icon(Icons.Rounded.Close, "إيقاف", tint = Palette.Muted) }
            }
        }
        }
    }
}

// ============================================================================ ayah actions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AyahSheet(a: AyahRef, onDismiss: () -> Unit, onRepeat: () -> Unit) {
    val ctx = LocalContext.current
    val changes by Settings.changes.collectAsState()
    val s = QuranDb.surah(a.sura)
    val text = remember(a) { QuranDb.ayahText(a.sura, a.ayah) }
    val marked = remember(changes) { (a.sura to a.ayah) in Settings.ayahMarks() }
    val label = "سورة ${s.name} · آية ${arabicDigits(a.ayah)}"
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = Palette.Emerald)
            Spacer(Modifier.height(8.dp))
            Text(
                text, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 21.sp, lineHeight = 38.sp),
                modifier = Modifier.background(Palette.Paper, RoundedCornerShape(16.dp)).padding(14.dp).fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Action(Icons.Rounded.PlayArrow, "تشغيل من هنا", Modifier.weight(1f)) {
                    QuranPlayer.play(ctx, a, AyahRef(a.sura, s.ayahs), continuous = true); onDismiss()
                }
                Action(Icons.Rounded.RepeatOne, "تكرار الآية", Modifier.weight(1f)) {
                    Settings.ayahRepeat = maxOf(3, Settings.ayahRepeat); Settings.rangeRepeat = 1
                    QuranPlayer.play(ctx, a, a); onDismiss()
                }
                Action(Icons.Rounded.Repeat, "تكرار مقطع", Modifier.weight(1f)) { onRepeat() }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Action(if (marked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkAdd, if (marked) "إزالة العلامة" else "حفظ علامة", Modifier.weight(1f)) {
                    Settings.toggleAyahMark(a.sura, a.ayah)
                }
                Action(Icons.Rounded.ContentCopy, "نسخ", Modifier.weight(1f)) {
                    ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("آية", "$text\n[$label]"))
                    Toast.makeText(ctx, "تم نسخ الآية", Toast.LENGTH_SHORT).show(); onDismiss()
                }
                Action(Icons.Rounded.Share, "مشاركة", Modifier.weight(1f)) {
                    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "﴿$text﴾\n[$label]"), "مشاركة الآية"))
                    onDismiss()
                }
            }
        }
    }
}

@Composable
private fun Action(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .background(Palette.EmeraldSoft, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = Palette.Emerald)
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = Palette.EmeraldDark)
    }
}

// ============================================================================ repeat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepeatSheet(start: AyahRef, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var fromS by remember { mutableStateOf(start.sura) }
    var fromA by remember { mutableStateOf(start.ayah.coerceAtLeast(1)) }
    var toS by remember { mutableStateOf(start.sura) }
    var toA by remember { mutableStateOf(QuranDb.surah(start.sura).ayahs) }
    var perAyah by remember { mutableStateOf(Settings.ayahRepeat) }
    var rounds by remember { mutableStateOf(Settings.rangeRepeat) }
    var speed by remember { mutableStateOf(Settings.playSpeed) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
            Text("التشغيل والتكرار", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            Label("من")
            AyahPicker(fromS, fromA) { s, a ->
                fromS = s; fromA = a
                if (toS < s || (toS == s && toA < a)) { toS = s; toA = QuranDb.surah(s).ayahs }
            }
            Label("إلى")
            AyahPicker(toS, toA) { s, a ->
                toS = s; toA = a
                if (s < fromS || (s == fromS && a < fromA)) { fromS = s; fromA = a }
            }
            Spacer(Modifier.height(10.dp))
            Line("تكرار كل آية") {
                Stepper(perAyah, 1..20, format = { arabicDigits(it) + " مرة" }) { perAyah = it }
            }
            Line("تكرار المقطع كاملاً") {
                Stepper(if (rounds < 0) 21 else rounds, 1..21, format = { if (it > 20) "بلا توقف" else arabicDigits(it) + " مرة" }) { rounds = if (it > 20) -1 else it }
            }
            Spacer(Modifier.height(8.dp))
            Label("سرعة التلاوة")
            Segmented(listOf(0.75f, 1f, 1.25f, 1.5f), speed, { arabicDigits(it.toString().removeSuffix(".0")) + "×" }) { speed = it }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    Settings.ayahRepeat = perAyah; Settings.rangeRepeat = rounds; Settings.playSpeed = speed
                    QuranPlayer.play(ctx, AyahRef(fromS, fromA), AyahRef(toS, toA))
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Palette.Emerald),
            ) { Icon(Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("ابدأ التلاوة") }
        }
    }
}

@Composable
private fun Label(t: String) = Text(t, style = MaterialTheme.typography.labelLarge, color = Palette.Muted, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))

@Composable
private fun Line(title: String, trailing: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        trailing()
    }
}

/** Surah stepper + ayah stepper. */
@Composable
private fun AyahPicker(sura: Int, ayah: Int, onChange: (Int, Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            Text("سورة ${QuranDb.surah(sura).name}", style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Stepper(sura, 1..114, format = { arabicDigits(it) }) { onChange(it, 1) }
        }
        Column(Modifier.weight(1f)) {
            Text("آية", style = MaterialTheme.typography.titleSmall)
            Stepper(ayah, 1..QuranDb.surah(sura).ayahs, format = { arabicDigits(it) }) { onChange(sura, it) }
        }
    }
}

// ============================================================================ reciter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReciterSheet(currentSura: Int, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val changes by Settings.changes.collectAsState()
    var progress by remember { mutableStateOf<Float?>(null) }
    var rev by remember { mutableStateOf(0) }
    val listState = rememberLazyListState(Reciters.all.indexOfFirst { it.id == Settings.reciter }.coerceAtLeast(0))
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        key(changes, rev) {
            val r = Reciters.byId(Settings.reciter)
            val downloaded = remember(changes, rev) { Reciters.isSurahDownloaded(ctx, r, currentSura) }
            Column(Modifier.navigationBarsPadding()) {
                Text("القارئ", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 20.dp))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
                        .background(Palette.EmeraldSoft, RoundedCornerShape(16.dp))
                        .clickable(enabled = !downloaded && progress == null) {
                            progress = 0f
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) { Reciters.downloadSurah(ctx, r, currentSura) { progress = it } }
                                progress = null; rev++
                                Toast.makeText(ctx, if (ok) "تم تنزيل السورة" else "تعذّر التنزيل — تأكّد من الاتصال", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(if (downloaded) Icons.Rounded.DownloadDone else Icons.Rounded.Download, null, tint = Palette.Emerald)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (downloaded) "سورة ${QuranDb.surah(currentSura).name} محمّلة للاستماع بدون إنترنت"
                            else "تنزيل سورة ${QuranDb.surah(currentSura).name} بصوت ${r.name}",
                            style = MaterialTheme.typography.titleSmall, color = Palette.EmeraldDark,
                        )
                        progress?.let { LinearProgressIndicator({ it }, Modifier.fillMaxWidth().padding(top = 6.dp)) }
                    }
                }
                LazyColumn(state = listState, contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 16.dp), modifier = Modifier.height(420.dp)) {
                    items(Reciters.all, key = { it.id }) { rc ->
                        Row(
                            Modifier.fillMaxWidth().clickable { Settings.reciter = rc.id }.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(rc.id == Settings.reciter, { Settings.reciter = rc.id })
                            Column {
                                Text(rc.name, style = MaterialTheme.typography.titleMedium)
                                Text(rc.style, style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Page number in an ornamented badge on the page's outer edge (right on right-hand pages). */
@Composable
private fun PageBadge(page: Int, night: Boolean, modifier: Modifier) {
    val rightHand = page % 2 == 1
    Box(modifier, contentAlignment = if (rightHand) androidx.compose.ui.AbsoluteAlignment.CenterRight else androidx.compose.ui.AbsoluteAlignment.CenterLeft) {
        Box(Modifier.width(128.dp).height(36.dp), contentAlignment = Alignment.Center) {
            val col = remember(night) { com.mtgofa.muslem.quran.OrnamentColors(night) }
            var box by remember { mutableStateOf<android.graphics.RectF?>(null) }
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                drawIntoCanvas { c ->
                    box = com.mtgofa.muslem.quran.Ornaments.pageBadge(c.nativeCanvas, size.width, size.height, rightHand, col)
                }
            }
            val density = androidx.compose.ui.platform.LocalDensity.current
            val b = box
            if (b != null) Box(
                Modifier
                    .align(androidx.compose.ui.AbsoluteAlignment.TopLeft)
                    .absolutePadding(left = with(density) { b.left.toDp() }, top = with(density) { b.top.toDp() })
                    .size(with(density) { b.width().toDp() }, with(density) { b.height().toDp() }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    arabicDigits(page), style = MaterialTheme.typography.labelLarge, fontSize = 13.sp,
                    color = if (night) Color(0xFFE9E6DC) else Color(0xFF2F7039),
                )
            }
        }
    }
}
