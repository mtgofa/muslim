package com.mtgofa.muslim.prayer

import android.icu.util.IslamicCalendar
import android.icu.util.ULocale
import java.time.LocalDate
import java.time.ZoneId

data class HijriDate(val day: Int, val month: Int, val year: Int) {
    val monthName: String get() = MONTHS[month - 1]
    override fun toString() = "${arabicDigits(day)} $monthName ${arabicDigits(year)} هـ"

    companion object {
        val MONTHS = listOf(
            "محرّم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
            "رجب", "شعبان", "رمضان", "شوّال", "ذو القعدة", "ذو الحجة",
        )
    }
}

object Hijri {
    fun of(date: LocalDate): HijriDate {
        val cal = IslamicCalendar(ULocale("ar@calendar=islamic-umalqura"))
        cal.calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
        val millis = date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        cal.timeInMillis = millis
        return HijriDate(
            cal.get(IslamicCalendar.DAY_OF_MONTH),
            cal.get(IslamicCalendar.MONTH) + 1,
            cal.get(IslamicCalendar.EXTENDED_YEAR),
        )
    }
}

private const val AR_DIGITS = "٠١٢٣٤٥٦٧٨٩"

fun arabicDigits(n: Int): String = arabicDigits(n.toString())
fun arabicDigits(s: String): String = buildString { s.forEach { append(if (it in '0'..'9') AR_DIGITS[it - '0'] else it) } }
