package org.wordpress.android.ui.bloggingprompts.promptslist.compose

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.wordpress.android.ui.bloggingprompts.promptslist.BloggingPromptsListViewModel.UiState
import org.wordpress.android.ui.bloggingprompts.promptslist.model.BloggingPromptsListItemModel
import java.util.Date

class BloggingPromptsListScreenPreviewProvider : PreviewParameterProvider<UiState> {
    private val fakePromptList = List(11) {
        BloggingPromptsListItemModel(
                id = it,
                text = "Prompt text",
                date = Date(if (it == 0) 1671678000000 else 1671591600000),
                formattedDate = if (it == 0) "Dec 22" else "Dec 21",
                isAnswered = it % 2 == 0,
                answersCount = it,
        )
    }

    override val values: Sequence<UiState> = sequenceOf(
            UiState.None,
            UiState.Loading,
            UiState.Content(fakePromptList),
            UiState.Content(emptyList()),
            UiState.FetchError,
            UiState.NetworkError,
    )
}

class BloggingPromptsListItemPreviewProvider : PreviewParameterProvider<BloggingPromptsListItemModel> {
    override val values: Sequence<BloggingPromptsListItemModel> = sequenceOf(
            BloggingPromptsListItemModel(
                    id = 0,
                    text = "Cast the movie of your life.",
                    date = Date(1671678000000),
                    formattedDate = "Dec 22",
                    isAnswered = false,
                    answersCount = 0,
            ),
            BloggingPromptsListItemModel(
                    id = 1,
                    text = "What makes you feel nostalgic?",
                    date = Date(1671591600000),
                    formattedDate = "Dec 21",
                    isAnswered = true,
                    answersCount = 123,
            )
    )
}
