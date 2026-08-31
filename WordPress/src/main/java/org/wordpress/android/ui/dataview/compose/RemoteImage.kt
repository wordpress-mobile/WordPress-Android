package org.wordpress.android.ui.dataview.compose

import android.content.Context
import android.util.TypedValue
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import android.content.res.Resources


@Composable
fun RemoteImage(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    fallbackImageRes: Int? = null,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center
) {
    if (imageUrl.isNullOrBlank()) {
        // No image and no fallback: render nothing (e.g. an inline body image with no placeholder).
        if (fallbackImageRes != null) {
            Image(
                painter = painterResource(id = fallbackImageRes),
                contentDescription = null,
                alignment = alignment,
                contentScale = contentScale,
                modifier = modifier
            )
        }
    } else if (imageUrl.startsWith("drawable:")) {
        // Handle drawable resource ID passed as string
        val resourceId = imageUrl.removePrefix("drawable:").toIntOrNull()
        val painterRes = if (resourceId != null && isValidDrawableId(LocalContext.current, resourceId)) {
            resourceId
        } else {
            fallbackImageRes
        }
        if (painterRes != null) {
            Image(
                painter = painterResource(id = painterRes),
                contentDescription = null,
                alignment = alignment,
                contentScale = contentScale,
                modifier = modifier
            )
        }
    } else {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .apply { fallbackImageRes?.let { error(it) } }
                .crossfade(true)
                .build(),
            contentDescription = null,
            alignment = alignment,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}

@Suppress("SwallowedException")
private fun isValidDrawableId(context: Context, drawableId: Int): Boolean = try {
    context.resources.getValue(drawableId, TypedValue(), true)
    true
} catch (e: Resources.NotFoundException) {
    false
}

