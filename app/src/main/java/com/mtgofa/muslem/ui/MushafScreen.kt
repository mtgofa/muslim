@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Palette as PaletteIcon
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipPath
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
import com.mtgofa.muslem.quran.Tafsir
import androidx.compose.runtime.produceState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.TextButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MushafScreen(startPage: Int, focus: AyahRef?, wird: Long, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val pack by MushafPack.state.collectAsState()
    LaunchedEffect(Unit) { MushafPack.refresh(ctx) }
    if (pack is MushafPack.State.Ready) MushafReader(startPage, focus, wird, onBack) else PackGate(pack, onBack)
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
/**
 * [wird] != 0: reading that khatma's wird — its status shows in the header and turning past its
 * next page records it; the ordinary reading place is left alone. Otherwise the reverse.
 */
private fun MushafReader(startPage: Int, focus: AyahRef?, wird: Long, onBack: () -> Unit) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val changes by Settings.changes.collectAsState()
    val play by QuranPlayer.state.collectAsState()
    val pager = rememberPagerState(initialPage = (startPage - 1).coerceIn(0, 603)) { 604 }
    var chrome by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf(focus) }
    var sheet by remember { mutableStateOf<String?>(null) }
    // meanings mode: words with a meaning are marked; tapping one shows it in a card
    var wordMode by remember { mutableStateOf(false) }
    var word by remember { mutableStateOf<Pair<com.mtgofa.muslem.data.WordMeaning, AyahRef>?>(null) }
    val style = remember(changes) { MushafStyle.of(Settings.tajweed) }
    val page = pager.currentPage + 1

    // full screen: clock, battery and navigation bar hidden; a tap brings them back with the bars
    val bars = remember(view) {
        (view.context as? android.app.Activity)?.window?.let { androidx.core.view.WindowCompat.getInsetsController(it, view) }
    }
    DisposableEffect(chrome, changes) {
        view.keepScreenOn = Settings.keepScreenOn
        bars?.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // light status icons over the dark top bar; the navigation bar sits over the page
        bars?.isAppearanceLightStatusBars = !chrome
        bars?.isAppearanceLightNavigationBars = true
        val types = androidx.core.view.WindowInsetsCompat.Type.systemBars()
        if (chrome) bars?.show(types) else bars?.hide(types)
        onDispose {
            view.keepScreenOn = false
            bars?.show(types)
            bars?.isAppearanceLightStatusBars = true
            bars?.isAppearanceLightNavigationBars = true
        }
    }
    val ctx = LocalContext.current
    val wirdRev by com.mtgofa.muslem.data.Wird.changes.collectAsState()
    LaunchedEffect(pager) {
        var prev = pager.currentPage + 1
        snapshotFlow { pager.currentPage }.collect {
            val now = it + 1
            // turning forward past the wird's next page records it as read
            if (wird != 0L) com.mtgofa.muslem.data.Wird.onPageTurn(ctx, wird, prev, now)
            else Settings.lastPage = now
            prev = now
        }
    }
    // show the bars briefly on open, then give the whole screen to the page
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(2500); chrome = false }
    // follow the recitation across pages
    LaunchedEffect(play.current) {
        val cur = play.current ?: return@LaunchedEffect
        val target = if (cur.ayah == 0) QuranDb.surah(cur.sura).page else QuranDb.pageOf(cur.sura, cur.ayah)
        if (target != pager.currentPage + 1) pager.animateScrollToPage(target - 1)
    }

    val paper = Palette.Paper
    val inkSoft = Color(0xFF8A7A5A)
    // dark bars so they stand out over the cream page
    val barBg = BarDark
    val barInk = Color.White
    val barSoft = Color(0xB3FFFFFF)

    Box(Modifier.fillMaxSize().background(paper)) {
        // the page always uses the whole screen; bars float over it
        HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1, key = { it }) { idx ->
            val p = idx + 1
            val data = remember(p) { QuranDb.page(p) }
            val suraName = data.suras.firstOrNull()?.let { QuranDb.surah(it).name } ?: ""
            Box(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.displayCutout)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    // juz on the right, surah name on the left, as printed
                    // juz on the right, surah name on the left, as printed; the wird between them
                    Row(Modifier.fillMaxWidth().clickable { chrome = !chrome }.padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("الجزء ${JUZ_NAMES[QuranDb.juzOfPage(p) - 1]}", color = Color(0xFF2A2A2A), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                        if (wird != 0L) key(wirdRev) { WirdBadge(p, wird) }
                        Text(suraName, color = Color(0xFF2A2A2A), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    }
                    val hl = if (sheet == "ayah" && selected != null) selected else if (play.active && play.current != null) play.current else selected
                    MushafPageView(
                        page = p,
                        style = style,
                        highlight = hl,
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 4.dp),
                        // a tap shows or hides the bars; the ayah menu is on long-press
                        onTap = { selected = null; if (word != null) word = null else chrome = !chrome },
                        onLongPress = { a -> selected = a; sheet = "ayah" },
                        wordMode = wordMode,
                        onWord = { m, a -> word = m to a },
                    )
                    PageFooter(p, Modifier.fillMaxWidth().clickable { chrome = !chrome }.padding(horizontal = 4.dp))
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
                    .height(64.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BarIcon(Icons.AutoMirrored.Rounded.ArrowBack, "رجوع", barInk, onBack)
                Column(Modifier.weight(1f).clickable { sheet = "index" }) {
                    val sura = QuranDb.page(page).suras.firstOrNull()?.let { QuranDb.surah(it).name } ?: ""
                    Text("سورة $sura", style = MaterialTheme.typography.titleMedium, color = barInk)
                    Text("صفحة ${arabicDigits(page)} · الجزء ${arabicDigits(QuranDb.juzOfPage(page))}", style = MaterialTheme.typography.bodySmall, color = barSoft, maxLines = 1)
                }
                // tap: meanings mode on/off; long-press: every meaning on this page
                Box(
                    Modifier.size(52.dp).clip(CircleShape).combinedClickable(
                        onClick = { wordMode = !wordMode; word = null; if (wordMode) chrome = false },
                        onLongClick = { sheet = "meanings" },
                    ),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Lightbulb, "معاني الكلمات", tint = if (wordMode) Palette.Gold else barInk, modifier = Modifier.size(29.dp)) }
                BarIcon(Icons.Rounded.Search, "الفهرس والبحث", barInk) { sheet = "index" }
                BarIcon(if (marked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, "علامة", if (marked) Palette.Gold else barInk) {
                    Settings.toggleBookmark(page)
                }
                BarIcon(Icons.Rounded.PaletteIcon, "التجويد", if (Settings.tajweed) Palette.Gold else barSoft) { sheet = "tajweed" }
            }
        }

        // ---- floating player: shown and hidden with the bars, never pushes the page
        Box(Modifier.align(Alignment.BottomCenter)) {
            AnimatedVisibility(chrome, enter = fadeIn() + slideInVertically { it }, exit = fadeOut() + slideOutVertically { it }) {
                PlayerBar(
                    page = page,
                    onReciter = { sheet = "reciter" },
                    onRepeat = { sheet = "repeat" },
                    onIndex = { sheet = "index" },
                )
            }
        }
        // ---- meanings mode: the tapped word's meaning, and a way to see the whole page
        if (wordMode && word != null) Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()
                .padding(start = 14.dp, end = 14.dp, bottom = if (chrome) 96.dp else 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val w = word
            if (w != null) Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(BarDark).clickable { word = null }.padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(w.first.word, fontFamily = Plex, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 22.sp, color = Color(0xFFF2D59B))
                    Spacer(Modifier.weight(1f))
                    Text("سورة ${QuranDb.surah(w.second.sura).name} · آية ${arabicDigits(w.second.ayah)}", style = MaterialTheme.typography.labelMedium, color = Color(0xB3FFFFFF))
                }
                Spacer(Modifier.height(6.dp))
                Text(w.first.meaning, style = MaterialTheme.typography.bodyLarge, fontSize = 18.sp, lineHeight = 28.sp, color = Color.White)
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
        "tajweed" -> TajweedSheet { sheet = null }
        "meanings" -> PageMeaningsSheet(page) { sheet = null }
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
private fun PlayerBar(page: Int, onReciter: () -> Unit, onRepeat: () -> Unit, onIndex: () -> Unit) {
    val ctx = LocalContext.current
    val play by QuranPlayer.state.collectAsState()
    val changes by Settings.changes.collectAsState()
    val reciter = remember(changes) { Reciters.byId(Settings.reciter) }
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(10.dp)
            .background(BarDark, RoundedCornerShape(24.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides Color.White,
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
                    style = MaterialTheme.typography.bodySmall, color = Color(0xB3FFFFFF), maxLines = 1,
                )
            }
            val repeatOn = Settings.ayahRepeat > 1 || Settings.rangeRepeat != 1
            if (!play.active) IconButton(onClick = onIndex) { Icon(Icons.AutoMirrored.Rounded.List, "الفهرس") }
            IconButton(onClick = onRepeat) {
                Icon(if (Settings.ayahRepeat > 1) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, "التكرار", tint = if (repeatOn) Palette.Gold else Color.White)
            }
            if (play.active) IconButton(onClick = { QuranPlayer.skip(ctx, forward = false) }) { Icon(Icons.Rounded.SkipNext, "السابق") }
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Palette.Gold)
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
                IconButton(onClick = { QuranPlayer.stop(ctx) }) { Icon(Icons.Rounded.Close, "إيقاف", tint = Color(0xB3FFFFFF)) }
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
        Column(
            Modifier.navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 16.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = Palette.Emerald)
            Spacer(Modifier.height(8.dp))
            Text(
                text, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 21.sp, lineHeight = 38.sp),
                modifier = Modifier.background(Palette.Paper, RoundedCornerShape(16.dp)).padding(14.dp).fillMaxWidth(),
            )
            val meanings = remember(a) { com.mtgofa.muslem.data.Meanings.of(a.sura, a.ayah) }
            // word meanings | tafsir; the last tab chosen stays chosen for the next ayah
            var tab by remember { mutableStateOf(if (meanings.isEmpty()) AyahTab.TAFSIR else lastAyahTab) }
            Spacer(Modifier.height(12.dp))
            Segmented(AyahTab.entries, tab, { it.label }) { tab = it; lastAyahTab = it }
            Spacer(Modifier.height(8.dp))
            when (tab) {
                AyahTab.MEANINGS ->
                    if (meanings.isEmpty()) Text("لا توجد كلمات غريبة في هذه الآية", style = MaterialTheme.typography.bodyMedium, color = Palette.Muted, modifier = Modifier.padding(vertical = 8.dp))
                    else meanings.forEach { MeaningRow(it) }
                AyahTab.TAFSIR -> TafsirPane(a)
            }
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

