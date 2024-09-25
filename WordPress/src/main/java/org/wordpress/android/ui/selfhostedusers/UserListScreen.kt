package org.wordpress.android.ui.selfhostedusers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.M3Theme
import uniffi.wp_api.UserWithEditContext

@Composable
fun UserListScreen(users: List<UserWithEditContext>) {
    val content: @Composable () -> Unit = @Composable {
        LazyColumn {
            items(users) {
                UserCard(it)
            }
        }
    }
    Screen(
        content = content,
        onCloseClick = {}
    )
}

@Composable
fun UserCard(user: UserWithEditContext) {
    Row(modifier = Modifier.padding(all = 8.dp)) {
        Column {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Screen(
    content: @Composable () -> Unit,
    onCloseClick: () -> Unit
) {
    M3Theme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(id = R.string.feedback_form_title)) },
                    navigationIcon = {
                        IconButton(onClick = onCloseClick) {
                            Icon(Icons.Filled.Close, stringResource(R.string.close))
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
                    .verticalScroll(rememberScrollState())
            ) {
                content()
            }
        }
    }
}

@Composable
@Preview
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
    MaterialTheme {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn {
                items(userList) {
                    UserCard(it)
                }
            }
        }
    }
}
