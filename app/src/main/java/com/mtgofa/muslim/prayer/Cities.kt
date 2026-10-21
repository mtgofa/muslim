package com.mtgofa.muslim.prayer

data class City(val name: String, val country: String, val lat: Double, val lng: Double, val zone: String, val method: CalcMethod)

/** Built-in places for choosing a location by region instead of GPS. */
object Cities {
    private val EG = CalcMethod.EGYPT
    private val UQ = CalcMethod.UMM_AL_QURA
    private const val CAIRO = "Africa/Cairo"

    val all: List<City> = listOf(
        // ---- مصر
        City("القاهرة", "مصر", 30.0444, 31.2357, CAIRO, EG),
        City("الجيزة", "مصر", 30.0131, 31.2089, CAIRO, EG),
        City("الإسكندرية", "مصر", 31.2001, 29.9187, CAIRO, EG),
        City("القليوبية (بنها)", "مصر", 30.4660, 31.1848, CAIRO, EG),
        City("شبرا الخيمة", "مصر", 30.1286, 31.2422, CAIRO, EG),
        City("المنوفية (شبين الكوم)", "مصر", 30.5527, 31.0094, CAIRO, EG),
        City("الغربية (طنطا)", "مصر", 30.7865, 31.0004, CAIRO, EG),
        City("المحلة الكبرى", "مصر", 30.9697, 31.1663, CAIRO, EG),
        City("الدقهلية (المنصورة)", "مصر", 31.0409, 31.3785, CAIRO, EG),
        City("الشرقية (الزقازيق)", "مصر", 30.5877, 31.5020, CAIRO, EG),
        City("كفر الشيخ", "مصر", 31.1107, 30.9388, CAIRO, EG),
        City("البحيرة (دمنهور)", "مصر", 31.0341, 30.4682, CAIRO, EG),
        City("دمياط", "مصر", 31.4165, 31.8133, CAIRO, EG),
        City("بورسعيد", "مصر", 31.2653, 32.3019, CAIRO, EG),
        City("الإسماعيلية", "مصر", 30.5965, 32.2715, CAIRO, EG),
        City("السويس", "مصر", 29.9668, 32.5498, CAIRO, EG),
        City("الفيوم", "مصر", 29.3084, 30.8428, CAIRO, EG),
        City("بني سويف", "مصر", 29.0661, 31.0994, CAIRO, EG),
        City("المنيا", "مصر", 28.0871, 30.7618, CAIRO, EG),
        City("أسيوط", "مصر", 27.1783, 31.1859, CAIRO, EG),
        City("سوهاج", "مصر", 26.5591, 31.6957, CAIRO, EG),
        City("قنا", "مصر", 26.1551, 32.7160, CAIRO, EG),
        City("الأقصر", "مصر", 25.6872, 32.6396, CAIRO, EG),
        City("أسوان", "مصر", 24.0889, 32.8998, CAIRO, EG),
        City("الغردقة (البحر الأحمر)", "مصر", 27.2579, 33.8116, CAIRO, EG),
        City("الوادي الجديد (الخارجة)", "مصر", 25.4390, 30.5586, CAIRO, EG),
        City("مرسى مطروح", "مصر", 31.3543, 27.2373, CAIRO, EG),
        City("العريش (شمال سيناء)", "مصر", 31.1316, 33.7984, CAIRO, EG),
        City("الطور (جنوب سيناء)", "مصر", 28.2366, 33.6254, CAIRO, EG),
        City("شرم الشيخ", "مصر", 27.9158, 34.3299, CAIRO, EG),
        City("السادس من أكتوبر", "مصر", 29.9285, 30.9188, CAIRO, EG),
        City("العاشر من رمضان", "مصر", 30.2965, 31.7426, CAIRO, EG),
        // ---- السعودية والخليج
        City("مكة المكرمة", "السعودية", 21.4225, 39.8262, "Asia/Riyadh", UQ),
        City("المدينة المنورة", "السعودية", 24.4672, 39.6111, "Asia/Riyadh", UQ),
        City("الرياض", "السعودية", 24.7136, 46.6753, "Asia/Riyadh", UQ),
        City("جدة", "السعودية", 21.4858, 39.1925, "Asia/Riyadh", UQ),
        City("الدمام", "السعودية", 26.4207, 50.0888, "Asia/Riyadh", UQ),
        City("الطائف", "السعودية", 21.2703, 40.4158, "Asia/Riyadh", UQ),
        City("تبوك", "السعودية", 28.3838, 36.5550, "Asia/Riyadh", UQ),
        City("أبها", "السعودية", 18.2164, 42.5053, "Asia/Riyadh", UQ),
        City("دبي", "الإمارات", 25.2048, 55.2708, "Asia/Dubai", CalcMethod.DUBAI),
        City("أبوظبي", "الإمارات", 24.4539, 54.3773, "Asia/Dubai", CalcMethod.DUBAI),
        City("الشارقة", "الإمارات", 25.3463, 55.4209, "Asia/Dubai", CalcMethod.DUBAI),
        City("الكويت", "الكويت", 29.3759, 47.9774, "Asia/Kuwait", CalcMethod.KUWAIT),
        City("الدوحة", "قطر", 25.2854, 51.5310, "Asia/Qatar", CalcMethod.QATAR),
        City("المنامة", "البحرين", 26.2285, 50.5860, "Asia/Bahrain", UQ),
        City("مسقط", "عُمان", 23.5880, 58.3829, "Asia/Muscat", CalcMethod.MWL),
        // ---- الشام والعراق
        City("عمّان", "الأردن", 31.9454, 35.9284, "Asia/Amman", CalcMethod.MWL),
        City("القدس", "فلسطين", 31.7683, 35.2137, "Asia/Jerusalem", CalcMethod.MWL),
        City("غزة", "فلسطين", 31.5017, 34.4668, "Asia/Gaza", CalcMethod.EGYPT),
        City("دمشق", "سوريا", 33.5138, 36.2765, "Asia/Damascus", CalcMethod.MWL),
        City("حلب", "سوريا", 36.2021, 37.1343, "Asia/Damascus", CalcMethod.MWL),
        City("بيروت", "لبنان", 33.8938, 35.5018, "Asia/Beirut", CalcMethod.MWL),
        City("بغداد", "العراق", 33.3152, 44.3661, "Asia/Baghdad", CalcMethod.MWL),
        City("البصرة", "العراق", 30.5085, 47.7804, "Asia/Baghdad", CalcMethod.MWL),
        // ---- شمال أفريقيا
        City("الخرطوم", "السودان", 15.5007, 32.5599, "Africa/Khartoum", CalcMethod.EGYPT),
        City("طرابلس", "ليبيا", 32.8872, 13.1913, "Africa/Tripoli", CalcMethod.EGYPT),
        City("بنغازي", "ليبيا", 32.1167, 20.0667, "Africa/Tripoli", CalcMethod.EGYPT),
        City("تونس", "تونس", 36.8065, 10.1815, "Africa/Tunis", CalcMethod.MWL),
        City("الجزائر", "الجزائر", 36.7538, 3.0588, "Africa/Algiers", CalcMethod.MWL),
        City("الدار البيضاء", "المغرب", 33.5731, -7.5898, "Africa/Casablanca", CalcMethod.MWL),
        City("الرباط", "المغرب", 34.0209, -6.8416, "Africa/Casablanca", CalcMethod.MWL),
        City("نواكشوط", "موريتانيا", 18.0735, -15.9582, "Africa/Nouakchott", CalcMethod.MWL),
        City("صنعاء", "اليمن", 15.3694, 44.1910, "Asia/Aden", CalcMethod.MWL),
        City("عدن", "اليمن", 12.7855, 45.0187, "Asia/Aden", CalcMethod.MWL),
        City("مقديشو", "الصومال", 2.0469, 45.3182, "Africa/Mogadishu", CalcMethod.MWL),
        // ---- العالم
        City("إسطنبول", "تركيا", 41.0082, 28.9784, "Europe/Istanbul", CalcMethod.TURKEY),
        City("أنقرة", "تركيا", 39.9334, 32.8597, "Europe/Istanbul", CalcMethod.TURKEY),
        City("كراتشي", "باكستان", 24.8607, 67.0011, "Asia/Karachi", CalcMethod.KARACHI),
        City("جاكرتا", "إندونيسيا", -6.2088, 106.8456, "Asia/Jakarta", CalcMethod.MWL),
        City("كوالالمبور", "ماليزيا", 3.1390, 101.6869, "Asia/Kuala_Lumpur", CalcMethod.MWL),
        City("لندن", "المملكة المتحدة", 51.5074, -0.1278, "Europe/London", CalcMethod.MWL),
        City("باريس", "فرنسا", 48.8566, 2.3522, "Europe/Paris", CalcMethod.FRANCE),
        City("برلين", "ألمانيا", 52.5200, 13.4050, "Europe/Berlin", CalcMethod.MWL),
        City("نيويورك", "الولايات المتحدة", 40.7128, -74.0060, "America/New_York", CalcMethod.ISNA),
        City("تورونتو", "كندا", 43.6532, -79.3832, "America/Toronto", CalcMethod.ISNA),
    )
}
