package com.mtgofa.muslem

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Settings as SettingsIcon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mtgofa.muslem.data.Settings
import com.mtgofa.muslem.data.Updater
import com.mtgofa.muslem.prayer.Prayer
import com.mtgofa.muslem.prayer.Reliability
import com.mtgofa.muslem.prayer.Scheduler
import com.mtgofa.muslem.quran.AyahRef
import com.mtgofa.muslem.quran.QuranDb
import com.mtgofa.muslem.quran.QuranPlayer
import com.mtgofa.muslem.ui.AboutScreen
import com.mtgofa.muslem.ui.AdhkarChapterScreen
import com.mtgofa.muslem.ui.AdhkarHomeScreen
import com.mtgofa.muslem.ui.HisnScreen
import com.mtgofa.muslem.ui.TasbeehScreen
import com.mtgofa.muslem.ui.HomeScreen
import com.mtgofa.muslem.ui.LocationScreen
import com.mtgofa.muslem.ui.MuezzinScreen
import com.mtgofa.muslem.ui.MushafScreen
import com.mtgofa.muslem.ui.MuslemTheme
import com.mtgofa.muslem.ui.Palette
import com.mtgofa.muslem.ui.PrayerSheet
import com.mtgofa.muslem.ui.PrayersScreen
import com.mtgofa.muslem.ui.QuranIndexScreen
import com.mtgofa.muslem.ui.RecitersScreen
import com.mtgofa.muslem.ui.ReliabilityScreen
import com.mtgofa.muslem.ui.SettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface Route {
    data object Tabs : Route
    data object Location : Route
    data object Muezzin : Route
    data object Prayers : Route
    data object Reliability : Route
    data object Reciters : Route
    data object About : Route
    data class Mushaf(val page: Int, val focus: AyahRef?) : Route
    data object Hisn : Route
    data class Adhkar(val id: Int) : Route
}

class MainActivity : ComponentActivity() {
    private val openMushaf = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        Settings.init(this)
        openMushaf.value = intent?.getBooleanExtra("openMushaf", false) == true
        setContent { MuslemTheme { App(openMushaf) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("openMushaf", false)) openMushaf.value = true
    }

    override fun onResume() {
        super.onResume()
        Scheduler.schedule(this)
    }
}

