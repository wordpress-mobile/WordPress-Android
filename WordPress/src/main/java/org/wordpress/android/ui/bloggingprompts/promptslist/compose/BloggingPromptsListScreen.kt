package org.wordpress.android.ui.bloggingprompts.promptslist.compose

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import org.wordpress.android.R
import org.wordpress.android.ui.bloggingprompts.promptslist.BloggingPromptsListViewModel.UiState
import org.wordpress.android.ui.bloggingprompts.promptslist.BloggingPromptsListViewModel.UiState.Content
import org.wordpress.android.ui.bloggingprompts.promptslist.BloggingPromptsListViewModel.UiState.FetchError
import org.wordpress.android.ui.bloggingprompts.promptslist.BloggingPromptsListViewModel.UiState.Loading
import org.wordpress.android.ui.bloggingprompts.promptslist.BloggingPromptsListViewModel.UiState.NetworkError
import org.wordpress.android.ui.bloggingprompts.promptslist.BloggingPromptsListViewModel.UiState.None
import org.wordpress.android.ui.bloggingprompts.promptslist.model.BloggingPromptsListItemModel
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
        Surface(modifier = Modifier.fillMaxSize()) {
            when (uiState) {
                is Content -> ListContent(uiState.content)
                Loading -> LoadingContent()
                FetchError -> TODO()
                NetworkError -> TODO()
                None -> {}
            }
        }
    }
}

@Composable
private fun ListContent(
    promptsList: List<BloggingPromptsListItemModel>
) {
    LazyColumn(Modifier.fillMaxSize()) {
        itemsIndexed(promptsList) { index, item ->
            if (index != 0) Divider()

            BloggingPromptsListItem(
                model = item,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(Modifier.fillMaxSize()) {
        Text(
            "Loading...",
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun BloggingPromptsListScreenPreview(
    @PreviewParameter(provider = BloggingPromptsListScreenPreviewProvider::class) uiState: UiState
) {
    AppTheme {
        BloggingPromptsListScreen(uiState = uiState, onNavigateUp = {})
    }
}
