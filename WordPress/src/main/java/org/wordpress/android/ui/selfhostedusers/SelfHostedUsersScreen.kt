package org.wordpress.android.ui.selfhostedusers

import android.content.res.Configuration
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.ProgressDialog
import org.wordpress.android.ui.compose.components.ProgressDialogState
import org.wordpress.android.ui.selfhostedusers.SelfHostedUsersViewModel.SelfHostedUserState
import uniffi.wp_api.UserWithEditContext

@Composable
fun SelfHostedUsersScreen(
    uiState: StateFlow<SelfHostedUserState>,
    onCloseClick: () -> Unit = {},
    onUserClick: (UserWithEditContext) -> Unit = {},
    onUserAvatarClick: (avatarUrl: String?) -> Unit = {},
) {
    val state = uiState.collectAsState().value

    val title = when (state) {
        is SelfHostedUserState.UserDetail -> state.user.name
        is SelfHostedUserState.UserAvatar -> ""
        else -> stringResource(R.string.users)
    }

    val closeIcon = when (state) {
        is SelfHostedUserState.UserAvatar -> Icons.Default.Close
        else -> Icons.AutoMirrored.Filled.ArrowBack
    }

    val isScrollable = when (state) {
        is SelfHostedUserState.UserList -> true
        is SelfHostedUserState.UserDetail -> true
        else -> false
    }

    Crossfade(
        targetState = state,
        animationSpec = tween(
            durationMillis = integerResource(android.R.integer.config_mediumAnimTime)
        ),
        label = "Crossfade"
    ) { targetState ->
        ScreenWithTopBar(
            title = title,
            onCloseClick = { onCloseClick() },
            isScrollable = isScrollable,
            closeIcon = closeIcon,
        ) {
            when (targetState) {
                is SelfHostedUserState.Loading -> {
                    ProgressDialog(
                        ProgressDialogState(
                            message = R.string.loading,
                            showCancel = false,
                            dismissible = false
                        )
                    )
                }

                is SelfHostedUserState.UserList -> {
                    UserList(targetState.users, onUserClick)
                }

                is SelfHostedUserState.EmptyUserList -> {
                    MessageView(
                        R.drawable.ic_people_white_24dp,
                        R.string.no_users,
                    )
                }

                is SelfHostedUserState.UserAvatar -> {
                    LargeAvatar(targetState.avatarUrl)
                }

                is SelfHostedUserState.UserDetail -> {
                    UserDetail(
                        targetState.user,
                        onUserAvatarClick
                    )
                }

                is SelfHostedUserState.Offline -> {
                    OfflineView()
                }
            }
        }
    }
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
            .fillMaxWidth()
            .clickable(
                onClickLabel = stringResource(R.string.user_row_content_description, user.name)
            ) {
                onUserClick(user)
            }
    ) {
        item {
            Column(modifier = Modifier.padding(all = userScreenPaddingDp)) {
                SmallAvatar(
                    avatarUrl = user.avatarUrls?.values?.firstOrNull(),
                    contentDescription = stringResource(R.string.user_avatar_content_description, user.name)
                )
            }
            Column(
                modifier = Modifier
                    .padding(
                        top = userScreenPaddingDp,
                        bottom = userScreenPaddingDp,
                        end = userScreenPaddingDp
                    )
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
    onAvatarClick: (String?) -> Unit = {},
) {
    Row(
        modifier = Modifier
            .padding(all = userScreenPaddingDp)
            .fillMaxWidth()
    ) {
        Column {
            val avatarUrl = user.avatarUrls?.values?.firstOrNull()
            SmallAvatar(
                avatarUrl = avatarUrl,
                contentDescription = stringResource(R.string.user_avatar_content_description, user.name),
                onAvatarClick = if (avatarUrl.isNullOrEmpty()) {
                    null
                } else {
                    onAvatarClick
                }
            )
        }

        Column(
            modifier = Modifier
                .padding(start = userScreenPaddingDp)
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
                    text = user.description.ifEmpty {
                        stringResource(R.string.biographical_info_empty)
                    },
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
    Spacer(modifier = Modifier.height(userScreenPaddingDp))
    content()
    HorizontalDivider(thickness = 1.dp)
    Spacer(modifier = Modifier.height(userScreenPaddingDp))
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
    Spacer(modifier = Modifier.height(userScreenPaddingDp))
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
private fun UserListScreenPreview() {
    val uiState = SelfHostedUserState.UserList(SampleUsers.getSampleUsers())
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
private fun UserDetailScreenPreview() {
    val uiState = SelfHostedUserState.UserDetail(SampleUsers.getSampleUsers().first())
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
private fun EmptyUserListScreenPreview() {
    val uiState = SelfHostedUserState.EmptyUserList
    SelfHostedUsersScreen(MutableStateFlow(uiState))
}

@Composable
@Preview(
    name = "Offline View Light Mode",
    showBackground = true
)
@Preview(
    name = "Offline View Dark Mode",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
private fun OfflineScreenPreview() {
    val uiState = SelfHostedUserState.Offline
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
private fun ProgressPreview() {
    val uiState = SelfHostedUserState.Loading
    SelfHostedUsersScreen(MutableStateFlow(uiState))
}

private val userScreenPaddingDp = 16.dp
