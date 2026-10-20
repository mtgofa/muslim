package com.mtgofa.muslem.quran

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.mtgofa.muslem.R
import java.io.File

/**
 * The ayah as a picture to share: the text on the Mushaf's paper colour, the reference under
 * it, and at the foot the app's logo with its name, as other apps do when they share a card.
 */
object AyahImage {
    private const val W = 1080
    private const val PAPER = 0xFFFFF8EB.toInt()
    private const val CREAM = 0xFFFBF5E6.toInt()
    private const val GOLD = 0xFFB8893A.toInt()
    private const val EMERALD = 0xFF0F7A62.toInt()
    private const val INK = 0xFF1A1A1A.toInt()

    /** Draws the card and returns a shareable image intent, or null if it could not be made. */
    fun shareIntent(ctx: Context, sura: Int, ayah: Int, text: String, label: String): Intent? = runCatching {
        val bmp = render(ctx, sura, ayah, text, label) ?: return@runCatching null
        val dir = File(ctx.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "ayah.png")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }.getOrNull()

    /**
     * The ayah set in the Mushaf's own font and colours, with the surah band above it when it opens
     * the surah and its ayah-end mark (with the number) after it; the reference and the logo follow.
     */
    private fun render(ctx: Context, sura: Int, ayah: Int, text: String, label: String): Bitmap? {
        val pad = 90
        val inner = W - 2 * pad
        val page = QuranDb.pageOf(sura, ayah)
        val card = ayahCardPage(QuranDb.page(page), sura, ayah)
        if (card.lines.isEmpty()) return null
        val mstyle = MushafStyle.of(com.mtgofa.muslem.data.Settings.tajweed)
        // a first pass finds the text size the words fit at, then the lines are spaced for it
        val probe = layoutPage(ctx, card, inner.toFloat(), inner * 0.2f * 15, mstyle)
        val lineH = probe.textSize * 1.3f
        val lay = layoutPage(ctx, card, inner.toFloat(), lineH * 15, mstyle)

        // like a printed card: the Mushaf's banner and ayah on a cream ground, then a small logo
        // and the credit underneath in a soft gold
        val contentTop = 70
        val contentH = (card.lines.size * lineH).toInt()
        val chip = 130
        val footTop = contentTop + contentH + 40
        val H = footTop + chip + 150

        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(CREAM)

        c.save(); c.translate(pad.toFloat(), contentTop.toFloat()); drawMushaf(c, lay, INK); c.restore()

        // the logo on a soft round chip, then the credit
        val cx = W / 2f
        val chipCy = footTop + chip / 2f
        c.drawCircle(cx, chipCy, chip / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF1E7CC.toInt() })
        ContextCompat.getDrawable(ctx, R.mipmap.ic_launcher)?.let {
            val side = (chip * 0.82f).toInt()
            val l = (cx - side / 2f).toInt(); val t = (chipCy - side / 2f).toInt()
            it.setBounds(l, t, l + side, t + side)
            it.draw(c)
        }
        val credit = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = ctx.resources.getFont(R.font.plexar_semibold); textSize = 40f; color = GOLD; textAlign = Paint.Align.CENTER
        }
        c.drawText("بواسطة تطبيق مسلم", cx, (footTop + chip + 70).toFloat(), credit)
        return bmp
    }
}
