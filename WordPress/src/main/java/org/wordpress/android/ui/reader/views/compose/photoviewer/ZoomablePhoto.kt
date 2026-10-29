package org.wordpress.android.ui.reader.views.compose.photoviewer

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import coil.compose.AsyncImage
import org.wordpress.android.R
import kotlin.math.max
import kotlin.math.min

private const val MAX_SCALE = 4f
private const val DOUBLE_TAP_SCALE = 2.5f

/**
 * A full-screen photo that pinches, pans and double-taps to zoom. The low-res version shows while the high-res
 * one loads.
 */
@Composable
fun ZoomablePhoto(photo: ReaderPhoto, isCurrentPage: Boolean, onTap: () -> Unit) {
    val zoomState = remember { ZoomState() }
    var isLoResLoaded by remember { mutableStateOf(false) }
    var isHiResLoaded by remember { mutableStateOf(false) }
    var hasHiResFailed by remember { mutableStateOf(false) }

    // the pager keeps its neighbours composed, so a photo swiped away would otherwise stay zoomed
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) zoomState.reset()
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onDoubleTap = { zoomState.toggle(it, size) }
                    )
                }
                .pointerInput(Unit) { detectZoomAndPan(zoomState) }
                .graphicsLayer {
                    scaleX = zoomState.scale
                    scaleY = zoomState.scale
                    translationX = zoomState.offset.x
                    translationY = zoomState.offset.y
                }
        ) {
            if (!isHiResLoaded) {
                AsyncImage(
                    model = photo.loResUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    onSuccess = {
                        isLoResLoaded = true
                        zoomState.imageSize = it.painter.intrinsicSize
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            AsyncImage(
                model = photo.hiResUrl,
                contentDescription = stringResource(R.string.reader_photo_view_desc),
                contentScale = ContentScale.Fit,
                onSuccess = {
                    isHiResLoaded = true
                    zoomState.imageSize = it.painter.intrinsicSize
                },
                onError = { hasHiResFailed = true },
                modifier = Modifier.fillMaxSize()
            )
        }
        when {
            isHiResLoaded || isLoResLoaded -> Unit
            hasHiResFailed -> Text(
                text = stringResource(R.string.reader_toast_err_view_image),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            else -> CircularProgressIndicator()
        }
    }
}

/**
 * Zooms about the pinch centroid. A one-finger drag on an unzoomed photo, or one that starts by dragging a zoomed
 * photo past its edge, isn't consumed, so the pager pages instead.
 */
private suspend fun PointerInputScope.detectZoomAndPan(state: ZoomState) = awaitEachGesture {
    awaitFirstDown(requireUnconsumed = false)
    var hasPanned = false
    var isLeftToPager = false
    do {
        val event = awaitPointerEvent()
        val isPinch = event.changes.count { it.pressed } > 1
        val pan = event.calculatePan()
        val shouldHandle = when {
            isPinch -> true
            !state.isZoomed || isLeftToPager || pan == Offset.Zero -> false
            state.canPan(pan, size) -> true
            else -> {
                // the pager only takes a drag that starts at the edge - once this one has panned, it's already lost
                isLeftToPager = !hasPanned
                false
            }
        }
        val centroid = event.calculateCentroid()
        if (shouldHandle && centroid.isSpecified) {
            state.transform(event.calculateZoom(), centroid, pan, size)
            hasPanned = true
            event.changes.forEach { if (it.positionChanged()) it.consume() }
        }
    } while (event.changes.any { it.pressed })
}

@Stable
private class ZoomState {
    var scale by mutableFloatStateOf(1f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set

    /** the photo's intrinsic size, so panning stops at its edges rather than the screen's */
    var imageSize: Size = Size.Unspecified

    val isZoomed get() = scale > 1f

    fun transform(zoom: Float, centroid: Offset, pan: Offset, bounds: IntSize) {
        val newScale = (scale * zoom).coerceIn(1f, MAX_SCALE)
        // keeps the point under the centroid in place while scaling about the centre
        val appliedZoom = newScale / scale
        val center = Offset(bounds.width / 2f, bounds.height / 2f)
        offset = clamp(offset * appliedZoom + (centroid - center) * (1 - appliedZoom) + pan, newScale, bounds)
        scale = newScale
    }

    fun canPan(pan: Offset, bounds: IntSize) = clamp(offset + pan, scale, bounds) != offset

    fun toggle(tap: Offset, bounds: IntSize) {
        if (isZoomed) {
            reset()
        } else {
            transform(DOUBLE_TAP_SCALE, tap, Offset.Zero, bounds)
        }
    }

    fun reset() {
        scale = 1f
        offset = Offset.Zero
    }

    private fun clamp(offset: Offset, scale: Float, bounds: IntSize): Offset {
        val fitted = fittedSize(bounds)
        val maxX = max(0f, (fitted.width * scale - bounds.width) / 2)
        val maxY = max(0f, (fitted.height * scale - bounds.height) / 2)
        return Offset(offset.x.coerceIn(-maxX, maxX), offset.y.coerceIn(-maxY, maxY))
    }

    /** the size ContentScale.Fit draws the photo at */
    private fun fittedSize(bounds: IntSize): Size {
        val image = imageSize
        if (!image.isSpecified || image.width <= 0f || image.height <= 0f) {
            return Size(bounds.width.toFloat(), bounds.height.toFloat())
        }
        val fit = min(bounds.width / image.width, bounds.height / image.height)
        return Size(image.width * fit, image.height * fit)
    }
}
