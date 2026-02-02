package org.wordpress.android.support.common.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.EmptyContentM3
import org.wordpress.android.ui.compose.theme.AppThemeM3

@Composable
fun OfflineConversationsView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EmptyContentM3(
            title = stringResource(R.string.no_network_title),
        )
    }
}

@Preview(showBackground = true, name = "Empty Conversations View")
@Composable
private fun OfflineConversationsViewPreview() {
    AppThemeM3(isDarkTheme = false) {
        OfflineConversationsView()
    }
}

@Preview(showBackground = true, name = "Empty Conversations View - Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun OfflineConversationsViewPreviewDark() {
    AppThemeM3(isDarkTheme = true) {
        OfflineConversationsView()
    }
}

@Preview(showBackground = true, name = "Empty Conversations View - WordPress")
@Composable
private fun OfflineConversationsViewPreviewWordPress() {
    AppThemeM3(isDarkTheme = false, isJetpackApp = false) {
        OfflineConversationsView()
    }
}

@Preview(showBackground = true, name = "Empty Conversations View - Dark WordPress", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun OfflineConversationsViewPreviewWordPressDark() {
    AppThemeM3(isDarkTheme = true, isJetpackApp = false) {
        OfflineConversationsView()
    }
}
