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
fun ErrorConversationsView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EmptyContentM3(
            title = stringResource(R.string.error_generic),
            image = R.drawable.img_jetpack_empty_state,
            imageContentDescription = stringResource(R.string.error_generic)
        )
    }
}

@Preview(showBackground = true, name = "Error Conversations View")
@Composable
private fun ErrorConversationsViewPreview() {
    AppThemeM3(isDarkTheme = false) {
        ErrorConversationsView()
    }
}

@Preview(showBackground = true, name = "Error Conversations View - Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ErrorConversationsViewPreviewDark() {
    AppThemeM3(isDarkTheme = true) {
        ErrorConversationsView()
    }
}

@Preview(showBackground = true, name = "Error Conversations View - WordPress")
@Composable
private fun ErrorConversationsViewPreviewWordPress() {
    AppThemeM3(isDarkTheme = false, isJetpackApp = false) {
        ErrorConversationsView()
    }
}

@Preview(showBackground = true, name = "Error Conversations View - Dark WordPress", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ErrorConversationsViewPreviewWordPressDark() {
    AppThemeM3(isDarkTheme = true, isJetpackApp = false) {
        ErrorConversationsView()
    }
}
