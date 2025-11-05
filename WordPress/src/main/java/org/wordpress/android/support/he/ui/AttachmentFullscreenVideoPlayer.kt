package org.wordpress.android.support.he.ui

import android.view.ViewGroup
import androidx.core.net.toUri
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
import androidx.compose.runtime.DisposableEffect
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
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView
import org.wordpress.android.R
import org.wordpress.android.support.he.util.VideoUrlResolver

@Composable
fun AttachmentFullscreenVideoPlayer(
    videoUrl: String,
    onDismiss: () -> Unit,
    onDownload: () -> Unit = {},
    videoUrlResolver: VideoUrlResolver? = null
) {
    val context = LocalContext.current
    var hasError by remember { mutableStateOf(false) }
    var resolvedUrl by remember { mutableStateOf<String?>(null) }
    var isResolving by remember { mutableStateOf(true) }

    // Resolve URL redirects before playing
    androidx.compose.runtime.LaunchedEffect(videoUrl) {
        if (videoUrlResolver != null) {
            resolvedUrl = videoUrlResolver.resolveUrl(videoUrl)
        } else {
            resolvedUrl = videoUrl
        }
        isResolving = false
    }

    val exoPlayer = remember(resolvedUrl) {
        // Don't create player until URL is resolved
        val url = resolvedUrl ?: return@remember null

        SimpleExoPlayer.Builder(context).build().apply {
            // Add error listener
            addListener(object : Player.EventListener {
                override fun onPlayerError(error: com.google.android.exoplayer2.ExoPlaybackException) {
                    hasError = true
                }
            })

            // Simple configuration using MediaItem
            val mediaItem = MediaItem.fromUri(url.toUri())
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer?.stop()
            exoPlayer?.release()
        }
    }

    Dialog(
        onDismissRequest = {
            exoPlayer?.stop()
            onDismiss()
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
            when {
                isResolving -> {
                    // Show loading indicator while resolving URL
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White
                    )
                }
                hasError -> {
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
                                exoPlayer?.stop()
                                onDownload()
                                onDismiss()
                            }
                        ) {
                            Text(stringResource(R.string.he_support_download_video_button))
                        }
                    }
                }
                else -> {
                    // Show video player when URL is resolved and no error
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
                        exoPlayer?.stop()
                        onDownload.invoke()
                        onDismiss.invoke()
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
                        exoPlayer?.stop()
                        onDismiss()
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
