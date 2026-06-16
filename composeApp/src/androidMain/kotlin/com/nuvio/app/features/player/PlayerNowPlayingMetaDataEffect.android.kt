package com.nuvio.app.features.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.nuvio.app.R
import com.nuvio.app.core.ui.formatEpisodeCode
import com.nuvio.app.core.ui.rememberEpisodeCodeFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "NuvioNowPlaying"
private const val CHANNEL_ID = "nuvio_playback"
private const val CHANNEL_NAME = "Playback"
private const val NOTIFICATION_ID = 7103
private const val SEEK_INTERVAL_MS = 10_000L

private const val ACTION_PLAY = "com.nuvio.app.features.player.nowplaying.PLAY"
private const val ACTION_PAUSE = "com.nuvio.app.features.player.nowplaying.PAUSE"
private const val ACTION_REWIND = "com.nuvio.app.features.player.nowplaying.REWIND"
private const val ACTION_FORWARD = "com.nuvio.app.features.player.nowplaying.FORWARD"

private enum class AndroidNowPlayingArtworkShape(
    val width: Int,
    val height: Int,
) {
    Poster2x3(width = 600, height = 900),
    EpisodeThumbnail19x6(width = 1140, height = 360),
}

@Composable
internal actual fun PlayerScreenRuntime.PlayerNowPlayingMetaDataEffect() {
    val context = LocalContext.current.applicationContext
    val latestController = rememberUpdatedState(playerController)
    val snapshot = playbackSnapshot

    val episodeCodeFormat = rememberEpisodeCodeFormat()

    val nowPlayingText = remember(
        title,
        activeSeasonNumber,
        activeEpisodeNumber,
        activeEpisodeTitle,
        episodeCodeFormat,
    ) {
        buildNowPlayingText(
            title = title,
            seasonNumber = activeSeasonNumber,
            episodeNumber = activeEpisodeNumber,
            episodeTitle = activeEpisodeTitle,
            episodeCodeFormat = episodeCodeFormat,
        )
    }

    val isSeriesPlayback = activeSeasonNumber != null ||
        activeEpisodeNumber != null ||
        activeEpisodeTitle?.trim()?.isNotEmpty() == true

    val posterArtworkUrl = remember(poster) {
        poster
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    val episodeArtworkUrl = remember(activeEpisodeThumbnail) {
        activeEpisodeThumbnail
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    val artworkShape = if (isSeriesPlayback && episodeArtworkUrl != null) {
        AndroidNowPlayingArtworkShape.EpisodeThumbnail19x6
    } else {
        AndroidNowPlayingArtworkShape.Poster2x3
    }

    val artworkUrl = if (artworkShape == AndroidNowPlayingArtworkShape.EpisodeThumbnail19x6) {
        episodeArtworkUrl
    } else {
        posterArtworkUrl
    }

    var artworkBitmap by remember(artworkUrl, artworkShape) { mutableStateOf<Bitmap?>(null) }

    val mediaSession = remember(context) {
        MediaSession(context, "NuvioPlayer").apply {
            setFlags(
                MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS,
            )
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    latestController.value?.play()
                }

                override fun onPause() {
                    latestController.value?.pause()
                }

                override fun onSeekTo(pos: Long) {
                    latestController.value?.seekTo(pos.coerceAtLeast(0L))
                }

                override fun onFastForward() {
                    latestController.value?.seekBy(SEEK_INTERVAL_MS)
                }

                override fun onRewind() {
                    latestController.value?.seekBy(-SEEK_INTERVAL_MS)
                }

                override fun onSkipToNext() {
                    latestController.value?.seekBy(SEEK_INTERVAL_MS)
                }

                override fun onSkipToPrevious() {
                    latestController.value?.seekBy(-SEEK_INTERVAL_MS)
                }
            })
        }
    }

    SideEffect {
        AndroidNowPlayingActionRegistry.bind { latestController.value }
        mediaSession.isActive = playerController != null
    }

    DisposableEffect(context, mediaSession) {
        mediaSession.isActive = true
        onDispose {
            AndroidNowPlayingActionRegistry.clear()
            mediaSession.isActive = false
            mediaSession.release()
            AndroidNowPlayingNotification.cancel(context)
        }
    }

    LaunchedEffect(artworkUrl, artworkShape) {
        artworkBitmap = loadArtworkBitmap(artworkUrl, artworkShape)
    }

    LaunchedEffect(mediaSession, nowPlayingText, artworkBitmap, snapshot.durationMs) {
        mediaSession.setMetadata(
            buildAndroidMediaMetadata(
                title = nowPlayingText.title,
                subtitle = nowPlayingText.subtitle,
                artwork = artworkBitmap,
                durationMs = snapshot.durationMs,
            ),
        )
    }

    LaunchedEffect(
        mediaSession,
        snapshot.isPlaying,
        snapshot.isEnded,
        snapshot.positionMs,
        snapshot.bufferedPositionMs,
        snapshot.playbackSpeed,
    ) {
        mediaSession.setPlaybackState(buildAndroidPlaybackState(snapshot))
    }

    LaunchedEffect(
        context,
        mediaSession,
        nowPlayingText,
        artworkBitmap,
        snapshot.isPlaying,
    ) {
        AndroidNowPlayingNotification.show(
            context = context,
            mediaSession = mediaSession,
            title = nowPlayingText.title,
            subtitle = nowPlayingText.subtitle,
            artwork = artworkBitmap,
            isPlaying = snapshot.isPlaying,
        )
    }
}

