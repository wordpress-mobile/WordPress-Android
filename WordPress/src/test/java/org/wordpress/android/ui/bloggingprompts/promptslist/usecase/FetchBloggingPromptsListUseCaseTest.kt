package org.wordpress.android.ui.bloggingprompts.promptslist.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore.BloggingPromptsResult
import org.wordpress.android.ui.bloggingprompts.promptslist.BloggingPromptsListFixtures
import org.wordpress.android.ui.bloggingprompts.promptslist.usecase.FetchBloggingPromptsListUseCase.Result.Failure
import org.wordpress.android.ui.bloggingprompts.promptslist.usecase.FetchBloggingPromptsListUseCase.Result.Success
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import java.util.Date

@ExperimentalCoroutinesApi
class FetchBloggingPromptsListUseCaseTest : BaseUnitTest() {
    @Mock lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock lateinit var bloggingPromptsStore: BloggingPromptsStore
    lateinit var useCase: FetchBloggingPromptsListUseCase

    @Before
    fun setUp() {
        useCase = FetchBloggingPromptsListUseCase(
                bloggingPromptsStore = bloggingPromptsStore,
                selectedSiteRepository = selectedSiteRepository
        )
    }

    @Test
    fun `given getting site fails, when execute is called, then return failure`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        val result = useCase.execute()

        assertThat(result).isEqualTo(Failure)
    }

    @Test
    fun `given fetching prompts fails, when execute is called, then return failure`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(SiteModel().apply { id = 1 })
        whenever(bloggingPromptsStore.fetchPrompts(any(), any(), any()))
                .thenReturn(BloggingPromptsResult(error = mock()))

        val result = useCase.execute()

        assertThat(result).isEqualTo(Failure)
    }

    @Test
    fun `when execute is called, then return success with the correct items in descending date order`() = test {
        val now = Date()
        var initialDate: Date = now
        var count: Int = 0
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(SiteModel().apply { id = 1 })
        whenever(bloggingPromptsStore.fetchPrompts(any(), any(), any()))
                .doSuspendableAnswer {
                    count = it.getArgument(1)
                    initialDate = it.getArgument(2)
                    BloggingPromptsResult(
                            model = BloggingPromptsListFixtures.domainModelListForNextDays(
                                    initialDate = initialDate,
                                    count = count,
                            )
                    )
                }

        val result = useCase.execute()

        val expectedItems = BloggingPromptsListFixtures.domainModelListForNextDays(
                initialDate = initialDate,
                count = count, // just the right amount of items
        ).sortedByDescending { it.date }

        assertThat(count).isNotEqualTo(0)
        assertThat(initialDate).isNotEqualTo(now)

        assertThat(result).isInstanceOf(Success::class.java)

        val content = (result as Success).content
        assertThat(content).hasSize(count)
        assertThat(content).isEqualTo(expectedItems)
    }

    @Test
    fun `when execute is called, then return success with no prompts in the future`() = test {
        val now = Date()
        var initialDate: Date = now
        var count: Int = 0
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(SiteModel().apply { id = 1 })
        whenever(bloggingPromptsStore.fetchPrompts(any(), any(), any()))
            .doSuspendableAnswer {
                count = it.getArgument(1)
                initialDate = it.getArgument(2)
                BloggingPromptsResult(
                    model = BloggingPromptsListFixtures.domainModelListForNextDays(
                        initialDate = initialDate,
                        count = count + 10, // add items in the future as well for the test
                    )
                )
            }

        val result = useCase.execute()

        assertThat(count).isNotEqualTo(0)
        assertThat(initialDate).isNotEqualTo(now)

        assertThat(result).isInstanceOf(Success::class.java)

        val content = (result as Success).content
        assertThat(content).hasSize(count) // filtered out future prompts
        assertThat(content).noneMatch { it.date > now } // double check looking at each item's date
    }
}
