package org.wordpress.android.ui.bloggingprompts.promptslist.compose

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import org.wordpress.android.ui.compose.components.EmptyContentM3
import org.wordpress.android.ui.compose.theme.AppThemeM3

private val emptyContentModifier: Modifier = Modifier.fillMaxSize()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloggingPromptsListScreen(
    uiState: UiState,
    onNavigateUp: () -> Unit,
    onItemClick: (BloggingPromptsListItemModel) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.blogging_prompts_list_title))
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.back)
                        )
                    }
                },
            )
        },
    ) { contentPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            when (uiState) {
                is Content -> ListContent(uiState.content, onItemClick)
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
    promptsList: List<BloggingPromptsListItemModel>,
    onItemClick: (BloggingPromptsListItemModel) -> Unit
) {
    if (promptsList.isEmpty()) {
        NoContent()
    } else {
        LazyColumn(Modifier.fillMaxSize()) {
            itemsIndexed(promptsList) { index, item ->
                if (index != 0) HorizontalDivider()

                BloggingPromptsListItem(
                    model = item,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onItemClick,
                )
            }
        }
    }
}

@Composable
private fun NoContent() {
    EmptyContentM3(
        title = stringResource(R.string.blogging_prompts_list_no_prompts),
        image = R.drawable.img_illustration_empty_results_216dp,
        modifier = emptyContentModifier,
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
    EmptyContentM3(
        title = stringResource(R.string.blogging_prompts_list_error_fetch_title),
        subtitle = stringResource(R.string.blogging_prompts_list_error_fetch_subtitle),
        image = R.drawable.img_illustration_empty_results_216dp,
        modifier = emptyContentModifier,
    )
}

@Composable
private fun NetworkErrorContent() {
    EmptyContentM3(
        title = stringResource(R.string.no_connection_error_title),
        subtitle = stringResource(R.string.no_connection_error_description),
        modifier = emptyContentModifier,
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun BloggingPromptsListScreenPreview(
    @PreviewParameter(provider = BloggingPromptsListScreenPreviewProvider::class) uiState: UiState
) {
    AppThemeM3 {
        BloggingPromptsListScreen(
            uiState = uiState,
            onNavigateUp = {},
            onItemClick = {}
        )
    }
}
