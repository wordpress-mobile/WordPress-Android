package org.wordpress.android.ui.bloggingprompts.promptslist.usecase

import kotlinx.coroutines.flow.first
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
        // get the starting date to fetch the prompts from
        // it is today's date minus (number of prompts - 1) because it needs to fetch today's prompt as well
        val fromDate = LocalDate.now()
                .minusDays(NUMBER_OF_PROMPTS.toLong() - 1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .let { Date.from(it) }

        val site = selectedSiteRepository.getSelectedSite() ?: return Result.Failure

        // fetchPrompts do not return the actual fetched prompts, it only stores them in the local FluxC database so
        // if the fetch is successful we still need to cal getPrompts to get the actual prompts list result
        // if the get is also successful then we can proceed to mapping the prompt model to list item models
        val result = bloggingPromptsStore.fetchPrompts(site, NUMBER_OF_PROMPTS, fromDate)
                .takeUnless { it.isError }
                ?.let { bloggingPromptsStore.getPrompts(site) }
                ?.first()
                ?.takeUnless { it.isError }

        return result?.run {
            val prompts = model
                    ?.sortedByDescending { it.date }
                    ?.dropWhile { it.date > Date() } // don't display future prompts
                    ?.take(NUMBER_OF_PROMPTS)
                    ?: emptyList()
            Result.Success(prompts)
        } ?: Result.Failure
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
