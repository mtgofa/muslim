package com.mtgofa.muslem.prayer

import android.content.Context
import android.net.Uri
import com.mtgofa.muslem.R
import com.mtgofa.muslem.data.Http
import java.io.File

/** One adhan voice: bundled in the APK ([raw]) or downloaded once from [file]. */
data class Muezzin(val id: String, val name: String, val raw: Int = 0, val file: String = "") {
    val bundled get() = raw != 0
}

object Muezzins {
    private const val BASE = "https://raw.githubusercontent.com/abodehq/Athan-MP3/master/Sounds/"

    val all = listOf(
        Muezzin("abdulbasit", "عبد الباسط عبد الصمد", raw = R.raw.adhan_abdulbasit),
        Muezzin("makkah", "أذان الحرم المكي", raw = R.raw.adhan_makkah),
        Muezzin("refaat", "محمد رفعت", file = "Athan Mohammad Ref3at.mp3"),
        Muezzin("minshawy", "محمد صديق المنشاوي", file = "Athan Mohammad Almenshawy.mp3"),
        Muezzin("naina", "أحمد نعينع", file = "Athan Ahmad Nuyne3.mp3"),
        Muezzin("alafasy", "مشاري العفاسي", file = "Athan Mishary Alafasi.mp3"),
        Muezzin("qatami", "ناصر القطامي", file = "Athan Nasser Alqatami.mp3"),
        Muezzin("malki", "حمدان المالكي", file = "Athan Hamdan Almalki.mp3"),
        Muezzin("zahrani", "منصور الزهراني", file = "Athan Mansoor Az-Zahrani.mp3"),
        Muezzin("arkani", "إبراهيم الأركاني", file = "Athan Ibrahim Al-Arkani.mp3"),
        Muezzin("deghreri", "حمد الدغريري", file = "Athan Hamad Deghreri.mp3"),
        Muezzin("hamathani", "ماجد الحمداني", file = "Athan Majed Al-hamathani.mp3"),
        Muezzin("khatba", "صهيب خطبا", file = "Athan Suhaib Khatba.mp3"),
        Muezzin("fajr_chebae", "أذان الفجر — مالك شيبة", file = "Athan Al-fajer - Malek chebae.mp3"),
    )

    fun byId(id: String) = all.firstOrNull { it.id == id } ?: all.first()

    private fun localFile(ctx: Context, m: Muezzin) = File(File(ctx.filesDir, "adhan"), "${m.id}.mp3")

    fun isAvailable(ctx: Context, m: Muezzin) = m.bundled || localFile(ctx, m).length() > 50_000

    /** A playable Uri, falling back to the first bundled voice if this one is not downloaded. */
    fun uri(ctx: Context, m: Muezzin): Uri = when {
        m.bundled -> Uri.parse("android.resource://${ctx.packageName}/${m.raw}")
        isAvailable(ctx, m) -> Uri.fromFile(localFile(ctx, m))
        else -> uri(ctx, all.first())
    }

    /** Blocking download; returns true on success. [progress] gets 0..1. */
    fun download(ctx: Context, m: Muezzin, progress: (Float) -> Unit = {}): Boolean {
        if (m.bundled) return true
        val out = localFile(ctx, m)
        return Http.download(BASE + Uri.encode(m.file), out, progress)
    }

    fun delete(ctx: Context, m: Muezzin) { if (!m.bundled) localFile(ctx, m).delete() }
}