private data class AndroidNowPlayingText(
    val title: String,
    val subtitle: String?,
)

private fun buildNowPlayingText(
    title: String,
    seasonNumber: Int?,
    episodeNumber: Int?,
    episodeTitle: String?,
    episodeCodeFormat: com.nuvio.app.core.ui.EpisodeCodeFormat,
): AndroidNowPlayingText {
    val contentTitle = title
        .trim()
        .takeIf { it.isNotEmpty() }
        ?: "Nuvio"

    val episodePrefix = when {
        seasonNumber != null && episodeNumber != null ->
            formatEpisodeCode(seasonNumber, episodeNumber, episodeCodeFormat)
        episodeNumber != null ->
            "E${episodeNumber.toString().padStart(2, '0')}"
        else -> null
    }

    val episodeName = episodeTitle
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

    val subtitle = listOfNotNull(
        episodePrefix,
        episodeName,
    )
        .distinct()
        .joinToString(" • ")
        .takeIf { it.isNotEmpty() }

    return AndroidNowPlayingText(
        title = contentTitle,
        subtitle = subtitle,
    )
}

private suspend fun loadArtworkBitmap(
    artworkUrl: String?,
    shape: AndroidNowPlayingArtworkShape,
): Bitmap? = withContext(Dispatchers.IO) {
    val url = artworkUrl?.takeIf { it.isNotBlank() } ?: return@withContext null
    runCatching {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            instanceFollowRedirects = true
        }
        val source = try {
            connection.inputStream.use(BitmapFactory::decodeStream)
        } finally {
            connection.disconnect()
        }
        source?.let { bitmap -> renderArtworkBitmap(bitmap, shape) }
    }.onFailure { error ->
        Log.w(TAG, "Unable to load Now Playing artwork", error)
    }.getOrNull()
}

private fun renderArtworkBitmap(
    source: Bitmap,
    shape: AndroidNowPlayingArtworkShape,
): Bitmap {
    if (source.width <= 0 || source.height <= 0) return source

    val targetWidth = shape.width
    val targetHeight = shape.height
    val targetAspect = targetWidth.toFloat() / targetHeight.toFloat()
    val sourceAspect = source.width.toFloat() / source.height.toFloat()

    val drawWidth: Float
    val drawHeight: Float
    if (sourceAspect > targetAspect) {
        drawHeight = targetHeight.toFloat()
        drawWidth = drawHeight * sourceAspect
    } else {
        drawWidth = targetWidth.toFloat()
        drawHeight = drawWidth / sourceAspect
    }

    val left = (targetWidth - drawWidth) / 2f
    val top = (targetHeight - drawHeight) / 2f
    val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    canvas.drawBitmap(source, null, RectF(left, top, left + drawWidth, top + drawHeight), paint)
    return output
}

