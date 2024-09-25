package org.wordpress.android.ui.selfhostedusers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uniffi.wp_api.UserWithEditContext

@Composable
fun UserCard(user: UserWithEditContext) {
    Row(modifier = Modifier.padding(all = 8.dp)) {
        Column {
            Text(
                text = user.name,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text =  user.email,
            )
        }
    }
}

@Composable
@Preview
fun UserListScreen() {
    val userList = listOf(
        UserWithEditContext(
            id = 1,
            username = "userone",
            avatarUrls = emptyMap(),
            capabilities = emptyMap(),
            description = "User One description",
            email= "email@userone.com",
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
            email= "email@usertwo.com",
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
