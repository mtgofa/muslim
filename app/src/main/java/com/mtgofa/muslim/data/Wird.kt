package com.mtgofa.muslim.data

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import com.mtgofa.muslim.MainActivity
import com.mtgofa.muslim.MuslimApp
import com.mtgofa.muslim.R
import com.mtgofa.muslim.prayer.arabicDigits
import com.mtgofa.muslim.quran.QuranDb
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** What a daily portion is measured in. */
enum class WirdUnit(val singular: String, val dual: String, val plural: String, val accusative: String) {
    PAGE("وجه", "وجهان", "أوجه", "وجهاً"),
    QUARTER("ربع", "ربعان", "أرباع", "ربعاً"),
    HIZB("حزب", "حزبان", "أحزاب", "حزباً"),
    JUZ("جزء", "جزءان", "أجزاء", "جزءاً");

    /** grammatical count: "حزب واحد", "حزبان", "٣ أحزاب", "١١ حزباً" */
    fun count(n: Int): String = when {
        n == 1 -> "$singular واحد"
        n == 2 -> dual
        n in 3..10 -> "${arabicDigits(n)} $plural"
        else -> "${arabicDigits(n)} $accusative"
    }

    /** the number first, then the unit name: "٢ حزب" */
    fun short(n: Int) = "${arabicDigits(n)} $singular"

    /** first Mushaf page of each unit, in order */
    fun starts(): IntArray = when (this) {
        PAGE -> IntArray(604) { it + 1 }
        QUARTER -> QuranDb.quarters.map { it.page }.toIntArray()
        HIZB -> QuranDb.quarters.filterIndexed { i, _ -> i % 4 == 0 }.map { it.page }.toIntArray()
        JUZ -> QuranDb.juz.map { it.page }.toIntArray()
    }
}

/**
 * One khatma: read [amount] [unit]s a day from [startPage]. [next] is the next page to read; it
 * moves on by itself when the reader turns forward past it in the Mushaf. Progress follows a
 * timeline from [baseDay]: [baseUnits] units read before then plus [amount] for each day since.
 */
