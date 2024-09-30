package org.wordpress.android.ui.selfhostedusers

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.ProgressDialog
import org.wordpress.android.ui.compose.components.ProgressDialogState
import uniffi.wp_api.UserWithEditContext

@Composable
fun SelfHostedUsersScreen(
    uiState: StateFlow<SelfHostedUsersViewModel.SelfHostedUserState>,
    onCloseClick: () -> Unit = {},
    onUserClick: (UserWithEditContext) -> Unit = {},
) {
    val state = uiState.collectAsState().value

    val title = when(state) {
        is SelfHostedUsersViewModel.SelfHostedUserState.UserDetail -> state.user.name
        else -> stringResource(R.string.users)
    }

    val closeIcon = when(state) {
        is SelfHostedUsersViewModel.SelfHostedUserState.UserDetail -> Icons.Default.Close
        else ->  Icons.AutoMirrored.Filled.ArrowBack
    }

    val content: @Composable () -> Unit = @Composable {
        when (state) {
            is SelfHostedUsersViewModel.SelfHostedUserState.Loading -> {
                ProgressDialog(
                    ProgressDialogState(
                        message = R.string.loading,
                        showCancel = false,
                        dismissible = false
                    )
                )
            }

            is SelfHostedUsersViewModel.SelfHostedUserState.UserList -> {
                if (state.users.isNotEmpty()) {
                    UserList(state.users, onUserClick)
                } else {
                    UserEmptyView(stringResource(R.string.no_users))
                }
            }

            is SelfHostedUsersViewModel.SelfHostedUserState.UserDetail -> {
                UserDetail(state.user)
            }

            is SelfHostedUsersViewModel.SelfHostedUserState.Offline -> {
                // TODO
            }
        }
    }

    UserScreen(
        content = content,
        title = title,
        closeIcon = closeIcon,
        onCloseClick = { onCloseClick() },
        isScrollable = state is SelfHostedUsersViewModel.SelfHostedUserState.UserList
    )
}

@Composable
private fun UserList(
    users: List<UserWithEditContext>,
    onUserClick: (UserWithEditContext) -> Unit
) {
    for (user in users) {
        UserLazyRow(user, onUserClick)
        HorizontalDivider(thickness = 1.dp, modifier = Modifier.padding(start = 80.dp))
    }
}

@Composable
private fun UserLazyRow(
    user: UserWithEditContext,
    onUserClick: (UserWithEditContext) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .padding(all = 16.dp)
            .fillMaxWidth()
            .clickable { onUserClick(user) }
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
private fun UserDetail(
    user: UserWithEditContext,
) {
    Row(
        modifier = Modifier
            .padding(all = 16.dp)
            .fillMaxWidth()
    ) {
        Column {
            val avatarUrl = user.avatarUrls?.values?.firstOrNull() ?: ""
            UserAvatar(avatarUrl)
        }

        Column(
            modifier = Modifier
                .padding(start = 16.dp)
        ) {
            UserDetailSection(title = stringResource(R.string.name)) {
                UserDetailRow(
                    label = stringResource(R.string.username),
                    text = user.username,
                )
                UserDetailRow(
                    label = stringResource(R.string.role),
                    text = user.roles.joinToString(),
                )
                UserDetailRow(
                    label = stringResource(R.string.first_name),
                    text = user.firstName,
                )
                UserDetailRow(
                    label = stringResource(R.string.last_name),
                    text = user.lastName,
                )
                UserDetailRow(
                    label = stringResource(R.string.nickname),
                    text = user.nickname,
                )
                // TODO display name is missing from the model
            }

            UserDetailSection(title = stringResource(R.string.contact_info)) {
                UserDetailRow(
                    label = stringResource(R.string.email),
                    text = user.email,
                )
                UserDetailRow(
                    label = stringResource(R.string.website),
                    text = user.url,
                )
            }

            UserDetailSection(title = stringResource(R.string.about_the_user)) {
                UserDetailRow(
                    label = stringResource(R.string.biographical_info),
                    text = user.description,
                    isMultiline = true
                )
            }
        }
    }
}

@Composable
private fun UserDetailSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
    )
    Spacer(modifier = Modifier.height(16.dp))
    content()
    HorizontalDivider(thickness = 1.dp)
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun UserDetailRow(
    label: String,
    text: String,
    isMultiline: Boolean = false,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
    )
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = if (isMultiline) 10 else 1,
        overflow = TextOverflow.Ellipsis
    )
    Spacer(modifier = Modifier.height(16.dp))
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
    val uiState = SelfHostedUsersViewModel.SelfHostedUserState.UserList(SampleUsers.getSampleUsers())
    SelfHostedUsersScreen(MutableStateFlow(uiState))
}

@Composable
@Preview(
    name = "Detail Light Mode",
    showBackground = true
)
@Preview(
    name = "Detail Dark Mode",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
fun UserDetailScreenPreview() {
    val uiState = SelfHostedUsersViewModel.SelfHostedUserState.UserDetail(SampleUsers.getSampleUsers().first())
    SelfHostedUsersScreen(MutableStateFlow(uiState))
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
    val uiState = SelfHostedUsersViewModel.SelfHostedUserState.UserList(emptyList())
    SelfHostedUsersScreen(MutableStateFlow(uiState))
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
    val uiState = SelfHostedUsersViewModel.SelfHostedUserState.Loading
    SelfHostedUsersScreen(MutableStateFlow(uiState))
}
