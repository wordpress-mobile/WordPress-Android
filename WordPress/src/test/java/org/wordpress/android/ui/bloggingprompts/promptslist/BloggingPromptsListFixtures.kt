package org.wordpress.android.ui.bloggingprompts.promptslist

import org.wordpress.android.fluxc.model.bloggingprompts.BloggingPromptModel
import org.wordpress.android.ui.bloggingprompts.promptslist.model.BloggingPromptsListItemModel
import java.util.Calendar
import java.util.Date

@Suppress("unused")
object BloggingPromptsListFixtures {
    val DOMAIN_MODEL = BloggingPromptModel(
        id = 123,
        text = "Text",
        title = "Title",
        content = "Content",
        date = Date(1671678000000), // December 22, 2022
        isAnswered = true,
        attribution = "Attribution",
        respondentsCount = 321,
        respondentsAvatarUrls = emptyList(),
    )

    val UI_MODEL = BloggingPromptsListItemModel(
        id = 123,
        text = "Text",
        date = Date(1671678000000), // December 22, 2022
        formattedDate = "Dec 22",
        isAnswered = true,
        answersCount = 321,
    )

    fun domainModelListForNextDays(initialDate: Date, count: Int): List<BloggingPromptModel> {
        val calendar = Calendar.getInstance().apply { time = initialDate }
        return List(count) { generateFakePromptDomainModel(it, calendar.getDateAndAddADay()) }
    }

    fun uiModelListForNextDays(initialDate: Date, count: Int): List<BloggingPromptsListItemModel> {
        val calendar = Calendar.getInstance().apply { time = initialDate }
        return List(count) { generateFakePromptUiModel(it, calendar.getDateAndAddADay()) }
    }

    private fun generateFakePromptDomainModel(id: Int, date: Date) = BloggingPromptModel(
        id = id,
        text = "Text $id",
        date = date,
        isAnswered = id % 2 == 0,
        respondentsCount = id,
        title = "Title $id",
        content = "Content $id",
        attribution = "Attribution $id",
        respondentsAvatarUrls = listOf(),

        )

    private fun generateFakePromptUiModel(id: Int, date: Date) = BloggingPromptsListItemModel(
        id = id,
        text = "Text $id",
        date = date,
        formattedDate = "Date ${date.time}",
        isAnswered = id % 2 == 0,
        answersCount = id,
    )

    private fun Calendar.getDateAndAddADay(): Date {
        val currentDate = time
        add(Calendar.DAY_OF_YEAR, 1)
        return currentDate
    }
}
