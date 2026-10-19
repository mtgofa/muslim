package com.mtgofa.muslem.data

import android.content.Context
import android.content.SharedPreferences
import com.mtgofa.muslem.prayer.AsrMadhab
import com.mtgofa.muslem.prayer.CalcMethod
import com.mtgofa.muslem.prayer.Hijri
import com.mtgofa.muslem.prayer.Prayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.ZoneId

/** How a prayer time announces itself. */
enum class AlertMode(val ar: String) { ADHAN("أذان كامل"), NOTIFY("تنبيه «سبحان الله»"), SILENT("بدون صوت") }

/** All user settings, persisted in SharedPreferences. [changes] ticks on every write so UI recomposes. */
object Settings {
    private lateinit var sp: SharedPreferences
    private val _changes = MutableStateFlow(0)
    val changes: StateFlow<Int> = _changes

    fun init(ctx: Context) {
        if (::sp.isInitialized) return
        // device-protected so alarms can be re-armed and the adhan played before the first unlock
        val dp = Storage.dp(ctx)
        if (Storage.unlocked(ctx)) dp.moveSharedPreferencesFrom(ctx.applicationContext, "settings")
        sp = dp.getSharedPreferences("settings", Context.MODE_PRIVATE)
        Hijri.adjustDays = hijriAdjust
    }

    private fun edit(block: SharedPreferences.Editor.() -> Unit) {
        sp.edit().apply(block).apply()
        _changes.value++
    }

    // ---- location ------------------------------------------------------------------------
    val hasLocation get() = sp.contains("lat")
    var locationName: String
        get() = sp.getString("locName", "القاهرة")!!
        set(v) = edit { putString("locName", v) }
    val lat get() = sp.getFloat("lat", 30.0444f).toDouble()
    val lng get() = sp.getFloat("lng", 31.2357f).toDouble()
    /** "gps" or "city" */
    val locationSource get() = sp.getString("locSrc", "city")!!
    val zone: ZoneId
        get() = runCatching { ZoneId.of(sp.getString("zone", null) ?: ZoneId.systemDefault().id) }
            .getOrDefault(ZoneId.systemDefault())

    fun setLocation(name: String, lat: Double, lng: Double, zone: String?, source: String) = edit {
        putString("locName", name); putFloat("lat", lat.toFloat()); putFloat("lng", lng.toFloat())
        putString("locSrc", source)
        if (zone == null) remove("zone") else putString("zone", zone)
    }

    // ---- calculation ---------------------------------------------------------------------
    var method: CalcMethod
        get() = runCatching { CalcMethod.valueOf(sp.getString("method", "EGYPT")!!) }.getOrDefault(CalcMethod.EGYPT)
        set(v) = edit { putString("method", v.name) }
    var madhab: AsrMadhab
        get() = runCatching { AsrMadhab.valueOf(sp.getString("madhab", "SHAFII")!!) }.getOrDefault(AsrMadhab.SHAFII)
        set(v) = edit { putString("madhab", v.name) }
    var hijriAdjust: Int
        get() = sp.getInt("hijriAdj", 0)
        set(v) { Hijri.adjustDays = v; edit { putInt("hijriAdj", v) } }

    // ---- per prayer ----------------------------------------------------------------------
    fun offset(p: Prayer) = sp.getInt("off_${p.name}", 0)
    fun setOffset(p: Prayer, v: Int) = edit { putInt("off_${p.name}", v) }
    fun offsets(): Map<Prayer, Int> = Prayer.entries.associateWith { offset(it) }

    fun mode(p: Prayer): AlertMode = runCatching {
        AlertMode.valueOf(sp.getString("mode_${p.name}", if (p.hasAdhan) "ADHAN" else "SILENT")!!)
    }.getOrDefault(AlertMode.ADHAN)
    fun setMode(p: Prayer, v: AlertMode) = edit { putString("mode_${p.name}", v.name) }

    /** minutes before the adhan for a "سبحان الله" reminder; 0 = off */
    fun preAlert(p: Prayer) = sp.getInt("pre_${p.name}", if (p.hasAdhan) 10 else 0)
    fun setPreAlert(p: Prayer, v: Int) = edit { putInt("pre_${p.name}", v) }

    /** minutes after the adhan for the iqama alert; 0 = off */
    fun iqama(p: Prayer) = sp.getInt("iqama_${p.name}", DEFAULT_IQAMA[p] ?: 0)
    fun setIqama(p: Prayer, v: Int) = edit { putInt("iqama_${p.name}", v) }

    private val DEFAULT_IQAMA = mapOf(
        Prayer.FAJR to 20, Prayer.DHUHR to 15, Prayer.ASR to 15, Prayer.MAGHRIB to 10, Prayer.ISHA to 15,
    )

    // ---- adhan sound ---------------------------------------------------------------------
    var muezzin: String
        get() = sp.getString("muezzin", "abdulbasit")!!
        set(v) = edit { putString("muezzin", v) }
    /** separate voice for Fajr ("" = same as the others) */
    var fajrMuezzin: String
        // default: a Fajr recording, which includes "الصلاة خير من النوم"
        get() = sp.getString("fajrMuezzin", DEFAULT_FAJR)!!
        set(v) = edit { putString("fajrMuezzin", v) }
    /** The voice that calls this prayer (Fajr may have its own). */
    const val DEFAULT_FAJR = "fajr_chebae"

    fun muezzinFor(p: Prayer) = if (p == Prayer.FAJR && fajrMuezzin.isNotEmpty()) fajrMuezzin else muezzin

