package com.mtgofa.muslim

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
import androidx.compose.material.icons.rounded.AutoStories
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
import com.mtgofa.muslim.data.Settings
import com.mtgofa.muslim.prayer.Prayer
import com.mtgofa.muslim.prayer.Reliability
import com.mtgofa.muslim.prayer.Scheduler
import com.mtgofa.muslim.quran.AyahRef
import com.mtgofa.muslim.quran.QuranDb
import com.mtgofa.muslim.quran.QuranPlayer
import com.mtgofa.muslim.ui.AboutScreen
import com.mtgofa.muslim.ui.AdhkarChapterScreen
import com.mtgofa.muslim.ui.AdhkarHomeScreen
import com.mtgofa.muslim.ui.HisnScreen
import com.mtgofa.muslim.ui.QiblaScreen
import com.mtgofa.muslim.ui.WirdScreen
import com.mtgofa.muslim.ui.FastingScreen
import com.mtgofa.muslim.ui.TasbeehScreen
import com.mtgofa.muslim.ui.HomeScreen
import com.mtgofa.muslim.ui.LocationScreen
import com.mtgofa.muslim.ui.MuezzinScreen
import com.mtgofa.muslim.ui.MushafScreen
import com.mtgofa.muslim.ui.MuslimTheme
import com.mtgofa.muslim.ui.Palette
import com.mtgofa.muslim.ui.PrayerSheet
import com.mtgofa.muslim.ui.PrayersScreen
import com.mtgofa.muslim.ui.QuranIndexScreen
import com.mtgofa.muslim.ui.RecitersScreen
import com.mtgofa.muslim.ui.ReliabilityScreen
import com.mtgofa.muslim.ui.SettingsScreen
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
    /** [wird]: the khatma being read (0 = ordinary reading, which keeps its own place) */
    data class Mushaf(val page: Int, val focus: AyahRef?, val wird: Long = 0) : Route
    data object Hisn : Route
    data object Qibla : Route
    data object QuranIndex : Route
    data object Tasbeeh : Route
    data object AdhkarHome : Route
    data object Settings : Route
    data object Wird : Route
    data object Fasting : Route
    data object Qiyam : Route
    data class Adhkar(val id: Int) : Route
}

