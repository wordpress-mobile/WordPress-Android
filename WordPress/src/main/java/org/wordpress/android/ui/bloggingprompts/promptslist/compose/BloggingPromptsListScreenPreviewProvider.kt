package org.wordpress.android.ui.bloggingprompts.promptslist.compose

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.wordpress.android.ui.bloggingprompts.promptslist.BloggingPromptsListViewModel.UiState
import org.wordpress.android.ui.bloggingprompts.promptslist.model.BloggingPromptsListItemModel
import java.util.Date

class BloggingPromptsListScreenPreviewProvider : PreviewParameterProvider<UiState> {
    override val values: Sequence<UiState>
        get() = sequenceOf(
            UiState.None,
            UiState.Loading,
            UiState.Content(fakePromptList),
            // TODO thomashorta add missing UiStates when their content is developed
        )

    private val fakePromptList = List(11) {
        BloggingPromptsListItemModel(
            id = it,
            text = "Prompt text",
            date = Date(if (it == 0) 1671678000000 else 1671591600000),
            isAnswered = it % 2 == 0,
            answersCount = 10 * it,
        )
    }
}
