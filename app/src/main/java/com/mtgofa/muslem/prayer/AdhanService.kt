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
import com.mtgofa.muslem.data.Haptics
import com.mtgofa.muslem.data.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

/** What is sounding right now, for the full-screen adhan page and the settings previews. */
/** [alarm]: the night-prayer alarm (not a settings preview, though it has no prayer) */
data class Sounding(val prayer: Prayer?, val type: EventType?, val title: String, val alarm: Boolean = false)

/**
 * Plays the adhan, the short reminders and the iqama alert as a foreground service on
 * the alarm audio stream, so it is heard even when media volume is down and is never killed
 * mid-adhan.
 */
class AdhanService : Service() {

    private var player: MediaPlayer? = null
    private var repeatsLeft = 0
    private var focus: AudioFocusRequest? = null
    private val main = Handler(Looper.getMainLooper())
    private var finishedNote: Notification? = null
    /**
     * While a sound plays, a media session with "remote" volume takes the volume keys: Android
     * routes the hardware keys to it (screen on or off, app open or not), and any press stops
     * the sound instead of changing the volume.
     */
    private var keys: android.media.session.MediaSession? = null

    private fun grabVolumeKeys() {
        if (keys != null) return
        keys = android.media.session.MediaSession(this, "adhan-volume-keys").apply {
            setPlaybackToRemote(object : android.media.VolumeProvider(VOLUME_CONTROL_RELATIVE, 100, 50) {
                override fun onAdjustVolume(direction: Int) { main.post { finish() } }
                override fun onSetVolumeTo(volume: Int) { main.post { finish() } }
            })
            setPlaybackState(
                android.media.session.PlaybackState.Builder()
                    .setState(android.media.session.PlaybackState.STATE_PLAYING, 0, 1f)
                    .setActions(android.media.session.PlaybackState.ACTION_STOP or android.media.session.PlaybackState.ACTION_PAUSE)
                    .build(),
            )
            setCallback(object : android.media.session.MediaSession.Callback() {
                override fun onStop() { finish() }
                override fun onPause() { finish() }
            })
            isActive = true
        }
    }

    private fun releaseVolumeKeys() {
        keys?.run { isActive = false; release() }
        keys = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Settings.init(this)
        when (intent?.action) {
            ACTION_STOP -> { finish(); return START_NOT_STICKY }
            ACTION_PLAY -> play(intent)
            ACTION_PREVIEW -> preview(intent)
            ACTION_QIYAM -> qiyam()
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

        // on the ringtone volume, a silent phone stays quiet and a phone on vibrate vibrates,
        // unless the user chose to hear the adhan anyway (then it plays on the alarm volume)
        val ringer = am.ringerMode
        if (!Settings.adhanInSilent && ringer == AudioManager.RINGER_MODE_SILENT) { finish(); return }
        if (Settings.vibrateOnly || (!Settings.adhanInSilent && ringer == AudioManager.RINGER_MODE_VIBRATE)) {
            vibrateAlert(
                when {
                    type == EventType.IQAMA -> Haptics.IQAMA
                    fullAdhan -> Haptics.ADHAN
                    else -> Haptics.REMINDER
                },
            )
            return
        }

        when {
            fullAdhan -> startSound(Muezzins.uri(this, Muezzins.byId(Settings.muezzinFor(prayer))), repeats = 1)
            type == EventType.IQAMA -> startSound(Settings.iqamaSound.uri(this), repeats = 1)
            else -> startSound(Settings.preSound.uri(this), repeats = 1)
        }
    }

    /** The night prayer alarm: its sound on the alarm volume a few times, or vibration only. */
    private fun qiyam() {
        _sounding.value = Sounding(null, null, "قيام الليل", alarm = true)
        startInForeground(buildNote("قيام الليل", "﴿وَبِالْأَسْحَارِ هُمْ يَسْتَغْفِرُونَ﴾", ongoing = true, fullScreen = true))
        finishedNote = null
        if (Settings.qiyamVibrate) vibrateAlert(Haptics.QIYAM)
        else startSound(Settings.qiyamSound.uri(this), repeats = if (Settings.qiyamSound == AlertSound.TASBEEH) 6 else 3, alarm = true, gapMs = 1500)
    }

    private fun preview(intent: Intent) {
        val muezzin = intent.getStringExtra("muezzin")
        val sound = AlertSound.of(intent.getStringExtra("sound"), AlertSound.TASBEEH)
        val title = if (muezzin != null) Muezzins.byId(muezzin).name else sound.ar
        _sounding.value = Sounding(null, null, title)
        startInForeground(buildNote("تجربة الصوت", title, ongoing = true, fullScreen = false))
        finishedNote = null
        if (muezzin != null) startSound(Muezzins.uri(this, Muezzins.byId(muezzin)), 1)
        else startSound(sound.uri(this), 1)
    }

    private fun rawUri(res: Int): Uri = Uri.parse("android.resource://$packageName/$res")

    private var vibrating = false

    /** A silent alert in its own vibration pattern; the stop button and volume keys end it early. */
    private fun vibrateAlert(pattern: LongArray) {
        releasePlayer()
        vibrating = true
        Haptics.pattern(this, *pattern)
        grabVolumeKeys()
        main.postDelayed({ finish() }, pattern.sum() + 300)
    }

    /** Volume follows the phone: the ringtone volume, or the alarm volume when set to play in silent mode. */
    private fun startSound(uri: Uri, repeats: Int, alarm: Boolean = false, gapMs: Long = 350) {
        releasePlayer()
        repeatsLeft = repeats
        val attrs = AudioAttributes.Builder()
            .setUsage(if (alarm || Settings.adhanInSilent) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
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
                    if (repeatsLeft > 0) main.postDelayed({ runCatching { it.seekTo(0); it.start() } }, gapMs)
                    else finish()
                }
                setOnErrorListener { _, _, _ -> finish(); true }
                prepare()
                start()
            }
            grabVolumeKeys()
        } catch (e: Exception) {
            finish()
        }
    }

    private fun releasePlayer() {
        if (vibrating) { Haptics.cancel(this); vibrating = false }
        releaseVolumeKeys()
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
        const val ACTION_QIYAM = "com.mtgofa.muslem.QIYAM"
        private const val NOTE_PLAYING = 10
        private const val NOTE_DONE = 11

        private val _sounding = MutableStateFlow<Sounding?>(null)
        val sounding: StateFlow<Sounding?> = _sounding

        fun stopIntent(ctx: Context): PendingIntent = PendingIntent.getService(
            ctx, 4, Intent(ctx, AdhanService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE,
        )

        /** Stop a settings preview, leaving a real adhan alone. */
        fun stopPreview(ctx: Context) {
            val s = _sounding.value ?: return
            if (s.prayer == null && !s.alarm) stop(ctx)
        }

        fun stop(ctx: Context) {
            if (_sounding.value != null) ctx.startService(Intent(ctx, AdhanService::class.java).setAction(ACTION_STOP))
        }

        /** Play a muezzin, or an alert sound when [muezzinId] is null, for the settings pages. */
        fun preview(ctx: Context, muezzinId: String?, sound: AlertSound = AlertSound.TASBEEH) {
            androidx.core.content.ContextCompat.startForegroundService(
                ctx, Intent(ctx, AdhanService::class.java).setAction(ACTION_PREVIEW).apply {
                    if (muezzinId != null) putExtra("muezzin", muezzinId)
                    putExtra("sound", sound.name)
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
