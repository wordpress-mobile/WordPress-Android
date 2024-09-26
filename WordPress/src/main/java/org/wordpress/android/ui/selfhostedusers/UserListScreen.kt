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
    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Column(
            modifier = Modifier.align(Alignment.CenterVertically)
        ) {
            val avatarUrl = user.avatarUrls?.values?.firstOrNull()
            if (avatarUrl.isNullOrEmpty()) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_user_placeholder_primary_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
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
                text = user.username,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = user.name,
                style = MaterialTheme.typography.bodyMedium
            )
            if (user.roles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = user.roles.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
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
    UserListScreen(
        users = MutableStateFlow(sampleUserList).collectAsState(),
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

private val sampleUser1 = UserWithEditContext(
    id = 1,
    username = "sampleUser",
    avatarUrls = emptyMap(),
    capabilities = emptyMap(),
    description = "User description",
    email = "email@exmaple.com",
    extraCapabilities = emptyMap(),
    firstName = "Sample",
    lastName = "User",
    link = "example.com",
    locale = "en-US",
    name = "Sample User",
    nickname = "User nickname",
    registeredDate = "2023-01-01",
    roles = listOf("admin"),
    slug = "sample-user",
    url = "example.com",
)

// TODO remove the avatar url
private val sampleUser2 = UserWithEditContext(
    id = 2,
    username = "sampleUser",
    avatarUrls = mapOf("sampleUserTwo" to "https://nickbradbury.com/wp-content/uploads/2022/03/1394-2.jpg"),
    capabilities = emptyMap(),
    description = "User description",
    email = "email@exmaple.com",
    extraCapabilities = emptyMap(),
    firstName = "Sample",
    lastName = "User",
    link = "example.com",
    locale = "en-US",
    name = "Sample User",
    nickname = "User nickname",
    registeredDate = "2023-01-01",
    roles = listOf("admin"),
    slug = "sample-user",
    url = "example.com",
)
val sampleUserList = listOf(
    sampleUser1,
    sampleUser2,
    sampleUser1,
    sampleUser2,
    sampleUser1,
    sampleUser2,
    sampleUser1,
    sampleUser2,
    sampleUser1,
    sampleUser2,
    sampleUser1,
    sampleUser2,
    sampleUser1,
    sampleUser2,
    sampleUser1,
    sampleUser2,
)
