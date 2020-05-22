package org.wordpress.android.ui.posts.prepublishing.home.usecases

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.wordpress.android.BaseUnitTest

import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.util.DateTimeUtilsWrapper

class PublishPostImmediatelyUseCaseTest : BaseUnitTest() {
    private lateinit var useCase: PublishPostImmediatelyUseCase
    private lateinit var editPostRepository: EditPostRepository

    @Mock lateinit var dateTimeUtilsWrapper: DateTimeUtilsWrapper

    @InternalCoroutinesApi
    @Before
    fun setup() {
        useCase = PublishPostImmediatelyUseCase(dateTimeUtilsWrapper)
        editPostRepository = EditPostRepository(mock(), mock(), mock(), TEST_DISPATCHER, TEST_DISPATCHER)
        editPostRepository.set { PostModel() }
    }

    @Test
    fun `if newPost is true then the PostStatus should be a DRAFT`() {
        // arrange
        val isNewPost = true
        val expectedPostStatus = PostStatus.DRAFT

        // act
        useCase.updatePostToPublishImmediately(editPostRepository, isNewPost) {}

        assertThat(editPostRepository.status.toString()).isEqualTo(expectedPostStatus.toString())
    }

    @Test
    fun `if newPost is false then the PostStatus should be a PUBLISHED`() {
        // arrange
        val isNewPost = false
        val expectedPostStatus = PostStatus.PUBLISHED

        // act
        useCase.updatePostToPublishImmediately(editPostRepository, isNewPost) {}

        assertThat(editPostRepository.status.toString()).isEqualTo(expectedPostStatus.toString())
    }

    @Test
    fun `EditPostRepository's PostModel should be set with the currentDate`() {
        // arrange
        val currentDate = "2020-05-05T20:33:20+0200"
        whenever(dateTimeUtilsWrapper.currentTimeInIso8601()).thenReturn(currentDate)

        // act
        useCase.updatePostToPublishImmediately(editPostRepository, false) {}

        assertThat(editPostRepository.dateCreated).isEqualTo(currentDate)
    }
}