@Composable
private fun App(openMushaf: MutableStateFlow<Boolean>) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var dbReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { QuranDb.init(ctx) }
        dbReady = true
        QuranPlayer.reconnect(ctx)
        Updater.check(silent = true)
        com.mtgofa.muslem.data.Content.sync()
        withContext(Dispatchers.IO) {
            val fajr = com.mtgofa.muslem.prayer.Muezzins.byId(Settings.fajrMuezzin)
            if (Settings.fajrMuezzin.isNotEmpty() && !com.mtgofa.muslem.prayer.Muezzins.isAvailable(ctx, fajr)) {
                com.mtgofa.muslem.prayer.Muezzins.download(ctx, fajr)
            }
        }
    }
    val stack = remember { mutableStateListOf<Route>(Route.Tabs) }
    var tab by remember { mutableStateOf(0) }
    var prayerSheet by remember { mutableStateOf<Prayer?>(null) }
    var onboarded by remember { mutableStateOf(Settings.onboarded) }
    fun push(r: Route) { stack.add(r) }
    fun pop() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }

    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(onboarded) {
        if (onboarded && Build.VERSION.SDK_INT >= 33 && !Reliability.notificationsOn(ctx)) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    val wantMushaf by openMushaf.collectAsState()
    LaunchedEffect(wantMushaf, dbReady) {
        if (wantMushaf && dbReady) {
            openMushaf.value = false
            if (stack.lastOrNull() !is Route.Mushaf) push(Route.Mushaf(Settings.lastPage, null))
        }
    }

    BackHandler(stack.size > 1) { pop() }
    BackHandler(stack.size == 1 && tab != 0) { tab = 0 }

    if (!dbReady) {
        Box(Modifier.fillMaxSize().background(Palette.Bg), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    if (!onboarded) {
        if (stack.lastOrNull() == Route.Location) {
            LocationScreen(onBack = { pop(); if (Settings.hasLocation) { Settings.onboarded = true; onboarded = true } })
        } else Welcome(
            onLocation = { push(Route.Location) },
            onSkip = { Settings.onboarded = true; onboarded = true; Scheduler.schedule(ctx) },
        )
        return
    }

    AnimatedContent(stack.last(), transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "nav") { route ->
        Box(Modifier.fillMaxSize().background(Palette.Bg)) {
            when (route) {
                Route.Tabs -> Tabs(
                    tab, { tab = it },
                    home = {
                        HomeScreen(
                            onLocation = { push(Route.Location) },
                            onPrayer = { prayerSheet = it },
                            onOpenQuran = { push(Route.Mushaf(it, null)) },
                            onReliability = { push(Route.Reliability) },
                        )
                    },
                    quran = { QuranIndexScreen { page, s, a -> push(Route.Mushaf(page, if (s != null && a != null && a > 1) AyahRef(s, a) else null)) } },
                    tasbeeh = { TasbeehScreen() },
                    adhkar = { AdhkarHomeScreen(onHisn = { push(Route.Hisn) }, onChapter = { push(Route.Adhkar(it)) }) },
                    settings = {
                        SettingsScreen(
                            onLocation = { push(Route.Location) },
                            onMuezzin = { push(Route.Muezzin) },
                            onPrayers = { push(Route.Prayers) },
                            onReliability = { push(Route.Reliability) },
                            onReciters = { push(Route.Reciters) },
                            onAbout = { push(Route.About) },
                        )
                    },
                )
                Route.Location -> LocationScreen(::pop)
                Route.Muezzin -> MuezzinScreen(::pop)
                Route.Prayers -> PrayersScreen(::pop)
                Route.Reliability -> ReliabilityScreen(::pop)
                Route.Reciters -> RecitersScreen(::pop)
                Route.About -> AboutScreen(::pop)
                is Route.Mushaf -> MushafScreen(route.page, route.focus, ::pop)
                Route.Hisn -> HisnScreen(::pop) { push(Route.Adhkar(it)) }
                is Route.Adhkar -> AdhkarChapterScreen(route.id, ::pop)
            }
        }
    }
    prayerSheet?.let { PrayerSheet(it) { prayerSheet = null } }

    // ---- "new version" popup
    val prompt by Updater.prompt.collectAsState()
    val ust by Updater.state.collectAsState()
    val avail = ust as? Updater.State.Available
    if (prompt && avail != null) {
        AlertDialog(
            onDismissRequest = { Updater.prompt.value = false },
            title = { Text("إصدار جديد ${avail.rel.tag}") },
            text = { Text(avail.rel.notes.ifBlank { "يتوفر تحديث للتطبيق. هل تريد تنزيله وتثبيته الآن؟" }) },
            confirmButton = { TextButton(onClick = { scope.launch { Updater.install(ctx, avail.rel) } }) { Text("تحديث الآن") } },
            dismissButton = { TextButton(onClick = { Updater.prompt.value = false }) { Text("لاحقاً") } },
        )
    }
}

@Composable
private fun Tabs(
    tab: Int, onTab: (Int) -> Unit,
    home: @Composable () -> Unit, quran: @Composable () -> Unit, tasbeeh: @Composable () -> Unit,
    adhkar: @Composable () -> Unit, settings: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (tab) { 0 -> home(); 1 -> quran(); 2 -> tasbeeh(); 3 -> adhkar(); else -> settings() }
        }
        NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
            val colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Palette.Emerald, selectedTextColor = Palette.Emerald, indicatorColor = Palette.EmeraldSoft,
                unselectedIconColor = Palette.Muted, unselectedTextColor = Palette.Muted,
            )
            NavigationBarItem(tab == 0, { onTab(0) }, { Icon(Icons.Rounded.Mosque, null) }, label = { Text("الصلاة") }, colors = colors)
            NavigationBarItem(tab == 1, { onTab(1) }, { Icon(Icons.Rounded.MenuBook, null) }, label = { Text("القرآن") }, colors = colors)
            NavigationBarItem(tab == 2, { onTab(2) }, { Icon(painterResource(R.drawable.ic_tasbeeh), null) }, label = { Text("السبحة") }, colors = colors)
            NavigationBarItem(tab == 3, { onTab(3) }, { Icon(Icons.Rounded.SelfImprovement, null) }, label = { Text("الأذكار") }, colors = colors)
            NavigationBarItem(tab == 4, { onTab(4) }, { Icon(Icons.Rounded.SettingsIcon, null) }, label = { Text("الإعدادات") }, colors = colors)
        }
    }
}

@Composable
private fun Welcome(onLocation: () -> Unit, onSkip: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Palette.Bg), contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(96.dp).background(Palette.Emerald, RoundedCornerShape(30.dp)), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_stat_mosque), null, tint = Color.White, modifier = Modifier.size(56.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("السلام عليكم", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "مواقيت الصلاة والأذان في وقته، والمصحف الشريف بألوان التجويد وأصوات كبار القرّاء.\nلنبدأ بتحديد موقعك لحساب المواقيت بدقة.",
                style = MaterialTheme.typography.bodyLarge, color = Palette.Muted, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onLocation, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Palette.Emerald),
            ) { Text("تحديد الموقع") }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
                Text("المتابعة بمواقيت القاهرة")
            }
        }
    }
}
