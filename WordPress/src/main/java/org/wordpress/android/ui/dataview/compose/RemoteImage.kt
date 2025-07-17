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
    } else if (imageUrl.startsWith("android.resource://")) {
        // Handle local drawable resources
        val resourceId = extractResourceId(imageUrl)
        if (resourceId != null) {
            Image(
                painter = painterResource(id = resourceId),
                contentDescription = null,
                modifier = modifier
            )
        } else {
            // Fallback if resource can't be found
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

private fun extractResourceId(resourceUrl: String): Int? {
    return try {
        val resourceName = resourceUrl.substringAfterLast("/")
        when (resourceName) {
            "dummy_profile_1" -> org.wordpress.android.R.drawable.dummy_profile_1
            "dummy_profile_2" -> org.wordpress.android.R.drawable.dummy_profile_2
            "dummy_profile_3" -> org.wordpress.android.R.drawable.dummy_profile_3
            "dummy_profile_4" -> org.wordpress.android.R.drawable.dummy_profile_4
            "dummy_profile_5" -> org.wordpress.android.R.drawable.dummy_profile_5
            "dummy_profile_6" -> org.wordpress.android.R.drawable.dummy_profile_6
            "dummy_profile_7" -> org.wordpress.android.R.drawable.dummy_profile_7
            "dummy_profile_8" -> org.wordpress.android.R.drawable.dummy_profile_8
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}
