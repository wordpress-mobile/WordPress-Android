package org.wordpress.android.ui.dataview.compose

import android.content.Context
import android.util.TypedValue
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import android.content.res.Resources


@Composable
fun RemoteImage(
    imageUrl: String?,
    fallbackImageRes: Int,
    modifier: Modifier = Modifier
) {
    if (imageUrl.isNullOrBlank()) {
        Image(
            painter = painterResource(id = fallbackImageRes),
            contentDescription = null,
            modifier = modifier
        )
    } else if (imageUrl.startsWith("drawable:")) {
        // Handle drawable resource ID passed as string
        val resourceId = imageUrl.removePrefix("drawable:").toIntOrNull()
        if (resourceId != null && isValidDrawableId(LocalContext.current, resourceId)) {
            Image(
                painter = painterResource(id = resourceId),
                contentDescription = null,
                modifier = modifier
            )
        } else {
            // Fallback if resource ID can't be parsed or is invalid
            Image(
                painter = painterResource(id = fallbackImageRes),
                contentDescription = null,
                modifier = modifier
            )
        }
    } else {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .error(fallbackImageRes)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = modifier
        )
    }
}

private fun isValidDrawableId(context: Context, drawableId: Int): Boolean = try {
    context.resources.getValue(drawableId, TypedValue(), true)
    true
} catch (e: Resources.NotFoundException) {
    false
}