data class Khatma(
    val id: Long,
    val unit: WirdUnit,
    val amount: Int,
    val startPage: Int,
    val next: Int,
    val remind: Int,          // minutes after midnight, -1 = none
    val startDay: Long,
    val baseDay: Long,
    val baseUnits: Int,
    val todayDay: Long,
    val todayPages: Int,
) {
    private val starts by lazy { unit.starts() }
    private fun unitAt(page: Int): Int = (starts.indexOfLast { it <= page }).coerceAtLeast(0)
    private val startUnit get() = unitAt(startPage)

    val complete get() = next > Wird.LAST_PAGE
    val totalUnits get() = starts.size - startUnit
    /** whole units already read */
    val readUnits get() = if (complete) totalUnits else (unitAt(next) - startUnit).coerceAtLeast(0)
    val progress get() = (next - startPage).toFloat() / (Wird.LAST_PAGE - startPage + 1)
    val reminder: LocalTime? get() = remind.takeIf { it >= 0 }?.let { LocalTime.of(it / 60, it % 60) }
    val pagesToday get() = if (todayDay == LocalDate.now().toEpochDay()) todayPages else 0

    private fun expectedBy(day: Long): Int =
        (baseUnits + ((day - baseDay).coerceAtLeast(-1) + 1) * amount).toInt().coerceIn(0, totalUnits)
    private val today get() = LocalDate.now().toEpochDay()

    /** first page of the unit the plan expects to reach by the end of today */
    private val targetPage: Int
        get() {
            val idx = startUnit + expectedBy(today)
            return if (idx >= starts.size) Wird.LAST_PAGE + 1 else starts[idx]
        }

    /** pages still to read today */
    val leftPages get() = if (complete) 0 else (targetPage - next).coerceAtLeast(0)
    /** units behind the plan, not counting today's portion */
    val behind get() = (expectedBy(today - 1) - readUnits).coerceAtLeast(0)
    /** units read beyond today's portion */
    val ahead get() = (readUnits - expectedBy(today)).coerceAtLeast(0)

    fun status(): String = when {
        complete -> "أتممت الختمة"
        behind > 0 -> "متأخر بـ ${unit.short(behind)}"
        leftPages > 0 -> "باقي ${WirdUnit.PAGE.count(leftPages)}"
        ahead > 0 -> "متقدم بـ ${unit.short(ahead)}"
        else -> "أتممت ورد اليوم ✓"
    }

    val onTrack get() = !complete && behind == 0 && leftPages == 0

    /** When the khatma should end at the planned pace, counting today's remaining portion. */
    fun expectedEnd(): LocalDate {
        if (complete) return LocalDate.now()
        val remaining = totalUnits - expectedBy(today).coerceAtLeast(readUnits)
        return LocalDate.now().plusDays(((remaining + amount - 1) / amount).toLong())
    }

    val title: String
        get() {
            val s = QuranDb.page(startPage).suras.firstOrNull()?.let { QuranDb.surah(it).name } ?: ""
            return "${unit.count(amount)} يومياً · من ${if (startPage == 1) "أول المصحف" else "سورة $s"}"
        }

    fun toJson() = JSONObject().apply {
        put("id", id); put("unit", unit.name); put("amount", amount); put("start", startPage); put("next", next)
        put("remind", remind); put("startDay", startDay); put("baseDay", baseDay); put("baseUnits", baseUnits)
        put("todayDay", todayDay); put("todayPages", todayPages)
    }

    companion object {
        fun from(o: JSONObject) = Khatma(
            o.getLong("id"), runCatching { WirdUnit.valueOf(o.getString("unit")) }.getOrDefault(WirdUnit.PAGE),
            o.getInt("amount"), o.getInt("start"), o.getInt("next"), o.optInt("remind", -1),
            o.getLong("startDay"), o.optLong("baseDay", o.getLong("startDay")), o.optInt("baseUnits", 0),
            o.optLong("todayDay", -1), o.optInt("todayPages", 0),
        )

        /** Days and end date for a khatma of [units] units read [amount] a day from today. */
        fun estimate(units: Int, amount: Int): Pair<Int, LocalDate> {
            val days = ((units + amount - 1) / amount).coerceAtLeast(1)
            return days to LocalDate.now().plusDays(days - 1L)
        }
    }
}

/** All khatmas, persisted as JSON. */
object Wird {
    const val LAST_PAGE = 604
    private lateinit var sp: SharedPreferences
    private val _changes = MutableStateFlow(0)
    val changes: StateFlow<Int> = _changes
    @Volatile private var cache: List<Khatma>? = null

    fun init(ctx: Context) {
        if (::sp.isInitialized) return
        sp = ctx.applicationContext.getSharedPreferences("wird", Context.MODE_PRIVATE)
        migrate()
    }

    /** The first version kept a single page-based wird in flat keys. */
    private fun migrate() {
        if (!sp.getBoolean("active", false) || sp.contains("khatmas")) return
        val day = sp.getLong("startDay", LocalDate.now().toEpochDay())
        val k = Khatma(
            System.currentTimeMillis(), WirdUnit.PAGE, sp.getInt("ppd", 5), sp.getInt("start", 1), sp.getInt("next", 1),
            sp.getInt("remind", -1), day, sp.getLong("baseDay", day), sp.getInt("baseRead", 0),
            sp.getLong("todayDay", -1), sp.getInt("todayCount", 0),
        )
        save(listOf(k))
        sp.edit().putBoolean("active", false).apply()
    }

    val all: List<Khatma>
        get() = cache ?: run {
            val arr = runCatching { JSONArray(sp.getString("khatmas", "[]")) }.getOrDefault(JSONArray())
            List(arr.length()) { Khatma.from(arr.getJSONObject(it)) }
        }.also { cache = it }

    fun byId(id: Long) = all.firstOrNull { it.id == id }

    /** The khatmas still being read. */
    val active get() = all.filter { !it.complete }

    private fun save(list: List<Khatma>) {
        sp.edit().putString("khatmas", JSONArray().apply { list.forEach { put(it.toJson()) } }.toString()).apply()
        cache = list
        _changes.value++
    }

