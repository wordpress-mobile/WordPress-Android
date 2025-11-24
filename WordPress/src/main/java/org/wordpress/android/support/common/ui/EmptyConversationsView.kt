package org.wordpress.android.support.common.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3

@Composable
fun EmptyConversationsView(
    modifier: Modifier,
    onCreateNewConversationClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "💬",
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.he_support_empty_conversations_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.padding(8.dp))

        Text(
            text = stringResource(R.string.he_support_empty_conversations_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.padding(24.dp))

        Button(onClick = onCreateNewConversationClick) {
            Text(text = stringResource(R.string.he_support_empty_conversations_button))
        }
    }
}

@Preview(showBackground = true, name = "Empty Conversations View")
@Composable
private fun EmptyConversationsViewPreview() {
    AppThemeM3(isDarkTheme = false) {
        EmptyConversationsView(
            modifier = Modifier,
            onCreateNewConversationClick = { }
        )
    }
}

@Preview(showBackground = true, name = "Empty Conversations View - Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun EmptyConversationsViewPreviewDark() {
    AppThemeM3(isDarkTheme = true) {
        EmptyConversationsView(
            modifier = Modifier,
            onCreateNewConversationClick = { }
        )
    }
}

@Preview(showBackground = true, name = "Empty Conversations View - WordPress")
@Composable
private fun EmptyConversationsViewPreviewWordPress() {
    AppThemeM3(isDarkTheme = false, isJetpackApp = false) {
        EmptyConversationsView(
            modifier = Modifier,
            onCreateNewConversationClick = { }
        )
    }
}

@Preview(showBackground = true, name = "Empty Conversations View - Dark WordPress", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun EmptyConversationsViewPreviewWordPressDark() {
    AppThemeM3(isDarkTheme = true, isJetpackApp = false) {
        EmptyConversationsView(
            modifier = Modifier,
            onCreateNewConversationClick = { }
        )
    }
}
