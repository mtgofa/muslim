package com.mtgofa.muslem.quran

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.mtgofa.muslem.MainActivity
import com.mtgofa.muslem.data.Settings
import com.mtgofa.muslem.prayer.arabicDigits
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Background recitation with a media notification and lock-screen controls. */
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        val open = PendingIntent.getActivity(
            this, 7, Intent(this, MainActivity::class.java).putExtra("openMushaf", true),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, player)
            .setSessionActivity(open)
            .setCallback(object : MediaSession.Callback {
                // controllers strip the uri; restore it from the request metadata
                override fun onAddMediaItems(
                    mediaSession: MediaSession, controller: MediaSession.ControllerInfo, mediaItems: MutableList<MediaItem>,
                ): ListenableFuture<MutableList<MediaItem>> = Futures.immediateFuture(
                    mediaItems.map { it.buildUpon().setUri(it.requestMetadata.mediaUri).build() }.toMutableList(),
                )
            })
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = session?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        session?.run { player.release(); release() }
        session = null
        super.onDestroy()
    }
}

data class PlayState(
    val active: Boolean = false,
    val playing: Boolean = false,
    val buffering: Boolean = false,
    val current: AyahRef? = null,
    val error: String? = null,
)

/** UI-side handle on [PlaybackService]: builds ayah queues with repeats and reports progress. */
object QuranPlayer {
    private var controller: MediaController? = null
    private var future: ListenableFuture<MediaController>? = null
    private val _state = MutableStateFlow(PlayState())
    val state: StateFlow<PlayState> = _state

    /** what the current queue covers, to continue into the next surah when it ends */
    private var rangeEnd: AyahRef? = null
    private var continuous = false
    private lateinit var appCtx: Context

    private fun withController(ctx: Context, block: (MediaController) -> Unit) {
        appCtx = ctx.applicationContext
        controller?.let { return block(it) }
        val f = future ?: MediaController.Builder(
            appCtx, SessionToken(appCtx, ComponentName(appCtx, PlaybackService::class.java)),
        ).buildAsync().also { future = it }
        f.addListener({
            val c = runCatching { f.get() }.getOrNull() ?: run { future = null; return@addListener }
            if (controller == null) {
                controller = c
                c.addListener(listener)
            }
            block(c)
        }, ContextCompat.getMainExecutor(appCtx))
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish(player)
        override fun onPlayerError(error: PlaybackException) {
            _state.value = _state.value.copy(error = "تعذّر تشغيل التلاوة — تأكّد من الاتصال بالإنترنت أو نزّل السورة")
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) onQueueEnded()
        }
    }

    private fun publish(p: Player) {
        val id = p.currentMediaItem?.mediaId
        val parts = id?.split(':')
        val cur = if (parts != null && parts.size >= 2 && parts.getOrNull(2) != "b") AyahRef(parts[0].toInt(), parts[1].toInt())
        else if (parts?.getOrNull(2) == "b") AyahRef(parts[3].toInt(), 0) else null
        _state.value = PlayState(
            active = p.mediaItemCount > 0 && p.playbackState != Player.STATE_IDLE,
            playing = p.isPlaying,
            buffering = p.playbackState == Player.STATE_BUFFERING,
            current = cur,
            error = if (p.playerError != null) _state.value.error else null,
        )
    }

    private fun onQueueEnded() {
        val end = rangeEnd ?: return
        if (!continuous) return
        val s = end.sura
        if (end.ayah == QuranDb.surah(s).ayahs && s < 114) {
            play(appCtx, AyahRef(s + 1, 1), AyahRef(s + 1, QuranDb.surah(s + 1).ayahs), continuous = true)
        }
    }

    /**
     * Play [from]..[to] (inclusive, may span surahs). Each ayah plays [Settings.ayahRepeat]
     * times and the whole range [Settings.rangeRepeat] times (-1 = forever).
     */
    fun play(ctx: Context, from: AyahRef, to: AyahRef, continuous: Boolean = false) {
        val reciter = Reciters.byId(Settings.reciter)
        val ayahs = buildList {
            var s = from.sura; var a = from.ayah
            while (s < to.sura || (s == to.sura && a <= to.ayah)) {
                add(AyahRef(s, a))
                a++
                if (a > QuranDb.surah(s).ayahs) { s++; a = 1; if (s > 114) break }
            }
        }
        if (ayahs.isEmpty()) return
        val perAyah = Settings.ayahRepeat.coerceAtLeast(1)
        val rounds = Settings.rangeRepeat.let { if (it <= 0) 1 else it }
        val forever = Settings.rangeRepeat < 0
        val items = ArrayList<MediaItem>()
        repeat(rounds) { round ->
            for (r in ayahs) {
                if (r.ayah == 1 && r.sura != 1 && r.sura != 9) items += item(ctx, reciter, 1, 1, "${r.sura}:0:b:${r.sura}:$round", r.sura, 0)
                repeat(perAyah) { k -> items += item(ctx, reciter, r.sura, r.ayah, "${r.sura}:${r.ayah}:$round:$k", r.sura, r.ayah) }
            }
        }
        rangeEnd = to
        this.continuous = continuous && !forever && Settings.rangeRepeat == 1 && perAyah >= 1
        _state.value = _state.value.copy(error = null, current = from, active = true)
        withController(ctx) { c ->
            c.setMediaItems(items)
            c.repeatMode = if (forever) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
            c.setPlaybackSpeed(Settings.playSpeed)
            c.prepare()
            c.play()
        }
    }

    private fun item(ctx: Context, r: Reciter, sura: Int, ayah: Int, id: String, showSura: Int, showAyah: Int): MediaItem {
        val uri = Reciters.uri(ctx, r, sura, ayah)
        val surah = QuranDb.surah(showSura)
        val title = if (showAyah == 0) "سورة ${surah.name} — البسملة" else "سورة ${surah.name} — آية ${arabicDigits(showAyah)}"
        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(uri)
            .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
            .setMediaMetadata(
                MediaMetadata.Builder().setTitle(title).setArtist(r.name + if (r.style != "مرتّل") " (${r.style})" else "")
                    .setAlbumTitle("القرآن الكريم").build(),
            )
            .build()
    }

    fun toggle(ctx: Context) = withController(ctx) { if (it.isPlaying) it.pause() else it.play() }
    fun pause(ctx: Context) = withController(ctx) { it.pause() }

    fun stop(ctx: Context) = withController(ctx) {
        continuous = false
        it.stop(); it.clearMediaItems()
        _state.value = PlayState()
    }

    /** Jump to the next / previous distinct ayah in the queue. */
    fun skip(ctx: Context, forward: Boolean) = withController(ctx) { c ->
        val cur = c.currentMediaItem?.mediaId?.split(':')?.take(2)
        var i = c.currentMediaItemIndex
        while (true) {
            i += if (forward) 1 else -1
            if (i !in 0 until c.mediaItemCount) return@withController
            val id = c.getMediaItemAt(i).mediaId.split(':').take(2)
            if (id != cur) { c.seekTo(i, 0); c.play(); return@withController }
        }
    }

    fun setSpeed(ctx: Context, speed: Float) {
        Settings.playSpeed = speed
        withController(ctx) { it.setPlaybackSpeed(speed) }
    }

    /** Called when the app starts, so a running recitation shows up again. */
    fun reconnect(ctx: Context) = withController(ctx) { publish(it) }
}
