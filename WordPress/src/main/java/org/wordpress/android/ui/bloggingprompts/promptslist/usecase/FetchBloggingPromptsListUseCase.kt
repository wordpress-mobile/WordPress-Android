package org.wordpress.android.ui.bloggingprompts.promptslist.usecase

import org.wordpress.android.fluxc.model.bloggingprompts.BloggingPromptModel
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

class FetchBloggingPromptsListUseCase @Inject constructor(
    private val bloggingPromptsStore: BloggingPromptsStore,
    private val selectedSiteRepository: SelectedSiteRepository,
) {
    suspend fun execute(): Result {
        return fetchBloggingPrompts()
                ?.sortedByDescending { it.date }
                ?.dropWhile { it.date > Date() } // don't display future prompts
                ?.take(NUMBER_OF_PROMPTS)
                ?.let { Result.Success(it) } // success if fetchBloggingPrompts was not null
                ?: Result.Failure // failure otherwise
    }

    /**
     * Returns the List of Blogging Prompts for the current site, with a size of [NUMBER_OF_PROMPTS] if the fetch was
     * successful, otherwise returns null, indicating a failure.
     */
    private suspend fun fetchBloggingPrompts(): List<BloggingPromptModel>? {
        // get the starting date to fetch the prompts from
        // it is today's date minus (number of prompts - 1) because it needs to fetch today's prompt as well
        val fromDate = LocalDate.now()
                .minusDays(NUMBER_OF_PROMPTS.toLong() - 1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .let { Date.from(it) }

        return selectedSiteRepository.getSelectedSite()?.let { site ->
            bloggingPromptsStore.fetchPrompts(site, NUMBER_OF_PROMPTS, fromDate)
                    .takeUnless { it.isError }
                    ?.model
        }
    }

    sealed class Result {
        class Success(val content: List<BloggingPromptModel>) : Result()
        object Failure : Result()

        inline fun onSuccess(block: (List<BloggingPromptModel>) -> Unit): Result {
            if (this is Success) block(this.content)
            return this
        }

        fun onFailure(block: () -> Unit): Result {
            if (this is Failure) block()
            return this
        }
    }

    companion object {
        private const val NUMBER_OF_PROMPTS = 11
    }
}
