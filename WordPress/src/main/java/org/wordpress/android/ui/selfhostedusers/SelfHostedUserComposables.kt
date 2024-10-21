package org.wordpress.android.ui.selfhostedusers

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3

/**
 * These composables were created for the self-hosted users feature but were written to be reusable
 * in other projects.
 */

/**
 * A composable that displays an avatar image optionally with a click listener. This
 * is suitable for use in a list, such as in the self-hosted user list feature.
 */
@Composable
fun SmallAvatar(
    avatarUrl: String?,
    contentDescription: String? = null,
    onAvatarClick: ((String?) -> Unit)? = null,
) {
    val extraModifier = if (onAvatarClick != null) {
        Modifier.clickable {
            onAvatarClick(avatarUrl)
        }
    } else {
        Modifier
    }

    if (avatarUrl.isNullOrEmpty()) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_user_placeholder_primary_24),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .size(48.dp)
                .then(
                    extraModifier
                )
        )
    } else {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(avatarUrl)
                .error(R.drawable.ic_user_placeholder_primary_24)
                .crossfade(true)
                .build(),
            contentScale = ContentScale.Fit,
            contentDescription = contentDescription,
            modifier = Modifier
                .clip(CircleShape)
                .size(48.dp)
                .then(
                    extraModifier
                )
        )
    }
}

/**
 * A composable that displays an avatar image at the maximum screen size
 */
@Composable
fun LargeAvatar(avatarUrl: String) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(avatarUrl)
            .error(R.drawable.ic_user_placeholder_primary_24)
            .crossfade(true)
            .build(),
        contentScale = ContentScale.Fit,
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    )
}

/**
 * A composable that displays a message when there is no network connection
 */
@Composable
fun OfflineView(
    onRetryClick: (() -> Unit)? = null,
) {
    MessageView(
        imageRes = R.drawable.img_illustration_cloud_off_152dp,
        messageRes = R.string.no_network_message,
        buttonRes = R.string.retry,
        onButtonClick = onRetryClick
    )
}

/**
 * A composable that displays a message with an image above it and an optional button below it
 */
@Composable
fun MessageView(
    @DrawableRes imageRes: Int,
    @StringRes messageRes: Int,
    @StringRes buttonRes: Int? = null,
    onButtonClick: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(imageRes),
            tint = colorResource(R.color.neutral_30),
            contentDescription = null,
        )
        Text(
            text = stringResource(messageRes),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 16.dp),
        )
        if (buttonRes != null && onButtonClick != null) {
            Button(
                modifier = Modifier.padding(top = 16.dp),
                shape = RoundedCornerShape(2.dp),
                onClick = onButtonClick,
            ) {
                Text(
                    text = stringResource(R.string.retry).uppercase(),
                )
            }
        }
    }
}

/**
 * A composable that displays a screen with a top bar and a content area
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenWithTopBar(
    title: String,
    onCloseClick: () -> Unit,
    closeIcon: ImageVector = Icons.Default.Close,
    content: @Composable () -> Unit,
) {
    AppThemeM3 {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onCloseClick) {
                            Icon(closeIcon, stringResource(R.string.back))
                        }
                    },
                )
            },
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(contentPadding)
            ) {
                content()
            }
        }
    }
}

@Composable
@Preview(
    name = "Offline Screen Light Mode",
    showBackground = true
)
@Preview(
    name = "Offline Screen Dark Mode",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
private fun OfflineScreenPreview() {
    val content: @Composable () -> Unit = @Composable {
        OfflineView(
            onRetryClick = {}
        )
    }
    ScreenWithTopBar(
        title = "Title",
        content = content,
        onCloseClick = {},
    )
}
