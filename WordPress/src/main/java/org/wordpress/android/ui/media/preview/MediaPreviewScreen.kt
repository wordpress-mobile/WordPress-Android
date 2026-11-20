package org.wordpress.android.ui.media.preview

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.google.android.exoplayer2.source.MediaSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.LoadingState

private const val TOOLBAR_FADE_DELAY_MS = 3000L
private const val TOOLBAR_ALPHA = 0.4f

/** a page to preview: a local media ID, and/or the URI to show when there's no media model for it */
data class MediaPreviewSource(val mediaId: Int, val fallbackUri: String?)

enum class MediaPreviewKind { IMAGE, VIDEO, AUDIO }

data class MediaPreviewItem(val uri: String, val kind: MediaPreviewKind, val title: String?)

/**
 * Full-screen media that pages horizontally when there's more than one item. The toolbar's visibility depends on
 * the current item: an image hides it after a few seconds and brings it back on a tap or a page change, a video
 * keeps it in step with the player's controls (the player swallows taps, so an auto-hidden back arrow could never
 * be brought back), and audio keeps it up since its controls never hide either.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPreviewScreen(
    sources: List<MediaPreviewSource>,
    initialPage: Int,
    resolve: suspend (MediaPreviewSource) -> MediaPreviewItem,
    buildMediaSource: (Uri) -> MediaSource?,
    onPageChanged: (Int) -> Unit,
    onBack: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialPage) { sources.size }
    val items = remember { mutableStateMapOf<Int, MediaPreviewItem>() }
    var isToolbarVisible by remember { mutableStateOf(true) }
    var toolbarShowCount by remember { mutableIntStateOf(0) }

    fun showToolbar() {
        isToolbarVisible = true
        toolbarShowCount++
    }

    // the settled page rather than currentPage, which changes mid-drag, so a drag that's pulled back doesn't count
    // as a page change. Derived so the screen only recomposes when the kind changes, not on every swipe.
    val currentKind by remember { derivedStateOf { items[pagerState.settledPage]?.kind } }
    LaunchedEffect(toolbarShowCount, currentKind) {
        if (currentKind == MediaPreviewKind.IMAGE) {
            delay(TOOLBAR_FADE_DELAY_MS)
            isToolbarVisible = false
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.drop(1).collect { page ->
            showToolbar()
            onPageChanged(page)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // keeps a neighbour loaded either side
        HorizontalPager(state = pagerState, beyondViewportPageCount = 1, modifier = Modifier.fillMaxSize()) { page ->
            LaunchedEffect(page) {
                if (page !in items) items[page] = resolve(sources[page])
            }
            val item = items[page]
            if (item == null) {
                LoadingState()
            } else {
                MediaPreviewPage(
                    item = item,
                    isCurrentPage = pagerState.settledPage == page,
                    // only the item the user tapped plays automatically
                    autoPlay = page == initialPage,
                    buildMediaSource = buildMediaSource,
                    onImageTap = ::showToolbar,
                    onVideoControlsVisibilityChanged = { isToolbarVisible = it }
                )
            }
        }

        AnimatedVisibility(
            visible = isToolbarVisible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it }
        ) {
            TopAppBar(
                title = {
                    if (sources.size > 1) {
                        Text(stringResource(R.string.media_preview_title, pagerState.currentPage + 1, sources.size))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = TOOLBAR_ALPHA),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    }
}
