package org.wordpress.android.support.he.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import org.wordpress.android.R
import org.wordpress.android.support.he.ui.HESupportActivity.Companion.AUTHORIZATION_TAG
import org.wordpress.android.ui.compose.theme.AppThemeM3

@Composable
fun AttachmentFullscreenImagePreview(
    imageUrl: String,
    onGetAuthorizationHeaderArgument: () -> String,
    onDismiss: () -> Unit,
    onDownload: () -> Unit = {},
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Cache authorization header to avoid repeated function calls during composition
    val authorizationHeader = remember { onGetAuthorizationHeaderArgument() }

    // Load semantics
    val loadingImageDescription = stringResource(R.string.he_support_loading_image)
    val attachmentImageDescription = stringResource(R.string.he_support_attachment_image)
    val failedToLoadImageDescription = stringResource(R.string.he_support_failed_to_load_image)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onDismiss),
            color = Color.Black
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .semantics {
                            contentDescription = loadingImageDescription
                        }
                )
                // Zoomable image
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .apply {
                                addHeader(AUTHORIZATION_TAG, authorizationHeader)
                            }
                            .build(),
                        contentDescription = attachmentImageDescription,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val previousScale = scale
                                    scale = (scale * zoom).coerceIn(1f, 5f)

                                    if (scale > 1f) {
                                        // Calculate max pan bounds to prevent image from going off-screen
                                        val maxOffsetX = (size.width * (scale - 1f)) / 2f
                                        val maxOffsetY = (size.height * (scale - 1f)) / 2f

                                        offsetX = (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                                        offsetY = (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                    } else if (previousScale > 1f && scale == 1f) {
                                        // Only reset when transitioning from zoomed to unzoomed
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }
                            },
                        contentScale = ContentScale.Fit,
                        error = {
                            Icon(
                                painter = painterResource(R.drawable.ic_image_white_24dp),
                                contentDescription = failedToLoadImageDescription,
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    )
                }

                // Top bar with close button
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
                        onClick = onDismiss
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
}

@Preview(showBackground = true, name = "Fullscreen Image Preview")
@Composable
private fun AttachmentFullscreenImagePreviewPreview() {
    AppThemeM3(isDarkTheme = false) {
        AttachmentFullscreenImagePreview(
            imageUrl = "https://via.placeholder.com/800x600",
            onGetAuthorizationHeaderArgument = { "" },
            onDismiss = { },
            onDownload = { }
        )
    }
}

@Preview(showBackground = true, name = "Fullscreen Image Preview - Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AttachmentFullscreenImagePreviewPreviewDark() {
    AppThemeM3(isDarkTheme = true) {
        AttachmentFullscreenImagePreview(
            imageUrl = "https://via.placeholder.com/800x600",
            onGetAuthorizationHeaderArgument = { "" },
            onDismiss = { },
            onDownload = { }
        )
    }
}
