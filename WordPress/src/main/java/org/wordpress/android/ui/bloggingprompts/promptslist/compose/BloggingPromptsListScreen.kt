package org.wordpress.android.ui.bloggingprompts.promptslist.compose

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
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
import org.wordpress.android.ui.compose.components.EmptyContent
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
                FetchError -> FetchErrorContent()
                NetworkError -> NetworkErrorContent()
                None -> {}
            }
        }
    }
}

@Composable
private fun ListContent(
    promptsList: List<BloggingPromptsListItemModel>
) {
    if (promptsList.isEmpty()) {
        NoContent()
    } else {
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
}

@Composable
private fun NoContent() {
    EmptyContent(
        title = stringResource(R.string.blogging_prompts_list_no_prompts),
        image = R.drawable.img_illustration_empty_results_216dp,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun LoadingContent() {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun FetchErrorContent() {
    EmptyContent(
        title = stringResource(R.string.blogging_prompts_list_error_fetch_title),
        subtitle = stringResource(R.string.blogging_prompts_list_error_fetch_subtitle),
        image = R.drawable.img_illustration_empty_results_216dp,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun NetworkErrorContent() {
    EmptyContent(
        title = stringResource(R.string.blogging_prompts_list_error_network_title),
        subtitle = stringResource(R.string.blogging_prompts_list_error_network_subtitle),
        image = R.drawable.img_illustration_cloud_off_152dp,
        modifier = Modifier.fillMaxSize(),
    )
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
