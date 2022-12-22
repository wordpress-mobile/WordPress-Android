package org.wordpress.android.ui.bloggingprompts.promptslist.usecase

import kotlinx.coroutines.delay
import org.wordpress.android.ui.bloggingprompts.promptslist.model.BloggingPromptsListItemModel
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import kotlin.random.Random

class FetchBloggingPromptsListUseCase @Inject constructor() {
    suspend fun execute(): List<BloggingPromptsListItemModel> {
        // delay a bit to simulate a fetch
        delay(1500)
        return generateFakePrompts()
    }

    // FAKE DATA GENERATION BELOW

    private fun generateFakePrompts(): List<BloggingPromptsListItemModel> {
        val calendar = Calendar.getInstance()
        return List(11) { generateFakePrompt(it, calendar.getDateAndSubtractADay()) }
    }

    private fun generateFakePrompt(id: Int, date: Date) = BloggingPromptsListItemModel(
            id = id,
            text = fakePrompts.random(),
            date = date,
            isAnswered = listOf(true, false).random(),
            answersCount = Random.nextInt(5000),
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
