package org.wordpress.android.ui.stories.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.ui.posts.EditPostRepository

@ExperimentalCoroutinesApi
class UpdateStoryPostTitleUseCaseTest : BaseUnitTest() {
    private lateinit var editPostRepository: EditPostRepository
    private lateinit var updateStoryTitleUseCase: UpdateStoryPostTitleUseCase

    @Before
    fun setup() {
        updateStoryTitleUseCase = UpdateStoryPostTitleUseCase()
        editPostRepository = EditPostRepository(
                mock(),
                mock(),
                mock(),
                coroutinesTestRule.testDispatcher,
                coroutinesTestRule.testDispatcher
        )
        editPostRepository.set { PostModel() }
    }

    @Test
    fun `verify that when updateStoryTitleUseCase is called with a story title the post title is updated`() {
        // arrange
        val storyTitle = "Story Title"

        // act
        updateStoryTitleUseCase.updateStoryTitle(storyTitle, editPostRepository)

        // assert
        assertThat(editPostRepository.title).isEqualTo(storyTitle)
    }
}
