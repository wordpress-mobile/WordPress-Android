package org.wordpress.android.ui.reader.views.compose.article

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.View
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.core.text.htmlEncode
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.reader.repository.awaitRestRequest

/**
 * Sent as the referrer for embedded players. YouTube refuses to play in an app's WebView unless the request
 * identifies the app this way.
 */
private val PLAYER_ORIGIN = "https://${BuildConfig.APPLICATION_ID}"

private const val POSTER_SCRIM_ALPHA = 0.55f
private const val PLAY_BUTTON_ALPHA = 0.6f
private const val HOST_LABEL_ALPHA = 0.9f

/**
 * A video shown as its poster with a play button. Tapping swaps in the player, so nothing heavy loads until the
 * reader asks for it. Scrolling it away, or the post stopping being [isActive] (the pager keeps neighbouring posts
 * alive), releases the player and stops playback. Ported from WPFolio.
 */
@Composable
internal fun ReaderArticleVideo(video: ReaderArticleBlock.Video, accent: Color, isActive: Boolean) {
    var isPlaying by remember(video) { mutableStateOf(false) }
    LaunchedEffect(isActive) { if (!isActive) isPlaying = false }
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .aspectRatio(video.aspectRatio)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black),
    ) {
        if (isPlaying) {
            // if the WebView's renderer dies, fall back to the poster rather than crash
            VideoPlayer(video, Modifier.fillMaxSize(), onRendererGone = { isPlaying = false })
        } else {
            VideoPoster(video, accent) { isPlaying = true }
        }
    }
}

@Composable
private fun VideoPoster(video: ReaderArticleBlock.Video, accent: Color, onPlay: () -> Unit) {
    // VideoPress embeds don't include a poster, but WordPress.com's video API does
    val poster by produceState(video.poster, video) {
        if (value == null && video.host == VideoHost.VIDEOPRESS) value = fetchVideoPressPoster(video.id)
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(accent)
            .clickable(onClick = onPlay)
    ) {
        AsyncImage(
            model = poster,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.6f to Color.Transparent,
                        1f to Color.Black.copy(alpha = POSTER_SCRIM_ALPHA)
                    )
                )
        )
        Box(
            Modifier
                .align(Alignment.Center)
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = PLAY_BUTTON_ALPHA)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.PlayArrow,
                stringResource(R.string.reader_article_play_video),
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
        Text(
            video.host.label,
            style = ui(12f, 600),
            color = Color.White.copy(alpha = HOST_LABEL_ALPHA),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
        )
    }
}

private suspend fun fetchVideoPressPoster(guid: String): String? =
    awaitRestRequest(cancelWithCaller = true) { listener, errorListener ->
        WordPress.getRestClientUtilsV1_1().getWithLocale("videos/$guid", listener, errorListener)
    }.response?.optString("poster")?.takeIf { it.startsWith("http") }

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun VideoPlayer(video: ReaderArticleBlock.Video, modifier: Modifier, onRendererGone: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var webView by remember { mutableStateOf<WebView?>(null) }
    val chrome = remember { FullscreenChromeClient() }

    // pause with the app, so audio doesn't carry on in the background
    DisposableEffect(lifecycle, webView) {
        val view = webView ?: return@DisposableEffect onDispose {}
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> view.onPause()
                Lifecycle.Event.ON_RESUME -> view.onResume()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                // fill the box; a wrap-content WebView gives the page zero height, hiding the player
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(android.graphics.Color.BLACK)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                webChromeClient = chrome
                webViewClient = PlayerClient(uriHandler, onRendererGone)
                loadDataWithBaseURL(PLAYER_ORIGIN, playerHtml(video), "text/html", "utf-8", null)
                webView = this
            }
        },
        // runs when the block scrolls away or the post is swiped past, which stops playback
        onRelease = {
            chrome.exitFullscreen()
            it.destroy()
        },
        modifier = modifier,
    )
}

/** A page holding just the player, filling the WebView */
private fun playerHtml(video: ReaderArticleBlock.Video): String {
    val player = when (video.host) {
        // IDs are restricted to URL-safe characters by ReaderVideoLinks, so they can go straight in
        VideoHost.YOUTUBE -> iframe(
            "https://www.youtube.com/embed/${video.id}?autoplay=1&playsinline=1&rel=0&origin=$PLAYER_ORIGIN"
        )
        VideoHost.VIMEO -> iframe("https://player.vimeo.com/video/${video.id}?autoplay=1")
        VideoHost.VIDEOPRESS -> iframe(
            "https://videopress.com/embed/${video.id}?autoPlay=1&controls=1&playsinline=1"
        )
        VideoHost.FILE -> {
            val poster = video.poster?.let { " poster=\"${it.htmlEncode()}\"" }.orEmpty()
            "<video src=\"${video.id.htmlEncode()}\"$poster controls autoplay playsinline></video>"
        }
    }
    return """
        <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
        <style>html,body{margin:0;height:100%;background:#000;overflow:hidden}
        iframe,video{position:fixed;inset:0;border:0;width:100%;height:100%}</style></head>
        <body>$player</body></html>
    """.trimIndent()
}

private fun iframe(src: String) =
    "<iframe src=\"$src\" allow=\"autoplay; fullscreen; encrypted-media; picture-in-picture\" allowfullscreen>" +
        "</iframe>"

/** Keeps the player in place; links out of it (such as "Watch on YouTube") open as the post's links do */
private class PlayerClient(
    private val uriHandler: UriHandler,
    private val onRendererGone: () -> Unit,
) : WebViewClient() {
    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        onRendererGone()
        return true
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        if (!request.isForMainFrame) return false
        uriHandler.openUri(request.url.toString())
        return true
    }
}

/** Shows the player's fullscreen view in a fullscreen dialog with the system bars hidden */
private class FullscreenChromeClient : WebChromeClient() {
    private var dialog: Dialog? = null

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        exitFullscreen()
        dialog = Dialog(view.context, android.R.style.Theme_Black_NoTitleBar_Fullscreen).apply {
            setContentView(view)
            setOnDismissListener {
                dialog = null
                callback.onCustomViewHidden()
            }
            show()
            window?.let { window ->
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    hide(WindowInsetsCompat.Type.systemBars())
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        }
    }

    override fun onHideCustomView() = exitFullscreen()

    fun exitFullscreen() {
        dialog?.let {
            it.setOnDismissListener(null)
            dialog = null
            it.dismiss()
        }
    }
}

/** Embedded content the app can't show natively (audio, maps, social posts), as a card that opens it */
@Composable
internal fun ReaderArticleEmbedCard(embed: ReaderArticleBlock.Embed) {
    val colors = articleColors
    val uriHandler = LocalUriHandler.current
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .clip(shape)
            .border(1.dp, colors.outline, shape)
            .clickable { uriHandler.openUri(embed.url) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                embed.title ?: stringResource(R.string.reader_article_embedded_content),
                style = ui(15f, 600),
                color = colors.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                embed.url.toUri().host.orEmpty().removePrefix("www."),
                style = ui(12.5f, 500),
                color = colors.textSecondary,
                maxLines = 1,
            )
        }
        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, tint = colors.textSecondary)
    }
}