    /** play the adhan and reminders even when the phone is silent (on the alarm volume) */
    /** the adhan and all prayer alerts vibrate in their own patterns instead of sounding */
    var vibrateOnly: Boolean
        get() = sp.getBoolean("vibrateOnly", false)
        set(v) = edit { putBoolean("vibrateOnly", v) }
    // morning / evening adhkar reminders: minutes after midnight, and vibration instead of a sound
    var adhkarMorningOn: Boolean
        get() = sp.getBoolean("adhkarMorningOn", true)
        set(v) = edit { putBoolean("adhkarMorningOn", v) }
    var adhkarMorningAt: Int
        get() = sp.getInt("adhkarMorningAt", 6 * 60 + 30)
        set(v) = edit { putInt("adhkarMorningAt", v) }
    var adhkarEveningOn: Boolean
        get() = sp.getBoolean("adhkarEveningOn", true)
        set(v) = edit { putBoolean("adhkarEveningOn", v) }
    var adhkarEveningAt: Int
        get() = sp.getInt("adhkarEveningAt", 16 * 60 + 30)
        set(v) = edit { putInt("adhkarEveningAt", v) }
    var adhkarVibrate: Boolean
        get() = sp.getBoolean("adhkarVibrate", false)
        set(v) = edit { putBoolean("adhkarVibrate", v) }
    var adhanInSilent: Boolean
        get() = sp.getBoolean("adhanInSilent", false)
        set(v) = edit { putBoolean("adhanInSilent", v) }
    /** a persistent notification that keeps the app alive and shows the next prayer */
    var keepAlive: Boolean
        get() = sp.getBoolean("keepAlive", true)
        set(v) = edit { putBoolean("keepAlive", v) }
    /** pause the adhan while the phone is in a call */
    var muteInCall: Boolean
        get() = sp.getBoolean("muteInCall", true)
        set(v) = edit { putBoolean("muteInCall", v) }

    // ---- quran ---------------------------------------------------------------------------
    var lastPage: Int
        get() = sp.getInt("lastPage", 1)
        set(v) { sp.edit().putInt("lastPage", v).apply() }
    var reciter: String
        get() = sp.getString("reciter", "Abdul_Basit_Murattal_64kbps")!!
        set(v) = edit { putString("reciter", v) }
    var tajweed: Boolean
        get() = sp.getBoolean("tajweed", true)
        set(v) = edit { putBoolean("tajweed", v) }
    var ayahRepeat: Int
        get() = sp.getInt("ayahRepeat", 1)
        set(v) = edit { putInt("ayahRepeat", v) }
    /** 0 = no range repeat, -1 = forever */
    var rangeRepeat: Int
        get() = sp.getInt("rangeRepeat", 1)
        set(v) = edit { putInt("rangeRepeat", v) }
    var playSpeed: Float
        get() = sp.getFloat("playSpeed", 1f)
        set(v) = edit { putFloat("playSpeed", v) }
    var keepScreenOn: Boolean
        get() = sp.getBoolean("keepScreenOn", true)
        set(v) = edit { putBoolean("keepScreenOn", v) }

    fun bookmarks(): List<Int> = sp.getString("bookmarks", "")!!.split(',').mapNotNull { it.toIntOrNull() }
    fun toggleBookmark(page: Int) {
        val b = bookmarks().toMutableList()
        if (!b.remove(page)) b.add(0, page)
        edit { putString("bookmarks", b.joinToString(",")) }
    }

    /** saved ayah bookmarks as "sura:ayah" */
    fun ayahMarks(): List<Pair<Int, Int>> = sp.getString("ayahMarks", "")!!.split(',').mapNotNull {
        val p = it.split(':'); if (p.size == 2) (p[0].toIntOrNull() ?: return@mapNotNull null) to (p[1].toIntOrNull() ?: return@mapNotNull null) else null
    }
    fun toggleAyahMark(s: Int, a: Int) {
        val m = ayahMarks().toMutableList()
        if (!m.remove(s to a)) m.add(0, s to a)
        edit { putString("ayahMarks", m.joinToString(",") { "${it.first}:${it.second}" }) }
    }

    // ---- tasbeeh -------------------------------------------------------------------------
    var tasbeehPhrase: String
        get() = sp.getString("tsb_phrase", "subhan")!!
        set(v) = edit { putString("tsb_phrase", v) }
    fun tasbeehCount(id: String) = sp.getInt("tsb_count_$id", 0)
    /** counts are written without bumping [changes]: the counter screen holds its own state */
    fun setTasbeehCount(id: String, v: Int) { sp.edit().putInt("tsb_count_$id", v).apply() }
    var tasbeehTotal: Long
        get() = sp.getLong("tsb_total", 0)
        set(v) { sp.edit().putLong("tsb_total", v).apply() }
    var tasbeehVibrate: Boolean
        get() = sp.getBoolean("tsb_vibrate", true)
        set(v) = edit { putBoolean("tsb_vibrate", v) }
    /** 1 = big button (default), 0 = bead ring */
    var tasbeehStyle: Int
        get() = sp.getInt("tsb_style", 1)
        set(v) = edit { putInt("tsb_style", v) }
    /** 0 = no sound, 1 = at the end of each round, 2 = every 100 */
    var tasbeehSound: Int
        get() = sp.getInt("tsb_sound", 1)
        set(v) = edit { putInt("tsb_sound", v) }

    // ---- misc ----------------------------------------------------------------------------
    var onboarded: Boolean
        get() = sp.getBoolean("onboarded", false)
        set(v) = edit { putBoolean("onboarded", v) }
}
