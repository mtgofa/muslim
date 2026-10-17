package com.mtgofa.muslem.quran

import android.content.Context
import android.net.Uri
import com.mtgofa.muslem.data.Http
import java.io.File

/** A reciter with one audio file per ayah, so ranges and repeats are exact. */
data class Reciter(val id: String, val name: String, val style: String = "مرتّل")

object Reciters {
    private const val BASE = "https://everyayah.com/data/"

    val all = listOf(
        Reciter("Abdul_Basit_Murattal_64kbps", "عبد الباسط عبد الصمد"),
        Reciter("Abdul_Basit_Mujawwad_128kbps", "عبد الباسط عبد الصمد", "مجوّد"),
        Reciter("Minshawy_Murattal_128kbps", "محمد صديق المنشاوي"),
        Reciter("Minshawy_Mujawwad_64kbps", "محمد صديق المنشاوي", "مجوّد"),
        Reciter("Husary_128kbps", "محمود خليل الحصري"),
        Reciter("Husary_Mujawwad_64kbps", "محمود خليل الحصري", "مجوّد"),
        Reciter("Husary_Muallim_128kbps", "محمود خليل الحصري", "المصحف المعلّم"),
        Reciter("Mustafa_Ismail_48kbps", "مصطفى إسماعيل", "مجوّد"),
        Reciter("Mohammad_al_Tablaway_128kbps", "محمد محمود الطبلاوي"),
        Reciter("mahmoud_ali_al_banna_32kbps", "محمود علي البنا"),
        Reciter("Ahmed_Neana_128kbps", "أحمد نعينع"),
        Reciter("Alafasy_128kbps", "مشاري راشد العفاسي"),
        Reciter("Abdurrahmaan_As-Sudais_64kbps", "عبد الرحمن السديس"),
        Reciter("Saood_ash-Shuraym_64kbps", "سعود الشريم"),
        Reciter("MaherAlMuaiqly128kbps", "ماهر المعيقلي"),
        Reciter("Yasser_Ad-Dussary_128kbps", "ياسر الدوسري"),
        Reciter("Nasser_Alqatami_128kbps", "ناصر القطامي"),
        Reciter("Abu_Bakr_Ash-Shaatree_128kbps", "أبو بكر الشاطري"),
        Reciter("Ghamadi_40kbps", "سعد الغامدي"),
        Reciter("ahmed_ibn_ali_al_ajamy_128kbps", "أحمد بن علي العجمي"),
        Reciter("Fares_Abbad_64kbps", "فارس عباد"),
        Reciter("Muhammad_Ayyoub_128kbps", "محمد أيوب"),
        Reciter("Muhammad_Jibreel_128kbps", "محمد جبريل"),
        Reciter("Hudhaify_128kbps", "علي الحذيفي"),
        Reciter("Salah_Al_Budair_128kbps", "صلاح البدير"),
        Reciter("Abdullah_Basfar_192kbps", "عبد الله بصفر"),
        Reciter("Abdullaah_3awwaad_Al-Juhaynee_128kbps", "عبد الله عواد الجهني"),
        Reciter("Ali_Jaber_64kbps", "علي جابر"),
        Reciter("Hani_Rifai_192kbps", "هاني الرفاعي"),
        Reciter("Akram_AlAlaqimy_128kbps", "أكرم العلاقمي"),
        Reciter("Ibrahim_Akhdar_32kbps", "إبراهيم الأخضر"),
    )

    fun byId(id: String) = all.firstOrNull { it.id == id } ?: all.first()

    private fun name(sura: Int, ayah: Int) = "%03d%03d.mp3".format(sura, ayah)

    fun url(r: Reciter, sura: Int, ayah: Int) = BASE + r.id + "/" + name(sura, ayah)

    fun dir(ctx: Context, r: Reciter) = File(ctx.getExternalFilesDir(null) ?: ctx.filesDir, "audio/${r.id}")

    fun local(ctx: Context, r: Reciter, sura: Int, ayah: Int) = File(dir(ctx, r), name(sura, ayah))

    /** Local file when downloaded, otherwise the stream URL. */
    fun uri(ctx: Context, r: Reciter, sura: Int, ayah: Int): Uri {
        val f = local(ctx, r, sura, ayah)
        return if (f.length() > 0) Uri.fromFile(f) else Uri.parse(url(r, sura, ayah))
    }

    fun isSurahDownloaded(ctx: Context, r: Reciter, sura: Int): Boolean {
        val n = QuranDb.surah(sura).ayahs
        return (1..n).all { local(ctx, r, sura, it).length() > 0 }
    }

    /** Download every ayah of a surah (plus the basmala clip). */
    fun downloadSurah(ctx: Context, r: Reciter, sura: Int, progress: (Float) -> Unit): Boolean {
        val n = QuranDb.surah(sura).ayahs
        val list = buildList { add(1 to 1); for (a in 1..n) add(sura to a) }.distinct()
        list.forEachIndexed { i, (s, a) ->
            val f = local(ctx, r, s, a)
            if (f.length() == 0L && !Http.download(url(r, s, a), f)) return false
            progress((i + 1f) / list.size)
        }
        return true
    }

    fun downloadedSize(ctx: Context, r: Reciter): Long =
        dir(ctx, r).listFiles()?.sumOf { it.length() } ?: 0L

    fun deleteAll(ctx: Context, r: Reciter) { dir(ctx, r).deleteRecursively() }
}
