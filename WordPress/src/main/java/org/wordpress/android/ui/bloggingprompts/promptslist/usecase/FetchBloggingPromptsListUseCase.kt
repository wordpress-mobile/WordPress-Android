package org.wordpress.android.ui.bloggingprompts.promptslist.usecase

import kotlinx.coroutines.delay
import org.wordpress.android.ui.bloggingprompts.promptslist.model.BloggingPromptsListItemModel
import org.wordpress.android.ui.bloggingprompts.promptslist.usecase.FetchBloggingPromptsListUseCase.Result.Success
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

// TODO thomashorta remove this suppress annotation when this has a real implementation
@Suppress("MagicNumber")
class FetchBloggingPromptsListUseCase @Inject constructor() {
    suspend fun execute(): Result {
        // delay a bit to simulate a fetch
        delay(1500)
        return Success(generateFakePrompts())
    }

    sealed class Result {
        class Success(val content: List<BloggingPromptsListItemModel>) : Result()
        object Failure : Result()

        inline fun onSuccess(block: (List<BloggingPromptsListItemModel>) -> Unit): Result {
            if (this is Success) block(this.content)
            return this
        }

        fun onFailure(block: () -> Unit): Result {
            if (this is Failure) block()
            return this
        }
    }

    // FAKE DATA GENERATION BELOW

    private fun generateFakePrompts(): List<BloggingPromptsListItemModel> {
        val calendar = Calendar.getInstance()
        return List(11) { generateFakePrompt(it, calendar.getDateAndSubtractADay()) }
    }

    private fun generateFakePrompt(index: Int, date: Date) = BloggingPromptsListItemModel(
            id = index,
            text = fakePrompts.random(),
            date = date,
            isAnswered = listOf(true, false).random(),
            answersCount = index,
    )

    private fun Calendar.getDateAndSubtractADay(): Date {
        val currentDate = time
        add(Calendar.DAY_OF_YEAR, -1)
        return currentDate
    }

    companion object {
        private val fakePrompts = listOf(
                "What makes you feel nostalgic?",
                "What relationships have a negative impact on you?",
                "If you started a sports team, what would the colors and mascot be?",
                "How have your political views changed over time?",
                "You get to build your perfect space for reading and writing. What’s it like?",
                "Have you ever been in an automobile accident?"
        )
    }
}
