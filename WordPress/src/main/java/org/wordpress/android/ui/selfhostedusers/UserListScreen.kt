package org.wordpress.android.ui.selfhostedusers

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    onCloseClick: (context: Context) -> Unit = {},
) {
    val context = LocalContext.current
    val content: @Composable () -> Unit = @Composable {
        progressDialogState?.value?.let {
            ProgressDialog(it)
        } ?: run {
            if (users.value.isNotEmpty()) {
                UserList(users.value)
            } else {
                EmptyView()
            }
        }
    }
    Screen(
        content = content,
        onCloseClick = { onCloseClick(context) },
        isScrollable = users.value.isNotEmpty()
    )
}

@Composable
private fun UserList(users: List<UserWithEditContext>) {
    for (user in users) {
        UserRow(user)
    }
}

@Composable
fun UserRow(user: UserWithEditContext) {
    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(
            modifier = Modifier.align(Alignment.CenterVertically)
        ) {
            if (user.avatarUrls.isNullOrEmpty()) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_user_primary_white_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .size(48.dp)
                )
            } else {
                coil.compose.AsyncImage(
                    model = user.avatarUrls!!["0"],
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                )
            }
        }
        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .align(Alignment.CenterVertically)
        ) {
            Text(
                text = user.name,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = user.email,
            )
        }
    }
}

@Composable
private fun EmptyView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_people_white_24dp),
            contentDescription = null,
            modifier = Modifier
                .size(85.dp)
        )
        Text(
            text = stringResource(id = R.string.no_users),
            style = MaterialTheme.typography.titleLarge,
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
    val userList = listOf(
        UserWithEditContext(
            id = 1,
            username = "userone",
            avatarUrls = emptyMap(),
            capabilities = emptyMap(),
            description = "User One description",
            email = "email@userone.com",
            extraCapabilities = emptyMap(),
            firstName = "User",
            lastName = "One",
            link = "link@userone.com",
            locale = "en-US",
            name = "User One",
            nickname = "User One nickname",
            registeredDate = "2023-01-01",
            roles = emptyList(),
            slug = "userone",
            url = "url@userone.com",
        ),
        UserWithEditContext(
            id = 2,
            username = "usertwo",
            avatarUrls = emptyMap(),
            capabilities = emptyMap(),
            description = "User Two description",
            email = "email@usertwo.com",
            extraCapabilities = emptyMap(),
            firstName = "User",
            lastName = "Two",
            link = "link@usertwo.com",
            locale = "en-US",
            name = "User Two",
            nickname = "User Two nickname",
            registeredDate = "2023-01-01",
            roles = emptyList(),
            slug = "usertwo",
            url = "url@usertwo.com",
        )
    )
    UserListScreen(
        users = MutableStateFlow(userList).collectAsState(),
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
