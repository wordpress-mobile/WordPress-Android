package org.wordpress.android.ui.selfhostedusers

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.flow.MutableStateFlow
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.ProgressDialog
import org.wordpress.android.ui.compose.components.ProgressDialogState
import org.wordpress.android.ui.compose.theme.M3Theme
import uniffi.wp_api.UserWithEditContext

@Composable
fun UserListScreen(
    users: State<List<UserWithEditContext>>,
    progressDialogState: State<ProgressDialogState?>?,
    onCloseClick: () -> Unit = {},
    onUserClick: (user: UserWithEditContext) -> Unit = { }
) {
    val content: @Composable () -> Unit = @Composable {
        progressDialogState?.value?.let {
            ProgressDialog(it)
        } ?: run {
            if (users.value.isNotEmpty()) {
                UserList(users.value, onUserClick)
            } else {
                EmptyView()
            }
        }
    }
    Screen(
        content = content,
        onCloseClick = { onCloseClick() },
        isScrollable = users.value.isNotEmpty()
    )
}

@Composable
private fun UserList(
    users: List<UserWithEditContext>,
    onUserClick: (user: UserWithEditContext) -> Unit,
) {
    for (user in users) {
        UserLazyRow(user, onUserClick)
        HorizontalDivider(thickness = 1.dp, modifier = Modifier.padding(start = 80.dp))
    }
}

@Composable
private fun UserLazyRow(
    user: UserWithEditContext,
    onUserClick: (user: UserWithEditContext) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .padding(all = 16.dp)
            .fillMaxWidth()
            .clickable {
                onUserClick(user)
            }
    ) {
        item {
            val avatarUrl = user.avatarUrls?.values?.firstOrNull() ?: ""
            UserAvatar(avatarUrl)
        }

        item {
            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (user.roles.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = user.roles.joinToString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

@Composable
private fun UserAvatar(avatarUrl: String?) {
    if (avatarUrl.isNullOrEmpty()) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_user_placeholder_primary_24),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .size(48.dp)
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
                .size(48.dp),
            )
    }
}

@Composable
private fun EmptyView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_people_white_24dp),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .size(85.dp)
        )
        Text(
            text = stringResource(id = R.string.no_users),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Screen(
    content: @Composable () -> Unit,
    onCloseClick: () -> Unit,
    isScrollable: Boolean,
) {
    M3Theme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(id = R.string.users)) },
                    navigationIcon = {
                        IconButton(onClick = onCloseClick) {
                            Icon(Icons.Filled.Close, stringResource(R.string.close))
                        }
                    },
                )
            },
        ) { contentPadding ->
            Column(
                modifier = if (isScrollable) {
                    Modifier
                        .fillMaxSize()
                        .imePadding()
                        .padding(contentPadding)
                        .verticalScroll(rememberScrollState())
                } else {
                    Modifier
                        .fillMaxSize()
                        .imePadding()
                        .padding(contentPadding)
                }
            ) {
                content()
            }
        }
    }
}

@Composable
@Preview(
    name = "Light Mode",
    showBackground = true
)
@Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
fun UserListScreenPreview() {
    UserListScreen(
        users = MutableStateFlow(SampleUsers.getSampleUsers()).collectAsState(),
        progressDialogState = null,
    )
}

@Composable
@Preview(
    name = "Empty View Light Mode",
    showBackground = true
)
@Preview(
    name = "Empty View Dark Mode",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
fun EmptyUserListScreenPreview() {
    UserListScreen(
        users = MutableStateFlow(emptyList<UserWithEditContext>()).collectAsState(),
        progressDialogState = null,
    )
}

@Composable
@Preview(
    name = "Progress Light Mode",
    showBackground = true
)
@Preview(
    name = "Progress Dark Mode",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
fun ProgressPreview() {
    val progressDialogState = ProgressDialogState(
        message = R.string.loading,
        showCancel = false,
        progress = 50f / 100f,
    )
    UserListScreen(
        users = MutableStateFlow(emptyList<UserWithEditContext>()).collectAsState(),
        progressDialogState = MutableStateFlow(progressDialogState).collectAsState(),
    )
}
