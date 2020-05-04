package org.wordpress.android.ui.posts.prepublishing.visibility.usecases

import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.PRIVATE
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.PUBLIC

class UpdatePostStatusUseCaseTest : BaseUnitTest() {
    lateinit var editPostRepository: EditPostRepository
    lateinit var updatePostStatusUseCase: UpdatePostStatusUseCase

    @InternalCoroutinesApi
    @Before
    fun setup() {
        updatePostStatusUseCase = UpdatePostStatusUseCase()
        editPostRepository = EditPostRepository(mock(), mock(), mock(), TEST_DISPATCHER, TEST_DISPATCHER)
        editPostRepository.set { PostModel() }
    }

    @Test
    fun `verify that when updatePostStatus is called with PRIVATE Visibility the PostStatus is PRIVATE`() {
        // arrange
        val expectedPostStatus = PostStatus.PRIVATE

        // act
        updatePostStatusUseCase.updatePostStatus(PRIVATE, editPostRepository) {}

        // assert
        assertThat(editPostRepository.getPost()?.status).isEqualTo(expectedPostStatus.toString())
    }

    @Test
    fun `verify that when updatePostStatus is called with PUBLIC Visibility the PostStatus is DRAFT`() {
        // arrange
        val expectedPostStatus = PostStatus.DRAFT

        // act
        updatePostStatusUseCase.updatePostStatus(PUBLIC, editPostRepository) {}

        // assert
        assertThat(editPostRepository.getPost()?.status).isEqualTo(expectedPostStatus.toString())
    }
}
