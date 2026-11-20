package org.wordpress.android.ui.media.preview

import android.content.Context
import android.net.Uri
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleStartEffect
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.ui.PlayerView
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.ZoomablePhoto
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T

@Composable
fun MediaPreviewPage(
    item: MediaPreviewItem,
    isCurrentPage: Boolean,
    autoPlay: Boolean,
    buildMediaSource: (Uri) -> MediaSource?,
    onImageTap: () -> Unit,
    onVideoControlsVisibilityChanged: (Boolean) -> Unit
) {
    when (item.kind) {
        MediaPreviewKind.IMAGE -> ZoomablePhoto(
            model = item.uri.ifEmpty { null },
            contentDescription = stringResource(R.string.media_preview_desc),
            errorText = stringResource(R.string.error_media_load),
            isCurrentPage = isCurrentPage,
            onTap = onImageTap
        )
        MediaPreviewKind.VIDEO,
        MediaPreviewKind.AUDIO -> MediaPlayerPage(
            item = item,
            isCurrentPage = isCurrentPage,
            autoPlay = autoPlay,
            buildMediaSource = buildMediaSource,
            onVideoControlsVisibilityChanged = onVideoControlsVisibilityChanged
        )
    }
}

/**
 * Plays a video or an audio file. A page has a prepared player while it's composed (the current page or a
 * neighbour) and the screen is started, so a neighbouring video already shows its first frame when the user swipes
 * to it. Only the current page plays, and the position is kept for when the player is rebuilt.
 */
@Composable
private fun MediaPlayerPage(
    item: MediaPreviewItem,
    isCurrentPage: Boolean,
    autoPlay: Boolean,
    buildMediaSource: (Uri) -> MediaSource?,
    onVideoControlsVisibilityChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var position by rememberSaveable { mutableLongStateOf(0L) }
    var playWhenReady by rememberSaveable { mutableStateOf(autoPlay) }
    var player by remember { mutableStateOf<SimpleExoPlayer?>(null) }
    var isBuffering by remember { mutableStateOf(false) }
    val currentIsCurrentPage by rememberUpdatedState(isCurrentPage)

    LifecycleStartEffect(Unit) {
        val mediaSource = buildMediaSource(item.uri.toUri())
        val shouldPlay = playWhenReady && currentIsCurrentPage
        player = createPlayer(context, mediaSource, position, shouldPlay) { isBuffering = it }
        onStopOrDispose {
            player?.let {
                position = it.currentPosition
                playWhenReady = it.playWhenReady
                it.release()
            }
            player = null
            isBuffering = false
        }
    }

    // swiping away pauses, and swiping back doesn't resume playback on its own
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            player?.playWhenReady = false
            playWhenReady = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (item.kind == MediaPreviewKind.VIDEO) {
            VideoPlayer(player, isCurrentPage, onVideoControlsVisibilityChanged)
        } else {
            AudioPlayer(player, isCurrentPage, item.title)
        }
        if (isBuffering) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun VideoPlayer(
    player: SimpleExoPlayer?,
    isCurrentPage: Boolean,
    onControlsVisibilityChanged: (Boolean) -> Unit
) {
    // only the page the user is actually looking at may drive the toolbar
    val currentIsCurrentPage by rememberUpdatedState(isCurrentPage)
    val currentOnControlsVisibilityChanged by rememberUpdatedState(onControlsVisibilityChanged)
    var playerView by remember { mutableStateOf<PlayerView?>(null) }

    // when this page comes to the front or its player is rebuilt, sync the toolbar with the controls (they may
    // already be showing, which fires no change) and focus the player so a keyboard or D-pad controls playback
    LaunchedEffect(isCurrentPage, player) {
        val view = playerView
        if (isCurrentPage && player != null && view != null) {
            currentOnControlsVisibilityChanged(view.isControllerVisible)
            view.requestFocus()
        }
    }

    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                // the listener must be registered before the player is set, otherwise setPlayer() shows the
                // controls before anybody is listening and the toolbar is left out of sync
                setControllerVisibilityListener { visibility ->
                    if (currentIsCurrentPage) currentOnControlsVisibilityChanged(visibility == View.VISIBLE)
                }
                playerView = this
            }
        },
        update = { it.player = player },
        onRelease = { it.player = null },
        // keeps the playback controls clear of the navigation bar
        modifier = Modifier.fillMaxSize().navigationBarsPadding()
    )
}

@Composable
private fun BoxScope.AudioPlayer(player: SimpleExoPlayer?, isCurrentPage: Boolean, title: String?) {
    var controlView by remember { mutableStateOf<PlayerControlView?>(null) }

    // focus the controls so a keyboard or D-pad controls playback
    LaunchedEffect(isCurrentPage, player) {
        if (isCurrentPage && player != null) controlView?.requestFocus()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.align(Alignment.Center).padding(16.dp)
    ) {
        if (!title.isNullOrEmpty()) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_audio_white_24dp),
            contentDescription = stringResource(R.string.media_preview_audio_desc),
            tint = colorResource(R.color.neutral_30),
            modifier = Modifier.size(128.dp)
        )
    }
    AndroidView(
        factory = { context ->
            PlayerControlView(context).apply {
                showTimeoutMs = 0
                controlView = this
            }
        },
        update = { it.player = player },
        onRelease = { it.player = null },
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .navigationBarsPadding()
    )
}

private fun createPlayer(
    context: Context,
    mediaSource: MediaSource?,
    position: Long,
    playWhenReady: Boolean,
    onBufferingChanged: (Boolean) -> Unit
) = SimpleExoPlayer.Builder(context).build().apply {
    addListener(object : Player.EventListener {
        override fun onPlaybackStateChanged(state: Int) {
            onBufferingChanged(state == Player.STATE_BUFFERING)
        }

        override fun onPlayerError(error: ExoPlaybackException) {
            AppLog.e(T.MEDIA, "Media preview playback failed", error)
        }
    })
    mediaSource?.let { setMediaSource(it, position) }
    this.playWhenReady = playWhenReady
    prepare()
}