private enum class AyahTab(val label: String) { MEANINGS("معاني الكلمات"), TAFSIR("التفسير") }

private var lastAyahTab = AyahTab.MEANINGS

/** The ayah's tafsir; the first time, downloads the tafsir file (once, about half a megabyte). */
@Composable
private fun TafsirPane(a: AyahRef) {
    val ctx = LocalContext.current
    val state by Tafsir.state.collectAsState()
    LaunchedEffect(Unit) { Tafsir.refresh(ctx); if (Tafsir.state.value is Tafsir.State.Missing) Tafsir.download(ctx) }
    val scope = rememberCoroutineScope()
    when (val st = state) {
        Tafsir.State.Ready -> {
            val text by produceState<String?>(null, a) { value = Tafsir.of(ctx, a.sura, a.ayah) }
            Text(
                text ?: "", style = MaterialTheme.typography.bodyLarge, lineHeight = 30.sp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).heightIn(min = 60.dp),
            )
            Text(Tafsir.NAME, style = MaterialTheme.typography.labelMedium, color = Palette.Muted)
        }
        is Tafsir.State.Downloading -> Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            Text("جاري تحميل التفسير لمرة واحدة…", style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { st.progress }, modifier = Modifier.fillMaxWidth(), color = Palette.Emerald, trackColor = Palette.EmeraldSoft)
        }
        is Tafsir.State.Failed -> Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(st.reason, style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
            TextButton(onClick = { scope.launch { Tafsir.download(ctx) } }) { Text("إعادة المحاولة") }
        }
        Tafsir.State.Missing -> Unit
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
            Segmented(listOf(0.75f, 1f, 1.25f, 1.5f), speed, { arabicDigits(it.toString().removeSuffix(".0")) + "×" }) { speed = it; QuranPlayer.setSpeed(ctx, it) }
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