    private fun replace(k: Khatma) = save(all.map { if (it.id == k.id) k else it })

    fun add(ctx: Context, unit: WirdUnit, amount: Int, startPage: Int, remindAt: LocalTime?): Khatma {
        val today = LocalDate.now().toEpochDay()
        val k = Khatma(
            System.currentTimeMillis(), unit, amount, startPage, startPage, remindAt?.let { it.hour * 60 + it.minute } ?: -1,
            today, today, 0, -1, 0,
        )
        save(all + k)
        schedule(ctx)
        return k
    }

    /** New pace or reminder; the plan's timeline restarts today from what was read before it. */
    fun update(ctx: Context, id: Long, unit: WirdUnit, amount: Int, remindAt: LocalTime?) {
        val k = byId(id) ?: return
        val today = LocalDate.now().toEpochDay()
        val draft = k.copy(unit = unit, amount = amount)
        // units read before today, in the new unit
        val before = draft.copy(next = (k.next - k.pagesToday).coerceAtLeast(k.startPage)).readUnits
        replace(draft.copy(remind = remindAt?.let { it.hour * 60 + it.minute } ?: -1, baseDay = today, baseUnits = before))
        schedule(ctx)
    }

    fun remove(ctx: Context, id: Long) { save(all.filter { it.id != id }); schedule(ctx) }

    /** The reader turned from [from] to [to]; any khatma waiting on [from] moves on. */
    fun onPageTurn(ctx: Context, id: Long, from: Int, to: Int) {
        if (to != from + 1) return
        byId(id)?.takeIf { !it.complete && it.next == from }?.let { markRead(ctx, it.id) }
    }

    /** Record a khatma's next page as read (also for the last page, which has nothing after it). */
    fun markRead(ctx: Context, id: Long) {
        val k = byId(id)?.takeIf { !it.complete } ?: return
        val today = LocalDate.now().toEpochDay()
        val updated = k.copy(next = k.next + 1, todayDay = today, todayPages = k.pagesToday + 1)
        replace(updated)
        if (updated.leftPages == 0) ctx.getSystemService(NotificationManager::class.java).cancel(noteId(id))
    }

    // ---- daily reminders ----------------------------------------------------------------------

    private fun noteId(id: Long) = 3000 + (id % 1000).toInt()

    /** Arm one alarm for the earliest upcoming reminder of any khatma. */
    fun schedule(ctx: Context) {
        init(ctx)
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            ctx, 31, Intent(ctx, WirdReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        am.cancel(pi)
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        val nextAt = active.mapNotNull { it.reminder }.map { t ->
            var at = ZonedDateTime.of(now.toLocalDate(), t, zone)
            if (!at.isAfter(now)) at = at.plusDays(1)
            at
        }.minOrNull() ?: return
        val millis = nextAt.toInstant().toEpochMilli()
        val exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
    }

    internal fun remind(ctx: Context) {
        init(ctx)
        val minute = LocalTime.now().let { it.hour * 60 + it.minute }
        // every khatma whose reminder is due now (within a few minutes, alarms can be late)
        active.filter { it.remind >= 0 && (minute - it.remind + 1440) % 1440 <= 5 && (it.leftPages > 0 || it.behind > 0) }.forEach { k ->
            val open = PendingIntent.getActivity(
                ctx, noteId(k.id),
                Intent(ctx, MainActivity::class.java).putExtra("openMushaf", true).putExtra("page", k.next.coerceAtMost(LAST_PAGE)).putExtra("wird", k.id)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val n = NotificationCompat.Builder(ctx, MuslimApp.CH_WIRD)
                .setSmallIcon(R.drawable.ic_stat_mosque)
                .setContentTitle("حان وقت وردك اليومي")
                .setContentText("${k.status()} · من صفحة ${arabicDigits(k.next)}")
                .setColor(0xFF0F7A62.toInt())
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            ctx.getSystemService(NotificationManager::class.java).notify(noteId(k.id), n)
        }
        schedule(ctx)
    }
}

class WirdReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) = Wird.remind(ctx)
}
