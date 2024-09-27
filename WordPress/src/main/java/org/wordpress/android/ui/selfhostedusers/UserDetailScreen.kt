package org.wordpress.android.ui.selfhostedusers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import uniffi.wp_api.UserWithEditContext

@Composable
fun UserDetailScreen(
    user: State<UserWithEditContext>,
    onCloseClick: () -> Unit = {},
) {
    val content: @Composable () -> Unit = @Composable {
        UserDetail(user.value)
    }
    UserScreen(
        title = stringResource(R.string.user),
        content = content,
        onCloseClick = { onCloseClick() },
        isScrollable = true
    )
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