private fun buildAndroidMediaMetadata(
    title: String,
    subtitle: String?,
    artwork: Bitmap?,
    durationMs: Long,
): MediaMetadata {
    val builder = MediaMetadata.Builder()
        .putString(MediaMetadata.METADATA_KEY_TITLE, title)
        .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, title)

    subtitle?.let { value ->
        builder.putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, value)
        builder.putString(MediaMetadata.METADATA_KEY_ARTIST, value)
    }

    if (durationMs > 0L) {
        builder.putLong(MediaMetadata.METADATA_KEY_DURATION, durationMs)
    }

    artwork?.let { bitmap ->
        builder.putBitmap(MediaMetadata.METADATA_KEY_ART, bitmap)
        builder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bitmap)
        builder.putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, bitmap)
    }

    return builder.build()
}

private fun buildAndroidPlaybackState(snapshot: PlayerPlaybackSnapshot): PlaybackState {
    val state = when {
        snapshot.isEnded -> PlaybackState.STATE_STOPPED
        snapshot.isPlaying -> PlaybackState.STATE_PLAYING
        else -> PlaybackState.STATE_PAUSED
    }

    return PlaybackState.Builder()
        .setActions(
            PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE or
                PlaybackState.ACTION_SEEK_TO or
                PlaybackState.ACTION_FAST_FORWARD or
                PlaybackState.ACTION_REWIND or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS,
        )
        .setState(
            state,
            snapshot.positionMs.coerceAtLeast(0L),
            snapshot.playbackSpeed.takeIf { it > 0f } ?: 1f,
        )
        .setBufferedPosition(snapshot.bufferedPositionMs.coerceAtLeast(0L))
        .build()
}

private object AndroidNowPlayingNotification {
    fun show(
        context: Context,
        mediaSession: MediaSession,
        title: String,
        subtitle: String?,
        artwork: Bitmap?,
        isPlaying: Boolean,
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                },
            )
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }

        val playPauseAction = if (isPlaying) {
            Notification.Action(
                android.R.drawable.ic_media_pause,
                "Pause",
                actionIntent(context, ACTION_PAUSE),
            )
        } else {
            Notification.Action(
                android.R.drawable.ic_media_play,
                "Play",
                actionIntent(context, ACTION_PLAY),
            )
        }

        val notification = builder
            .setSmallIcon(com.nuvio.app.R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setLargeIcon(artwork)
            .setShowWhen(false)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .addAction(
                Notification.Action(
                    android.R.drawable.ic_media_rew,
                    "Rewind 10s",
                    actionIntent(context, ACTION_REWIND),
                ),
            )
            .addAction(playPauseAction)
            .addAction(
                Notification.Action(
                    android.R.drawable.ic_media_ff,
                    "Forward 10s",
                    actionIntent(context, ACTION_FORWARD),
                ),
            )
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2),
            )
            .build()

        runCatching {
            notificationManager.notify(NOTIFICATION_ID, notification)
        }.onFailure { error ->
            Log.w(TAG, "Unable to show Now Playing notification", error)
        }
    }

    fun cancel(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun actionIntent(context: Context, action: String): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            Intent(context, AndroidNowPlayingActionReceiver::class.java).setAction(action),
            flags,
        )
    }
}

internal object AndroidNowPlayingActionRegistry {
    private var controllerProvider: (() -> PlayerEngineController?)? = null

    fun bind(provider: () -> PlayerEngineController?) {
        controllerProvider = provider
    }

    fun clear() {
        controllerProvider = null
    }

    fun handle(action: String?) {
        val controller = controllerProvider?.invoke() ?: return
        when (action) {
            ACTION_PLAY -> controller.play()
            ACTION_PAUSE -> controller.pause()
            ACTION_REWIND -> controller.seekBy(-SEEK_INTERVAL_MS)
            ACTION_FORWARD -> controller.seekBy(SEEK_INTERVAL_MS)
        }
    }
}

class AndroidNowPlayingActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AndroidNowPlayingActionRegistry.handle(intent.action)
    }
}