class MainActivity : ComponentActivity() {
    private val openMushaf = MutableStateFlow(false)
    /** page to open the Mushaf at (from the wird reminder), 0 = last read */
    private val openPage = MutableStateFlow(0)
    /** the khatma the reminder was for, 0 = none */
    private val openWird = MutableStateFlow(0L)
    /** adhkar chapter to open (from the morning / evening reminder), 0 = none */
    private val openAdhkar = MutableStateFlow(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        Settings.init(this)
        openMushaf.value = intent?.getBooleanExtra("openMushaf", false) == true
        openPage.value = intent?.getIntExtra("page", 0) ?: 0
        openWird.value = intent?.getLongExtra("wird", 0L) ?: 0L
        openAdhkar.value = intent?.getIntExtra("openAdhkar", 0) ?: 0
        setContent { MuslimTheme { App(openMushaf, openPage, openWird, openAdhkar) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getIntExtra("openAdhkar", 0).takeIf { it != 0 }?.let { openAdhkar.value = it }
        if (intent.getBooleanExtra("openMushaf", false)) {
            openPage.value = intent.getIntExtra("page", 0)
            openWird.value = intent.getLongExtra("wird", 0L)
            openMushaf.value = true
        }
    }

    override fun onResume() {
        super.onResume()
        Scheduler.schedule(this)
        com.mtgofa.muslim.data.Qiyam.schedule(this)
    }
}

@Composable
private fun App(openMushaf: MutableStateFlow<Boolean>, openPage: MutableStateFlow<Int>, openWird: MutableStateFlow<Long>, openAdhkar: MutableStateFlow<Int>) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var dbReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { QuranDb.init(ctx) }
        dbReady = true
        QuranPlayer.reconnect(ctx)
        com.mtgofa.muslim.data.Content.sync()
        withContext(Dispatchers.IO) {
            val fajr = com.mtgofa.muslim.prayer.Muezzins.byId(Settings.fajrMuezzin)
            if (Settings.fajrMuezzin.isNotEmpty() && !com.mtgofa.muslim.prayer.Muezzins.isAvailable(ctx, fajr)) {
                com.mtgofa.muslim.prayer.Muezzins.download(ctx, fajr)
            }
        }
    }
    val stack = remember { mutableStateListOf<Route>(Route.Tabs) }
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
            val page = openPage.value.takeIf { it > 0 } ?: Settings.lastPage
            val wird = openWird.value
            openPage.value = 0; openWird.value = 0
            if (stack.lastOrNull() is Route.Mushaf) stack.removeAt(stack.lastIndex)
            push(Route.Mushaf(page, null, wird))
        }
    }

    val wantAdhkar by openAdhkar.collectAsState()
    LaunchedEffect(wantAdhkar, dbReady) {
        if (wantAdhkar != 0 && dbReady) {
            val id = wantAdhkar
            openAdhkar.value = 0
            if (stack.lastOrNull() is Route.Adhkar) stack.removeAt(stack.lastIndex)
            push(Route.Adhkar(id))
        }
    }

    BackHandler(stack.size > 1) { pop() }

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
                Route.Tabs -> HomeScreen(
                    onLocation = { push(Route.Location) },
                    onPrayer = { prayerSheet = it },
                    onOpenQuran = { push(Route.Mushaf(it, null)) },
                    onReliability = { push(Route.Reliability) },
                    onQibla = { push(Route.Qibla) },
                    onQuran = { push(Route.QuranIndex) },
                    onAdhkar = { push(Route.AdhkarHome) },
                    onTasbeeh = { push(Route.Tasbeeh) },
                    onHisn = { push(Route.Hisn) },
                    onSettings = { push(Route.Settings) },
                    onWird = { push(Route.Wird) },
                    onFasting = { push(Route.Fasting) },
                    onQiyam = { push(Route.Qiyam) },
                    onReadWird = { push(Route.Mushaf(it.next.coerceAtMost(com.mtgofa.muslim.data.Wird.LAST_PAGE), null, it.id)) },
                )
                Route.QuranIndex -> QuranIndexScreen(onBack = ::pop) { page, s, a ->
                    push(Route.Mushaf(page, if (s != null && a != null && a > 1) AyahRef(s, a) else null))
                }
                Route.Tasbeeh -> TasbeehScreen(::pop)
                Route.AdhkarHome -> AdhkarHomeScreen(onBack = ::pop, onHisn = { push(Route.Hisn) }, onChapter = { push(Route.Adhkar(it)) })
                Route.Settings -> SettingsScreen(
                    onBack = ::pop,
                    onLocation = { push(Route.Location) },
                    onMuezzin = { push(Route.Muezzin) },
                    onPrayers = { push(Route.Prayers) },
                    onReliability = { push(Route.Reliability) },
                    onReciters = { push(Route.Reciters) },
                    onAbout = { push(Route.About) },
                    onQiyam = { push(Route.Qiyam) },
                )
                Route.Location -> LocationScreen(::pop)
                Route.Muezzin -> MuezzinScreen(::pop)
                Route.Prayers -> PrayersScreen(::pop)
                Route.Reliability -> ReliabilityScreen(::pop)
                Route.Reciters -> RecitersScreen(::pop)
                Route.About -> AboutScreen(::pop)
                is Route.Mushaf -> MushafScreen(route.page, route.focus, route.wird, ::pop)
                Route.Hisn -> HisnScreen(::pop) { push(Route.Adhkar(it)) }
                Route.Qibla -> QiblaScreen(::pop)
                Route.Wird -> WirdScreen(::pop) { push(Route.Mushaf(it.next.coerceAtMost(com.mtgofa.muslim.data.Wird.LAST_PAGE), null, it.id)) }
                Route.Fasting -> FastingScreen(::pop)
                Route.Qiyam -> com.mtgofa.muslim.ui.QiyamScreen(::pop)
                is Route.Adhkar -> AdhkarChapterScreen(route.id, ::pop)
            }
        }
    }
    prayerSheet?.let { PrayerSheet(it) { prayerSheet = null } }

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