/** Surah and ayah as two select fields; each opens a scrollable list. */
@Composable
private fun AyahPicker(sura: Int, ayah: Int, onChange: (Int, Int) -> Unit) {
    var open by remember { mutableStateOf<String?>(null) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SelectField("سورة ${QuranDb.surah(sura).name}", Modifier.weight(1f)) { open = "sura" }
        SelectField("آية ${arabicDigits(ayah)}", Modifier.weight(1f)) { open = "ayah" }
    }
    when (open) {
        "sura" -> PickerDialog("اختر السورة", QuranDb.surahs.size, sura - 1, onDismiss = { open = null }) { i, selected ->
            val s = QuranDb.surahs[i]
            PickerRow(selected, { onChange(s.id, 1); open = null }) {
                Text("سورة ${s.name}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(arabicDigits(s.id), style = MaterialTheme.typography.labelLarge, color = Palette.Muted)
            }
        }
        "ayah" -> {
            val texts = remember(sura) { QuranDb.ayahTexts(sura) }
            PickerDialog("سورة ${QuranDb.surah(sura).name}", texts.size, ayah - 1, onDismiss = { open = null }) { i, selected ->
                PickerRow(selected, { onChange(sura, i + 1); open = null }) {
                    Text(
                        texts[i].split(' ').take(5).joinToString(" ") + " …",
                        style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(arabicDigits(i + 1), style = MaterialTheme.typography.labelLarge, color = Palette.Emerald)
                }
            }
        }
    }
}

@Composable
private fun SelectField(text: String, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Icon(Icons.Rounded.KeyboardArrowDown, null, tint = Palette.Muted)
    }
}

@Composable
private fun PickerDialog(
    title: String, count: Int, current: Int, onDismiss: () -> Unit,
    row: @Composable (index: Int, selected: Boolean) -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            val state = rememberLazyListState((current - 3).coerceAtLeast(0))
            LazyColumn(state = state) {
                items(count) { i -> row(i, i == current) }
            }
        }
    }
}

