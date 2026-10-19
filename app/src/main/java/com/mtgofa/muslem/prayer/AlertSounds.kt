package com.mtgofa.muslem.prayer

import android.content.Context
import android.net.Uri
import com.mtgofa.muslem.R

/**
 * Short sounds for the alerts around the adhan: phrases cut from a full adhan (Ali Mulla) and
 * from the iqama, besides «سبحان الله». Each alert type picks its own.
 */
enum class AlertSound(val ar: String, private val raw: Int) {
    TASBEEH("سبحان الله", R.raw.tasbeeh),
    HAYYA("حيّ على الصلاة", R.raw.alert_hayya),
    TAKBIR("الله أكبر", R.raw.alert_takbir),
    QAD_QAMAT("قد قامت الصلاة", R.raw.alert_qad_qamat),
    IQAMA("الإقامة كاملة", R.raw.iqama);

    fun uri(ctx: Context): Uri = Uri.parse("android.resource://${ctx.packageName}/$raw")

    companion object {
        /** before the adhan, the «تنبيه» mode at prayer time, and the night prayer alarm */
        val FOR_ALERTS = listOf(TASBEEH, HAYYA, TAKBIR)
        val FOR_IQAMA = listOf(IQAMA, QAD_QAMAT, HAYYA, TASBEEH)

        fun of(name: String?, fallback: AlertSound) = entries.firstOrNull { it.name == name } ?: fallback
    }
}
