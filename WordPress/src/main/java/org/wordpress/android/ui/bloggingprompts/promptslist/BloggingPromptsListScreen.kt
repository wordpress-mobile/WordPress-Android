package org.wordpress.android.ui.bloggingprompts.promptslist

import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.R
import org.wordpress.android.ui.bloggingprompts.promptslist.BloggingPromptsListViewModel.UiState
import org.wordpress.android.ui.bloggingprompts.promptslist.BloggingPromptsListViewModel.UiState.None
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppTheme

@Composable
fun BloggingPromptsListScreen(
    uiState: UiState,
    onNavigateUp: () -> Unit,
) {
    Scaffold(
            topBar = {
                MainTopAppBar(
                        title = stringResource(R.string.blogging_prompts_list_title),
                        navigationIcon = NavigationIcons.BackIcon,
                        onNavigationIconClick = onNavigateUp
                )
            },
    ) {
        Text("This is just a placeholder for now: ${uiState::class.simpleName}")
    }
}

@Preview
@Composable
fun BloggingPromptsListScreenPreview() {
    AppTheme {
        BloggingPromptsListScreen(uiState = None, onNavigateUp = {})
    }
}
