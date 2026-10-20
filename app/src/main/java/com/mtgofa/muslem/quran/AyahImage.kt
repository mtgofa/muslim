package com.mtgofa.muslem.quran

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
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
    fun shareIntent(ctx: Context, text: String, label: String): Intent? = runCatching {
        val bmp = render(ctx, text, label)
        val dir = File(ctx.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "ayah.png")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }.getOrNull()

    private fun render(ctx: Context, text: String, label: String): Bitmap {
        val pad = 90
        val inner = W - 2 * pad
        val logoDrawable = ContextCompat.getDrawable(ctx, R.mipmap.ic_launcher)
        val tf = ctx.resources.getFont(R.font.plexar_regular)
        val bold = ctx.resources.getFont(R.font.plexar_bold)

        val body = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = tf; textSize = 58f; color = INK }
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, body, inner)
            .setAlignment(Layout.Alignment.ALIGN_CENTER).setLineSpacing(0f, 1.55f).build()
        val ref = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = bold; textSize = 40f; color = EMERALD }
        val refLayout = StaticLayout.Builder.obtain(label, 0, label.length, ref, inner)
            .setAlignment(Layout.Alignment.ALIGN_CENTER).build()

        val logo = 150
        val footerH = logo + 110
        val cardTop = 80
        val textTop = cardTop + 110
        val refTop = textTop + layout.height + 40
        val cardBottom = refTop + refLayout.height + 60
        val H = cardBottom + footerH + 80

        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)

        // the card: paper, with a thin emerald frame
        val card = RectF(40f, cardTop.toFloat(), (W - 40).toFloat(), cardBottom.toFloat())
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PAPER }
        c.drawRoundRect(card, 48f, 48f, bg)
        val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = EMERALD; style = Paint.Style.STROKE; strokeWidth = 4f }
        c.drawRoundRect(card, 48f, 48f, frame)

        c.save(); c.translate(pad.toFloat(), textTop.toFloat()); layout.draw(c); c.restore()
        c.save(); c.translate(pad.toFloat(), refTop.toFloat()); refLayout.draw(c); c.restore()

        // footer: the logo, and the app's name under it
        val footTop = cardBottom + 60
        val lx = (W - logo) / 2
        logoDrawable?.let {
            it.setBounds(lx, footTop, lx + logo, footTop + logo)
            it.draw(c)
        }
        val name = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = bold; textSize = 50f; color = EMERALD; textAlign = Paint.Align.CENTER }
        c.drawText(ctx.getString(R.string.app_name), (W / 2).toFloat(), (footTop + logo + 70).toFloat(), name)
        return bmp
    }
}
