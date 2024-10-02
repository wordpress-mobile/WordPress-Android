package org.wordpress.android.ui.selfhostedusers

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.M3Theme

/**
 * These composables were created for the self-hosted users feature but were written to be reusable
 * in other projects.
 */
@Composable
fun UserAvatar(
    avatarUrl: String?,
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
            contentDescription = null,
            modifier = Modifier
                .clip(CircleShape)
                .size(48.dp)
                .then(
                    extraModifier
                )
        )
    }
}

@Composable
fun UserLargeAvatar(avatarUrl: String) {
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

@Composable
fun UserEmptyView() {
    UserMessageView(
        R.drawable.ic_people_white_24dp,
        R.string.no_users,
    )
}

@Composable
fun UserOfflineView() {
    UserMessageView(
        R.drawable.ic_wifi_off_24px,
        R.string.no_network_title,
        R.string.no_network_message,
    )
}

@Composable
private fun UserMessageView(
    @DrawableRes iconRes: Int,
    @StringRes titleRes: Int,
    @StringRes messageRes: Int? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .size(85.dp)
        )
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (messageRes != null) {
            Text(
                text = stringResource(messageRes),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreen(
    title: String,
    content: @Composable () -> Unit,
    closeIcon: ImageVector,
    onCloseClick: () -> Unit,
    isScrollable: Boolean,
) {
    M3Theme {
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
            val extraModifier = if (isScrollable) {
                Modifier
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
            } else {
                Modifier
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .then(extraModifier)
            ) {
                content()
            }
        }
    }
}
