package com.mtgofa.muslim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtgofa.muslim.data.Settings
import com.mtgofa.muslim.prayer.arabicDigits
import com.mtgofa.muslim.quran.QuranDb
import com.mtgofa.muslim.quran.SearchHit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

val JUZ_NAMES = listOf(
    "الأول", "الثاني", "الثالث", "الرابع", "الخامس", "السادس", "السابع", "الثامن", "التاسع", "العاشر",
    "الحادي عشر", "الثاني عشر", "الثالث عشر", "الرابع عشر", "الخامس عشر", "السادس عشر", "السابع عشر",
    "الثامن عشر", "التاسع عشر", "العشرون", "الحادي والعشرون", "الثاني والعشرون", "الثالث والعشرون",
    "الرابع والعشرون", "الخامس والعشرون", "السادس والعشرون", "السابع والعشرون", "الثامن والعشرون",
    "التاسع والعشرون", "الثلاثون",
)

/** Number inside a small eight-point ornament, used for surah and juz numbers. */
@Composable
fun NumberOrnament(n: Int, color: Color = Palette.Gold) {
    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(28.dp).rotate(45f).border(1.6.dp, color, RoundedCornerShape(6.dp)))
        Box(Modifier.size(28.dp).border(1.6.dp, color, RoundedCornerShape(6.dp)))
        Text(arabicDigits(n), fontSize = if (n > 99) 11.sp else 13.sp, fontWeight = FontWeight.Bold, color = Palette.Ink, fontFamily = Plex)
    }
}

@Composable
fun QuranIndexScreen(embedded: Boolean = false, onBack: (() -> Unit)? = null, onOpen: (page: Int, sura: Int?, ayah: Int?) -> Unit) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val changes by Settings.changes.collectAsState()

    Column(Modifier.fillMaxSize()) {
        if (embedded) Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("الفهرس", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = { searching = !searching; if (!searching) query = "" }) {
                Icon(if (searching) Icons.Rounded.Close else Icons.Rounded.Search, "بحث")
            }
        } else TopBar("القرآن الكريم", onBack) {
            IconButton(onClick = { searching = !searching; if (!searching) query = "" }) {
                Icon(if (searching) Icons.Rounded.Close else Icons.Rounded.Search, "بحث")
            }
        }
        if (searching) {
            OutlinedTextField(
                query, { query = it },
                placeholder = { Text("ابحث بكلمة من الآية") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            )
            SearchResults(query, onOpen)
            return@Column
        }
        Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Segmented(listOf(0, 1, 2), tab, { listOf("السور", "الأجزاء", "العلامات")[it] }) { tab = it }
        }
        when (tab) {
            0 -> LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)) {
                if (!embedded) item { ContinueCard(onOpen) }
                items(QuranDb.surahs, key = { it.id }) { s ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(s.page, s.id, 1) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NumberOrnament(s.id)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("سورة ${s.name}", style = MaterialTheme.typography.titleMedium)
                            Text(
                                (if (s.mecca) "مكية" else "مدنية") + " · ${arabicDigits(s.ayahs)} آية",
                                style = MaterialTheme.typography.bodySmall, color = Palette.Muted,
                            )
                        }
                        Text("ص ${arabicDigits(s.page)}", style = MaterialTheme.typography.labelMedium, color = Palette.Muted)
                    }
                }
            }
            1 -> LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)) {
                items(QuranDb.juz, key = { it.id }) { j ->
                    val s = QuranDb.surah(j.sura)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(j.page, j.sura, j.ayah) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NumberOrnament(j.id, Palette.Emerald)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("الجزء ${JUZ_NAMES[j.id - 1]}", style = MaterialTheme.typography.titleMedium)
                            Text("يبدأ من سورة ${s.name} · آية ${arabicDigits(j.ayah)}", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
                        }
                        Text("ص ${arabicDigits(j.page)}", style = MaterialTheme.typography.labelMedium, color = Palette.Muted)
                    }
                }
            }
            else -> {
                val pages = remember(changes) { Settings.bookmarks() }
                val ayahs = remember(changes) { Settings.ayahMarks() }
                if (pages.isEmpty() && ayahs.isEmpty()) {
                    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.BookmarkBorder, null, tint = Palette.Muted, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.size(12.dp))
                        Text("لا توجد علامات بعد", style = MaterialTheme.typography.titleMedium)
                        Text("اضغط على أي آية في المصحف واختر «حفظ علامة»", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
                    }
                } else LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    items(ayahs, key = { "a${it.first}:${it.second}" }) { (s, a) ->
                        BookmarkRow("سورة ${QuranDb.surah(s).name} · آية ${arabicDigits(a)}", QuranDb.ayahText(s, a)) {
                            onOpen(QuranDb.pageOf(s, a), s, a)
                        }
                    }
                    items(pages, key = { "p$it" }) { p ->
                        val sura = QuranDb.page(p).suras.firstOrNull()?.let { QuranDb.surah(it).name } ?: ""
                        BookmarkRow("صفحة ${arabicDigits(p)}", "سورة $sura · الجزء ${JUZ_NAMES[QuranDb.juzOfPage(p) - 1]}") { onOpen(p, null, null) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinueCard(onOpen: (Int, Int?, Int?) -> Unit) {
    val page = Settings.lastPage
    val sura = remember(page) { QuranDb.page(page).suras.firstOrNull()?.let { QuranDb.surah(it).name } ?: "" }
    Card(Modifier.padding(bottom = 8.dp), color = Palette.Emerald, onClick = { onOpen(page, null, null) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.MenuBook, tint = Color.White, bg = Color(0x33FFFFFF))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("متابعة القراءة", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text("سورة $sura · صفحة ${arabicDigits(page)}", style = MaterialTheme.typography.bodySmall, color = Color(0xD9FFFFFF))
            }
            Icon(Icons.Rounded.ChevronLeft, null, tint = Color.White)
        }
    }
}

@Composable
private fun BookmarkRow(title: String, sub: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Bookmark, null, tint = Palette.Gold)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = Palette.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SearchResults(query: String, onOpen: (Int, Int?, Int?) -> Unit) {
    var hits by remember { mutableStateOf<List<SearchHit>>(emptyList()) }
    LaunchedEffect(query) {
        delay(250)
        hits = withContext(Dispatchers.IO) { QuranDb.search(query) }
    }
    if (query.length >= 2 && hits.isEmpty()) {
        Text("لا توجد نتائج", color = Palette.Muted, modifier = Modifier.padding(24.dp))
    }
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        if (hits.isNotEmpty()) item {
            Text("${arabicDigits(hits.size)}${if (hits.size >= 200) "+" else ""} نتيجة", style = MaterialTheme.typography.labelLarge, color = Palette.Muted, modifier = Modifier.padding(bottom = 8.dp))
        }
        items(hits, key = { "${it.sura}:${it.ayah}" }) { h ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .clickable { onOpen(h.page, h.sura, h.ayah) }
                    .padding(14.dp),
            ) {
                Text("سورة ${QuranDb.surah(h.sura).name} · آية ${arabicDigits(h.ayah)}", style = MaterialTheme.typography.labelLarge, color = Palette.Emerald)
                Text(h.text, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 32.sp))
            }
        }
    }
}
