package org.wordpress.android.ui.bloggingprompts.promptslist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
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
import org.wordpress.android.util.LocaleManager
import java.text.DateFormat

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
    // TODO thomashorta this is all just temporary for now, the actual list item will be created in the next task
    val dateFormat = DateFormat.getDateInstance(
            DateFormat.MEDIUM,
            LocaleManager.getSafeLocale(LocalContext.current)
    )
    LazyColumn(
            Modifier.fillMaxWidth(),
    ) {
        items(promptsList) {
            val answeredText = if (it.isAnswered) "- Answered" else ""
            Column(Modifier.padding(16.dp)) {
                Text(
                        it.text,
                        style = MaterialTheme.typography.subtitle1
                )
                Text(
                        "${dateFormat.format(it.date)} - ${it.answersCount} answers $answeredText",
                        style = MaterialTheme.typography.body2
                )
            }
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
@Composable
fun BloggingPromptsListScreenPreview(
    @PreviewParameter(provider = BloggingPromptsListScreenPreviewProvider::class) uiState: UiState
) {
    AppTheme {
        BloggingPromptsListScreen(uiState = uiState, onNavigateUp = {})
    }
}
