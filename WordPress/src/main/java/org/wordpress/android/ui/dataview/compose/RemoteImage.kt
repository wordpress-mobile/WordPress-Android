package org.wordpress.android.ui.dataview.compose

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest

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
        if (resourceId != null) {
            Image(
                painter = painterResource(id = resourceId),
                contentDescription = null,
                modifier = modifier
            )
        } else {
            // Fallback if resource ID can't be parsed
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
