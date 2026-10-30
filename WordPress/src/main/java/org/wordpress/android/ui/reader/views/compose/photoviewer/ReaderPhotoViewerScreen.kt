package org.wordpress.android.ui.reader.views.compose.photoviewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import org.wordpress.android.R

private const val TOOLBAR_FADE_DELAY_MS = 3000L
private const val TRANSLUCENT_ALPHA = 0.5f

data class ReaderPhoto(val loResUrl: String, val hiResUrl: String)

/**
 * Full-screen photos that page horizontally. The toolbar hides itself after a few seconds and comes back on a tap
 * or a page change; a tap also toggles the "1 of 5" count.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderPhotoViewerScreen(photos: List<ReaderPhoto>, initialPage: Int, onBack: () -> Unit) {
    val pagerState = rememberPagerState(initialPage = initialPage) { photos.size }
    var toolbarShowCount by remember { mutableIntStateOf(0) }
    var isToolbarVisible by remember { mutableStateOf(true) }
    var isCountVisible by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(toolbarShowCount) {
        isToolbarVisible = true
        delay(TOOLBAR_FADE_DELAY_MS)
        isToolbarVisible = false
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.drop(1).collect { toolbarShowCount++ }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // keeps a neighbour loaded either side, as the old ViewPager did
        HorizontalPager(state = pagerState, beyondViewportPageCount = 1, modifier = Modifier.fillMaxSize()) { page ->
            ZoomablePhoto(
                photo = photos[page],
                isCurrentPage = pagerState.settledPage == page,
                onTap = {
                    isCountVisible = !isCountVisible
                    toolbarShowCount++
                }
            )
        }

        if (photos.size > 1) {
            AnimatedVisibility(
                visible = isCountVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.reader_title_photo_viewer, pagerState.currentPage + 1, photos.size),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = TRANSLUCENT_ALPHA), RoundedCornerShape(36.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = isToolbarVisible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it }
        ) {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(
                    Brush.verticalGradient(listOf(Color.Black.copy(alpha = TRANSLUCENT_ALPHA), Color.Transparent))
                )
            )
        }
    }
}
