package com.mtgofa.muslem.prayer

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/** The six daily times, in the order they occur. */
enum class Prayer(val ar: String, val hasAdhan: Boolean) {
    FAJR("الفجر", true),
    SUNRISE("الشروق", false),
    DHUHR("الظهر", true),
    ASR("العصر", true),
    MAGHRIB("المغرب", true),
    ISHA("العشاء", true);

    /** Friday's noon prayer has its own name. */
    fun label(date: LocalDate): String =
        if (this == DHUHR && date.dayOfWeek == java.time.DayOfWeek.FRIDAY) "الجمعة" else ar
}

/**
 * A calculation convention: twilight angle for Fajr, and either an angle or a fixed number of
 * minutes after Maghrib for Isha.
 */
enum class CalcMethod(val ar: String, val fajr: Double, val isha: Double, val ishaMinutes: Int = 0) {
    EGYPT("الهيئة المصرية العامة للمساحة", 19.5, 17.5),
    MWL("رابطة العالم الإسلامي", 18.0, 17.0),
    UMM_AL_QURA("أم القرى (مكة المكرمة)", 18.5, 0.0, ishaMinutes = 90),
    KARACHI("جامعة العلوم الإسلامية بكراتشي", 18.0, 18.0),
    ISNA("الجمعية الإسلامية لأمريكا الشمالية", 15.0, 15.0),
    DUBAI("الإمارات (دبي)", 18.2, 18.2),
    KUWAIT("الكويت", 18.0, 17.5),
    QATAR("قطر", 18.0, 0.0, ishaMinutes = 90),
    TURKEY("تركيا (ديانت)", 18.0, 17.0),
    FRANCE("اتحاد المنظمات الإسلامية بفرنسا", 12.0, 12.0),
}

enum class AsrMadhab(val ar: String, val factor: Int) {
    SHAFII("الجمهور (شافعي، مالكي، حنبلي)", 1),
    HANAFI("حنفي", 2),
}

data class DayTimes(val date: LocalDate, val times: Map<Prayer, ZonedDateTime>) {
    operator fun get(p: Prayer): ZonedDateTime = times.getValue(p)
}

/**
 * Astronomical prayer-time calculation (solar position after Jean Meeus, simplified to the
 * accuracy prayer schedules need — well under a minute for inhabited latitudes).
 */
object PrayerCalc {

    fun compute(
        date: LocalDate,
        lat: Double,
        lng: Double,
        zone: ZoneId,
        method: CalcMethod,
        madhab: AsrMadhab,
        offsets: Map<Prayer, Int> = emptyMap(),
        ramadanIshaBonus: Boolean = true,
    ): DayTimes {
        val tz = zone.rules.getOffset(date.atTime(12, 0)).totalSeconds / 3600.0
        val jd = julian(date.year, date.monthValue, date.dayOfMonth) - lng / (15 * 24.0)

        // two refinement passes: each event is re-evaluated at its own approximate hour
        var t = doubleArrayOf(5.0, 6.0, 12.0, 13.0, 18.0, 18.0, 18.0)
        repeat(2) {
            t = doubleArrayOf(
                sunAngleTime(jd, t[0] / 24, method.fajr, lat, ccw = true),
                sunAngleTime(jd, t[1] / 24, RISE_SET, lat, ccw = true),
                midDay(jd, t[2] / 24),
                asrTime(jd, t[3] / 24, madhab.factor.toDouble(), lat),
                sunAngleTime(jd, t[4] / 24, RISE_SET, lat, ccw = false),
                sunAngleTime(jd, t[5] / 24, RISE_SET, lat, ccw = false),
                if (method.ishaMinutes == 0) sunAngleTime(jd, t[6] / 24, method.isha, lat, ccw = false) else 0.0,
            )
        }
        val adj = t.map { it + tz - lng / 15 }.toDoubleArray()
        val sunrise = adj[1]; val sunset = adj[4]
        var fajr = adj[0]; var isha = adj[6]

        // high latitudes / polar nights: fall back to the "angle-based" portion of the night
        val night = 24 - (sunset - sunrise)
        val fajrMax = method.fajr / 60.0 * night
        if (fajr.isNaN() || sunrise - fajr > fajrMax) fajr = sunrise - fajrMax
        if (method.ishaMinutes > 0) {
            val ramadan = ramadanIshaBonus && method == CalcMethod.UMM_AL_QURA && Hijri.of(date).month == 9
            isha = sunset + (if (ramadan) 120 else method.ishaMinutes) / 60.0
        } else {
            val ishaMax = method.isha / 60.0 * night
            if (isha.isNaN() || isha - sunset > ishaMax) isha = sunset + ishaMax
        }
        // Dhuhr is taken a minute after true noon (zawal) — the common published convention
        val hours = doubleArrayOf(fajr, sunrise, adj[2] + 1 / 60.0, adj[3], adj[5], isha)

        val base = date.atStartOfDay(zone)
        val out = LinkedHashMap<Prayer, ZonedDateTime>()
        Prayer.entries.forEachIndexed { i, p ->
            val minutes = Math.round(hours[i] * 60).toLong() + (offsets[p] ?: 0)
            out[p] = base.plusMinutes(minutes)
        }
        return DayTimes(date, out)
    }

    private const val RISE_SET = 0.833

    // ---- astronomy ------------------------------------------------------------------------

    private fun julian(y0: Int, m0: Int, d: Int): Double {
        var y = y0; var m = m0
        if (m <= 2) { y -= 1; m += 12 }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + d + b - 1524.5
    }

    /** Sun declination (deg) and equation of time (hours) for a julian day. */
    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = darctan2(dcos(e) * dsin(l), dcos(l)) / 15
        val eqt = q / 15 - fixHour(ra)
        val decl = darcsin(dsin(e) * dsin(l))
        return decl to eqt
    }

    private fun midDay(jd: Double, t: Double): Double {
        val eqt = sunPosition(jd + t).second
        return fixHour(12 - eqt)
    }

    private fun sunAngleTime(jd: Double, t: Double, angle: Double, lat: Double, ccw: Boolean): Double {
        val decl = sunPosition(jd + t).first
        val noon = midDay(jd, t)
        val c = (-dsin(angle) - dsin(decl) * dsin(lat)) / (dcos(decl) * dcos(lat))
        if (abs(c) > 1) return Double.NaN
        val h = darccos(c) / 15
        return noon + if (ccw) -h else h
    }

    private fun asrTime(jd: Double, t: Double, factor: Double, lat: Double): Double {
        val decl = sunPosition(jd + t).first
        val angle = -darccot(factor + dtan(abs(lat - decl)))
        return sunAngleTime(jd, t, angle, lat, ccw = false)
    }

    private fun dsin(d: Double) = sin(Math.toRadians(d))
    private fun dcos(d: Double) = cos(Math.toRadians(d))
    private fun dtan(d: Double) = tan(Math.toRadians(d))
    private fun darcsin(x: Double) = Math.toDegrees(asin(x))
    private fun darccos(x: Double) = Math.toDegrees(acos(x))
    private fun darctan2(y: Double, x: Double) = Math.toDegrees(atan2(y, x))
    private fun darccot(x: Double) = Math.toDegrees(atan(1 / x))
    private fun fixAngle(a: Double) = a - 360.0 * floor(a / 360.0)
    private fun fixHour(a: Double) = a - 24.0 * floor(a / 24.0)
}
