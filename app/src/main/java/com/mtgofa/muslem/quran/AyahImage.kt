package com.mtgofa.muslem.quran

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
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
    private const val EMERALD = 0xFF0F7A62.toInt()
    private const val INK = 0xFF1A1A1A.toInt()
    private const val MUTED = 0xFF6B6B6B.toInt()

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

        val logo = 150
        val contentTop = 130
        val contentH = (card.lines.size * lineH).toInt()
        val cardTop = 80
        val cardBottom = contentTop + contentH + 70
        val refFont = ctx.resources.getFont(R.font.plexar_bold)
        val ref = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = refFont; textSize = 40f; color = EMERALD }
        val refLayout = StaticLayout.Builder.obtain(label, 0, label.length, ref, inner)
            .setAlignment(Layout.Alignment.ALIGN_CENTER).build()
        val refTop = cardBottom + 40
        val footTop = refTop + refLayout.height + 40
        val H = footTop + logo + 150

        val bmp = Bitmap.createBitmap(W, H.toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)

        // the card: the Mushaf's paper, with a thin emerald frame
        val card2 = RectF(40f, cardTop.toFloat(), (W - 40).toFloat(), cardBottom.toFloat())
        c.drawRoundRect(card2, 48f, 48f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PAPER })
        c.drawRoundRect(card2, 48f, 48f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = EMERALD; style = Paint.Style.STROKE; strokeWidth = 4f })

        c.save(); c.translate(pad.toFloat(), contentTop.toFloat()); drawMushaf(c, lay, INK); c.restore()
        c.save(); c.translate(pad.toFloat(), refTop.toFloat()); refLayout.draw(c); c.restore()

        // footer: the logo, and the app's name under it
        val logoDrawable = ContextCompat.getDrawable(ctx, R.mipmap.ic_launcher)
        val lx = (W - logo) / 2
        logoDrawable?.let {
            it.setBounds(lx, footTop, lx + logo, footTop + logo)
            it.draw(c)
        }
        val credit = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = refFont; textSize = 44f; color = EMERALD; textAlign = Paint.Align.CENTER }
        c.drawText("بواسطة تطبيق مسلم", (W / 2).toFloat(), (footTop + logo + 80).toFloat(), credit)
        return bmp
    }
}
