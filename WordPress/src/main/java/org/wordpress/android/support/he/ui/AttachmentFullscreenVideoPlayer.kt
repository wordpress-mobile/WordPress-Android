package org.wordpress.android.support.he.ui

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView
import org.wordpress.android.R
import org.wordpress.android.support.he.model.VideoDownloadState
import org.wordpress.android.util.AppLog
import java.io.File

@Composable
fun AttachmentFullscreenVideoPlayer(
    videoUrl: String,
    onDismiss: () -> Unit,
    onDownload: () -> Unit = {},
    downloadState: VideoDownloadState,
    onStartVideoDownload: (String) -> Unit,
    onResetVideoDownloadState: () -> Unit = {},
) {
    val context = LocalContext.current
    var localVideoFile by remember { mutableStateOf<File?>(null) }

    // Start download when composable is first launched
    LaunchedEffect(videoUrl) {
        onStartVideoDownload(videoUrl)
    }

    // Update local file when download succeeds
    LaunchedEffect(downloadState) {
        if (downloadState is VideoDownloadState.Success) {
            localVideoFile = downloadState.file
        }
    }

    val exoPlayer = remember(localVideoFile) {
        // Don't create player until video is downloaded
        val file = localVideoFile ?: return@remember null

        SimpleExoPlayer.Builder(context).build().apply {
            // Add error listener for logging
            addListener(object : Player.EventListener {
                override fun onPlayerError(error: com.google.android.exoplayer2.ExoPlaybackException) {
                    AppLog.e(AppLog.T.SUPPORT, "Video playback error", error)
                }
            })

            // Play from local file
            val mediaItem = MediaItem.fromUri(file.toUri())
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    Dialog(
        onDismissRequest = {
            closeFullScreen(exoPlayer, onDismiss, onResetVideoDownloadState)
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            when (downloadState) {
                is VideoDownloadState.Idle,
                is VideoDownloadState.Downloading -> {
                    // Show loading indicator while downloading video
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White
                    )
                }
                is VideoDownloadState.Error -> {
                    // Show error message when video fails to load
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = stringResource(R.string.he_support_video_playback_error_title),
                            color = Color.White,
                            style = androidx.compose.material3.MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = stringResource(R.string.he_support_video_playback_error_message),
                            color = Color.White.copy(alpha = 0.7f),
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = {
                                onDownload()
                                closeFullScreen(exoPlayer, onDismiss, onResetVideoDownloadState)
                            }
                        ) {
                            Text(stringResource(R.string.he_support_download_video_button))
                        }
                    }
                }
                is VideoDownloadState.Success -> {
                    // Show video player when video is downloaded successfully
                    exoPlayer?.let { player ->
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    this.player = player
                                    useController = true
                                    layoutParams = FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Top bar with close and download buttons
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Download button
                IconButton(
                    onClick = {
                        onDownload()
                        closeFullScreen(exoPlayer, onDismiss, onResetVideoDownloadState)
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_get_app_white_24dp),
                        contentDescription = stringResource(R.string.he_support_download_attachment),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Close button
                IconButton(
                    onClick = {
                        closeFullScreen(exoPlayer, onDismiss, onResetVideoDownloadState)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

fun closeFullScreen(
    exoPlayer: SimpleExoPlayer?,
    onDismiss: () -> Unit,
    onResetVideoDownloadState: () -> Unit,
) {
    exoPlayer?.stop()
    exoPlayer?.release()
    onDismiss()
    onResetVideoDownloadState()
}
