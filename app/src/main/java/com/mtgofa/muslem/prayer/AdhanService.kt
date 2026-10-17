package com.mtgofa.muslem.prayer

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.mtgofa.muslem.MainActivity
import com.mtgofa.muslem.MuslemApp
import com.mtgofa.muslem.R
import com.mtgofa.muslem.data.AlertMode
import com.mtgofa.muslem.data.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

/** What is sounding right now, for the full-screen adhan page and the settings previews. */
data class Sounding(val prayer: Prayer?, val type: EventType?, val title: String)

/**
 * Plays the adhan, the "سبحان الله" reminders and the iqama alert as a foreground service on
 * the alarm audio stream, so it is heard even when media volume is down and is never killed
 * mid-adhan.
 */
class AdhanService : Service() {

    private var player: MediaPlayer? = null
    private var repeatsLeft = 0
    private var focus: AudioFocusRequest? = null
    private val main = Handler(Looper.getMainLooper())
    private var finishedNote: Notification? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Settings.init(this)
        when (intent?.action) {
            ACTION_STOP -> { finish(); return START_NOT_STICKY }
            ACTION_PLAY -> play(intent)
            ACTION_PREVIEW -> preview(intent)
            else -> { finish(); return START_NOT_STICKY }
        }
        return START_NOT_STICKY
    }

    private fun play(intent: Intent) {
        val type = runCatching { EventType.valueOf(intent.getStringExtra("type")!!) }.getOrDefault(EventType.ADHAN)
        val prayer = runCatching { Prayer.valueOf(intent.getStringExtra("prayer")!!) }.getOrDefault(Prayer.DHUHR)
        val prayerTime = intent.getLongExtra("prayerTime", System.currentTimeMillis())
        val date = Instant.ofEpochMilli(prayerTime).atZone(Settings.zone).toLocalDate()
        val name = prayer.label(date)
        val clock = formatTime(prayerTime)

        val (title, text) = when (type) {
            EventType.PRE -> "اقترب موعد ${prayerWord(prayer)} $name" to
                "باقي ${arabicDigits(Settings.preAlert(prayer))} دقيقة — الأذان الساعة $clock"
            EventType.ADHAN -> if (prayer == Prayer.SUNRISE) "حان الآن وقت الشروق" to "الساعة $clock"
                else "حان الآن موعد أذان $name" to "الساعة $clock · ${Settings.locationName}"
            EventType.IQAMA -> "حان موعد إقامة صلاة $name" to "أُذّن للصلاة الساعة $clock"
        }
        val fullAdhan = type == EventType.ADHAN && Settings.mode(prayer) == AlertMode.ADHAN && prayer.hasAdhan
        _sounding.value = Sounding(prayer, type, title)

        startInForeground(buildNote(title, text, ongoing = true, fullScreen = fullAdhan))
        finishedNote = buildNote(title, text, ongoing = false, fullScreen = false)

        val am = getSystemService(AudioManager::class.java)
        val inCall = am.mode == AudioManager.MODE_IN_CALL || am.mode == AudioManager.MODE_IN_COMMUNICATION
        if (inCall && Settings.muteInCall) { finish(); return }

        // on the ringtone volume, a silent or vibrating phone stays quiet unless the user chose
        // to hear the adhan anyway (then it plays on the alarm volume)
        if (!Settings.adhanInSilent && am.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
            if (am.ringerMode == AudioManager.RINGER_MODE_VIBRATE) vibrate()
            finish(); return
        }

        when {
            fullAdhan -> startSound(Muezzins.uri(this, Muezzins.byId(Settings.muezzinFor(prayer))), repeats = 1)
            // the iqama alert calls the iqama itself; reminders say "سبحان الله"
            type == EventType.IQAMA -> startSound(rawUri(R.raw.iqama), repeats = 1)
            else -> startSound(tasbeehUri(this), repeats = 1)
        }
    }

    private fun preview(intent: Intent) {
        val muezzin = intent.getStringExtra("muezzin")
        val title = if (muezzin != null) Muezzins.byId(muezzin).name else "سبحان الله"
        _sounding.value = Sounding(null, null, title)
        startInForeground(buildNote("تجربة الصوت", title, ongoing = true, fullScreen = false))
        finishedNote = null
        if (muezzin != null) startSound(Muezzins.uri(this, Muezzins.byId(muezzin)), 1)
        else startSound(tasbeehUri(this), 1)
    }

    private fun rawUri(res: Int): Uri = Uri.parse("android.resource://$packageName/$res")

    private fun vibrate() {
        val v = getSystemService(android.os.Vibrator::class.java) ?: return
        v.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 400, 250, 400, 250, 400), -1))
    }

    /** Volume follows the phone: the ringtone volume, or the alarm volume when set to play in silent mode. */
    private fun startSound(uri: Uri, repeats: Int) {
        releasePlayer()
        repeatsLeft = repeats
        val attrs = AudioAttributes.Builder()
            .setUsage(if (Settings.adhanInSilent) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val am = getSystemService(AudioManager::class.java)
        focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attrs).build().also { am.requestAudioFocus(it) }
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(attrs)
                setDataSource(this@AdhanService, uri)
                setOnCompletionListener {
                    repeatsLeft--
                    if (repeatsLeft > 0) main.postDelayed({ runCatching { it.seekTo(0); it.start() } }, 350)
                    else finish()
                }
                setOnErrorListener { _, _, _ -> finish(); true }
                prepare()
                start()
            }
        } catch (e: Exception) {
            finish()
        }
    }

    private fun releasePlayer() {
        main.removeCallbacksAndMessages(null)
        player?.runCatching { stop(); release() }
        player = null
        focus?.let { getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it) }
        focus = null
    }

    private fun finish() {
        releasePlayer()
        _sounding.value = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        finishedNote?.let { getSystemService(NotificationManager::class.java).notify(NOTE_DONE, it) }
        finishedNote = null
        stopSelf()
    }

    override fun onDestroy() {
        releasePlayer()
        _sounding.value = null
        super.onDestroy()
    }

    private fun startInForeground(n: Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        ServiceCompat.startForeground(this, NOTE_PLAYING, n, type)
    }

    private fun buildNote(title: String, text: String, ongoing: Boolean, fullScreen: Boolean): Notification {
        val open = PendingIntent.getActivity(
            this, 3, Intent(this, if (fullScreen) AdhanActivity::class.java else MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val b = NotificationCompat.Builder(this, MuslemApp.CH_ADHAN)
            .setSmallIcon(R.drawable.ic_stat_mosque)
            .setContentTitle(title)
            .setContentText(text)
            .setColor(0xFF0F7A62.toInt())
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(open)
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing)
            .setSilent(true)
        if (ongoing) {
            b.addAction(0, "إيقاف", stopIntent(this))
            b.setDeleteIntent(stopIntent(this))
        }
        if (fullScreen) b.setFullScreenIntent(open, true)
        return b.build()
    }

    companion object {
        const val ACTION_PLAY = "com.mtgofa.muslem.PLAY"
        const val ACTION_STOP = "com.mtgofa.muslem.STOP"
        const val ACTION_PREVIEW = "com.mtgofa.muslem.PREVIEW"
        private const val NOTE_PLAYING = 10
        private const val NOTE_DONE = 11

        private val _sounding = MutableStateFlow<Sounding?>(null)
        val sounding: StateFlow<Sounding?> = _sounding

        fun stopIntent(ctx: Context): PendingIntent = PendingIntent.getService(
            ctx, 4, Intent(ctx, AdhanService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE,
        )

        fun stop(ctx: Context) {
            if (_sounding.value != null) ctx.startService(Intent(ctx, AdhanService::class.java).setAction(ACTION_STOP))
        }

        /** Play a muezzin (or the "سبحان الله" clip when [muezzinId] is null) for the settings page. */
        fun preview(ctx: Context, muezzinId: String?) {
            androidx.core.content.ContextCompat.startForegroundService(
                ctx, Intent(ctx, AdhanService::class.java).setAction(ACTION_PREVIEW).apply {
                    if (muezzinId != null) putExtra("muezzin", muezzinId)
                },
            )
        }

        fun tasbeehUri(ctx: Context): Uri = Uri.parse("android.resource://${ctx.packageName}/${R.raw.tasbeeh}")

        private fun prayerWord(p: Prayer) = if (p == Prayer.SUNRISE) "" else "أذان"

        fun formatTime(millis: Long): String =
            Instant.ofEpochMilli(millis).atZone(Settings.zone).format(TIME_FMT)
                .replace("AM", "ص").replace("PM", "م")

        private val TIME_FMT = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    }
}
