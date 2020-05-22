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

class GetPostVisibilityUseCaseTest : BaseUnitTest() {
    private lateinit var editPostRepository: EditPostRepository
    private lateinit var getPostVisibilityUseCase: GetPostVisibilityUseCase

    @InternalCoroutinesApi
    @Before
    fun setup() {
        getPostVisibilityUseCase = GetPostVisibilityUseCase()
        editPostRepository = EditPostRepository(mock(), mock(), mock(), TEST_DISPATCHER, TEST_DISPATCHER)
    }

    @Test
    fun `If post password is not empty then PASSWORD_PROTECTED should be the visibility`() {
        // arrange
        val post = PostModel().apply {
            val password = "password"
            setPassword(password)
        }
        editPostRepository.set { post }

        // act
        val visibility = getPostVisibilityUseCase.getVisibility(editPostRepository)

        // assert
        assertThat(visibility).isEqualTo(PASSWORD_PROTECTED)
    }

    @Test
    fun `If PostStatus is PUBLISHED then PUBLISH should be the visibility`() {
        // arrange
        val post = PostModel().apply {
            setStatus(PostStatus.PUBLISHED.toString())
        }
        editPostRepository.set { post }

        // act
        val visibility = getPostVisibilityUseCase.getVisibility(editPostRepository)

        // assert
        assertThat(visibility).isEqualTo(PUBLISH)
    }

    @Test
    fun `If PostStatus is DRAFT then DRAFT should be the visibility`() {
        // arrange
        val post = PostModel().apply {
            setStatus(PostStatus.DRAFT.toString())
        }
        editPostRepository.set { post }

        // act
        val visibility = getPostVisibilityUseCase.getVisibility(editPostRepository)

        // assert
        assertThat(visibility).isEqualTo(DRAFT)
    }

    @Test
    fun `If PostStatus is PENDING then PENDING_REVIEW should be the visibility`() {
        // arrange
        val post = PostModel().apply {
            setStatus(PostStatus.PENDING.toString())
        }
        editPostRepository.set { post }

        // act
        val visibility = getPostVisibilityUseCase.getVisibility(editPostRepository)

        // assert
        assertThat(visibility).isEqualTo(PENDING_REVIEW)
    }


    @Test
    fun `If PostStatus is PRIVATE then PRIVATE should be the visibility`() {
        // arrange
        val post = PostModel().apply {
            setStatus(PostStatus.PRIVATE.toString())
        }
        editPostRepository.set { post }

        // act
        val visibility = getPostVisibilityUseCase.getVisibility(editPostRepository)

        // assert
        assertThat(visibility).isEqualTo(PRIVATE)
    }
}