@Composable
private fun PickerRow(selected: Boolean, onClick: () -> Unit, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (selected) Palette.EmeraldSoft else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
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
                            Modifier.fillMaxWidth().clickable { QuranPlayer.changeReciter(ctx, rc.id) }.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(rc.id == Settings.reciter, { QuranPlayer.changeReciter(ctx, rc.id) })
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

/**
 * Foot of the page, as printed: the page side on the right (an open book with this page's half
 * hatched), the page number in the middle, and the next page's first words on the left.
 */
@Composable
private fun PageFooter(page: Int, modifier: Modifier) {
    val rightHand = page % 2 == 1
    val catchword = remember(page) { QuranDb.catchword(page) }
    val ink = Color(0xFF175E43)
    Box(modifier.height(26.dp)) {
        PageSideIcon(rightHand, ink, Modifier.align(androidx.compose.ui.AbsoluteAlignment.CenterRight).size(30.dp, 20.dp))
        Text(
            arabicDigits(page), fontFamily = Plex, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            fontSize = 14.sp, color = ink, modifier = Modifier.align(Alignment.Center),
        )
        if (catchword.isNotEmpty()) Text(
            catchword, fontFamily = Plex, fontSize = 13.sp, color = Color(0xFF6B5B3E), maxLines = 1,
            modifier = Modifier.align(androidx.compose.ui.AbsoluteAlignment.CenterLeft),
        )
    }
}

/** An open book whose right or left page is hatched, telling which side of the spread this is. */
@Composable
private fun PageSideIcon(rightHand: Boolean, color: Color, modifier: Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val w = size.width; val h = size.height
        val stroke = 1.4.dp.toPx()
        val mid = w / 2
        fun page(left: Boolean): androidx.compose.ui.graphics.Path = androidx.compose.ui.graphics.Path().apply {
            // a page curving away from the spine
            val outer = if (left) stroke else w - stroke
            moveTo(mid, h * 0.18f)
            quadraticBezierTo((mid + outer) / 2, h * 0.02f, outer, h * 0.12f)
            lineTo(outer, h - stroke)
            quadraticBezierTo((mid + outer) / 2, h * 0.86f, mid, h - stroke)
            close()
        }
        val active = page(left = !rightHand)
        // hatch the active page with diagonal lines
        clipPath(active) {
            var x = -h
            while (x < w) {
                drawLine(color.copy(alpha = 0.55f), Offset(x, h), Offset(x + h, 0f), strokeWidth = stroke * 0.8f)
                x += 4.dp.toPx()
            }
        }
        drawPath(page(true), color, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
        drawPath(page(false), color, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
    }
}

// ============================================================================ tajweed

/** Tajweed colours on/off and what each colour means (colours as printed by the page fonts). */
private val TAJWEED_KEY = listOf(
    0xFFB50000 to "مدّ ٦ حركات لزوماً",
    0xFFF40000 to "مدّ واجب ٤ أو ٥ حركات",
    0xFFFF7B00 to "مدّ ٢ أو ٤ أو ٦ جوازاً",
    0xFFCE9E00 to "مدّ حركتان",
    0xFF09B000 to "إخفاء، ومواقع الغُنّة (حركتان)",
    0xFFA5A5A5 to "إدغام، وما لا يُلفَظ",
    0xFF3F48E6 to "تفخيم",
    0xFF2FADFF to "قلقلة",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TajweedSheet(onDismiss: () -> Unit) {
    val changes by Settings.changes.collectAsState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MaterialTheme.colorScheme.surface) {
        key(changes) {
            Column(Modifier.navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("ألوان التجويد", style = MaterialTheme.typography.headlineSmall)
                        Text("تلوين الحروف حسب أحكام التلاوة", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
                    }
                    androidx.compose.material3.Switch(
                        Settings.tajweed, { Settings.tajweed = it },
                        colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = Palette.Emerald),
                    )
                }
                Spacer(Modifier.height(16.dp))
                // two per row: each colour in its own lightly tinted card
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TAJWEED_KEY.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pair.forEach { (color, label) ->
                                val c = Color(color)
                                Row(
                                    Modifier
                                        .weight(1f)
                                        .height(64.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(c.copy(alpha = if (Settings.tajweed) 0.09f else 0.04f))
                                        .border(1.dp, c.copy(alpha = if (Settings.tajweed) 0.35f else 0.12f), RoundedCornerShape(14.dp))
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(Modifier.size(14.dp).clip(CircleShape).background(if (Settings.tajweed) c else c.copy(alpha = 0.35f)))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        label, style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                        color = if (Settings.tajweed) Palette.Ink else Palette.Muted, maxLines = 2, lineHeight = 18.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Background of the Mushaf's floating bars: deep emerald, nearly opaque. */
private val BarDark = Color(0xF20B3A2C)

// ============================================================================ word meanings

@Composable
private fun MeaningRow(m: com.mtgofa.muslem.data.WordMeaning) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Text(
            m.word, fontFamily = Plex, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 17.sp,
            color = Palette.EmeraldDark,
            modifier = Modifier.background(Palette.EmeraldSoft, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(m.meaning, style = MaterialTheme.typography.bodyLarge, lineHeight = 26.sp, modifier = Modifier.weight(1f).padding(top = 3.dp))
    }
}

/** Every word meaning on the page, grouped by ayah. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PageMeaningsSheet(page: Int, onDismiss: () -> Unit) {
    val groups = remember(page) {
        QuranDb.ayahsOnPage(page).map { it to com.mtgofa.muslem.data.Meanings.of(it.sura, it.ayah) }.filter { it.second.isNotEmpty() }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.navigationBarsPadding().fillMaxHeight(0.85f)) {
            Text("معاني كلمات الصفحة ${arabicDigits(page)}", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 20.dp))
            Text("من «السراج في بيان غريب القرآن» و«كلمات القرآن»", style = MaterialTheme.typography.bodySmall, color = Palette.Muted, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(8.dp))
            if (groups.isEmpty()) Text("لا توجد كلمات غريبة في هذه الصفحة", color = Palette.Muted, modifier = Modifier.padding(20.dp))
            LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
                groups.forEach { (ref, list) ->
                    item(key = "h${ref.sura}:${ref.ayah}") {
                        Text(
                            "سورة ${QuranDb.surah(ref.sura).name} · آية ${arabicDigits(ref.ayah)}",
                            style = MaterialTheme.typography.labelLarge, color = Palette.Gold,
                            modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                        )
                    }
                    items(list.size, key = { "w${ref.sura}:${ref.ayah}:$it" }) { MeaningRow(list[it]) }
                }
            }
        }
    }
}

/** A larger icon button for the Mushaf's top bar. */
@Composable
private fun BarIcon(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(52.dp)) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(29.dp))
    }
}

/** The status of the khatma being read, in the page header; on the khatma's last page, a button to record it. */
@Composable
private fun WirdBadge(page: Int, id: Long) {
    val ctx = LocalContext.current
    val k = com.mtgofa.muslem.data.Wird.byId(id) ?: return
    val color = when { k.onTrack -> Palette.Emerald; k.behind > 0 -> Color(0xFFC0392B); else -> Color(0xFFB8893A) }
    Row(
        Modifier.clip(CircleShape).background(color).padding(horizontal = 12.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(k.status(), style = MaterialTheme.typography.labelMedium, color = Color.White, maxLines = 1)
        // the very last page has no page after it to turn to
        if (page == k.next && page == com.mtgofa.muslem.data.Wird.LAST_PAGE) {
            Spacer(Modifier.width(8.dp))
            Text(
                "تمّ ✓", style = MaterialTheme.typography.labelMedium, color = color,
                modifier = Modifier.clip(CircleShape).background(Color.White).clickable { com.mtgofa.muslem.data.Wird.markRead(ctx, k.id) }.padding(horizontal = 8.dp, vertical = 1.dp),
            )
        }
    }
}
