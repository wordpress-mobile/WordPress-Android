package org.wordpress.android.ui.stories.usecase

import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.ui.posts.EditPostRepository

class UpdateStoryPostTitleUseCaseTest : BaseUnitTest() {
    private lateinit var editPostRepository: EditPostRepository
    private lateinit var updateStoryTitleUseCase: UpdateStoryPostTitleUseCase

    @InternalCoroutinesApi
    @Before
    fun setup() {
        updateStoryTitleUseCase = UpdateStoryPostTitleUseCase()
        editPostRepository = EditPostRepository(mock(), mock(), mock(), TEST_DISPATCHER, TEST_DISPATCHER)
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
