package org.wordpress.android.ui.bloggingprompts.promptslist.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest

@ExperimentalCoroutinesApi
class FetchBloggingPromptsListUseCaseTest : BaseUnitTest() {
    lateinit var useCase: FetchBloggingPromptsListUseCase

    @Before
    fun setUp() {
        useCase = FetchBloggingPromptsListUseCase()
    }

    @Test
    fun `when execute is called, then return 11 items`() = test {
        val result = useCase.execute()
        advanceUntilIdle()
        assertThat(result).hasSize(11)
        assertThat(result).allMatch { it.text.isNotBlank() }
    }
}
