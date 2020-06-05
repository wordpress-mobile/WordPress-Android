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
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.DRAFT
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.PASSWORD_PROTECTED
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.PENDING_REVIEW
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.PRIVATE
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.PUBLISH

class UpdateVisibilityUseCaseTest : BaseUnitTest() {
    private lateinit var editPostRepository: EditPostRepository
    private lateinit var updateVisibilityUseCase: UpdateVisibilityUseCase

    @InternalCoroutinesApi
    @Before
    fun setup() {
        updateVisibilityUseCase = UpdateVisibilityUseCase(UpdatePostStatusUseCase(mock()))
        editPostRepository = EditPostRepository(mock(), mock(), mock(), TEST_DISPATCHER, TEST_DISPATCHER)
        editPostRepository.set { PostModel() }
    }

    @Test
    fun `verify that when updatePostVisibility is called with PUBLISH Visibility the PostStatus is PUBLISHED`() {
        // arrange
        val expectedPostStatus = PostStatus.PUBLISHED

        // act
        updateVisibilityUseCase.updatePostVisibility(PUBLISH, editPostRepository) {}

        // assert
        assertThat(editPostRepository.getPost()?.status).isEqualTo(expectedPostStatus.toString())
    }

    @Test
    fun `verify that when updatePostVisibility is called with DRAFT Visibility the PostStatus is DRAFT`() {
        // arrange
        val expectedPostStatus = PostStatus.DRAFT

        // act
        updateVisibilityUseCase.updatePostVisibility(DRAFT, editPostRepository) {}

        // assert
        assertThat(editPostRepository.getPost()?.status).isEqualTo(expectedPostStatus.toString())
    }

    @Test
    fun `verify that when updatePostVisibility is called with PENDING_REVIEW Visibility the PostStatus is PENDING`() {
        // arrange
        val expectedPostStatus = PostStatus.PENDING

        // act
        updateVisibilityUseCase.updatePostVisibility(PENDING_REVIEW, editPostRepository) {}

        // assert
        assertThat(editPostRepository.getPost()?.status).isEqualTo(expectedPostStatus.toString())
    }

    @Test
    fun `verify that when updatePostVisibility is called with PRIVATE Visibility the PostStatus is PRIVATE`() {
        // arrange
        val expectedPostStatus = PostStatus.PRIVATE

        // act
        updateVisibilityUseCase.updatePostVisibility(PRIVATE, editPostRepository) {}

        // assert
        assertThat(editPostRepository.getPost()?.status).isEqualTo(expectedPostStatus.toString())
    }

    @Test(expected = IllegalStateException::class)
    fun `verify that when updatePostVisibility is called with PASSWORD_PROTECTED Visibility an exception is thrown`() {
        // act
        updateVisibilityUseCase.updatePostVisibility(PASSWORD_PROTECTED, editPostRepository) {}
    }
}
