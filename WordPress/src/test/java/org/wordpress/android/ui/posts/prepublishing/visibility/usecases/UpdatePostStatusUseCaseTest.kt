package org.wordpress.android.ui.posts.prepublishing.visibility.usecases

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.PRIVATE
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.PostUtilsWrapper
import org.wordpress.android.util.DateTimeUtilsWrapper

class UpdatePostStatusUseCaseTest : BaseUnitTest() {
    private lateinit var editPostRepository: EditPostRepository
    private lateinit var updatePostStatusUseCase: UpdatePostStatusUseCase
    @Mock lateinit var dateTimeUtilsWrapper: DateTimeUtilsWrapper
    @Mock lateinit var postUtilsWrapper: PostUtilsWrapper

    @InternalCoroutinesApi
    @Before
    fun setup() {
        updatePostStatusUseCase = UpdatePostStatusUseCase(dateTimeUtilsWrapper, postUtilsWrapper)
        editPostRepository = EditPostRepository(mock(), mock(), mock(), TEST_DISPATCHER, TEST_DISPATCHER)
    }

    @Test
    fun `if the new PostStatus is PRIVATE & the old PostStatus is SCHEDULED then the date created should be now`() {
        // arrange
        val currentDate = "2020-06-06T20:28:20+0200"
        whenever(dateTimeUtilsWrapper.currentTimeInIso8601()).thenReturn(currentDate)
        whenever(postUtilsWrapper.isPublishDateInTheFuture(any())).thenReturn(true)
        editPostRepository.set { PostModel().apply { setStatus(PostStatus.SCHEDULED.toString()) } }

        // act
        updatePostStatusUseCase.updatePostStatus(PRIVATE, editPostRepository) {}

        // assert
        assertThat(editPostRepository.dateCreated).isEqualTo(currentDate)
    }

    @Test
    fun `if the new PostStatus is PRIVATE & the old PostStatus is not SCHEDULED then the date should be the same`() {
        // arrange
        val currentDate = "2020-06-06T20:28:20+0200"
        editPostRepository.set {
            PostModel().apply {
                setDateCreated(currentDate)
                setStatus(PostStatus.DRAFT.toString())
            }
        }
        whenever(postUtilsWrapper.isPublishDateInTheFuture(any())).thenReturn(false)


        // act
        updatePostStatusUseCase.updatePostStatus(PRIVATE, editPostRepository) {}

        // assert
        assertThat(editPostRepository.dateCreated).isEqualTo(currentDate)
    }

    @Test
    fun `if the new PostStatus is PRIVATE & the old PostStatus is DRAFT then the postModel should be updated`() {
        // arrange
        editPostRepository.set {
            PostModel().apply {
                setStatus(PostStatus.DRAFT.toString())
            }
        }

        // act
        updatePostStatusUseCase.updatePostStatus(PRIVATE, editPostRepository) {}

        // assert
        assertThat(editPostRepository.status).isEqualTo(PRIVATE)
    }

    @Test
    fun `if the new PostStatus is PRIVATE & the old PostStatus is PENDING & date is in future then the date created should be now`() {
        // arrange
        val currentDate = "2020-06-06T20:28:20+0200"
        whenever(dateTimeUtilsWrapper.currentTimeInIso8601()).thenReturn(currentDate)
        whenever(postUtilsWrapper.isPublishDateInTheFuture(any())).thenReturn(true)
        editPostRepository.set { PostModel().apply { setStatus(PostStatus.PENDING.toString()) } }

        // act
        updatePostStatusUseCase.updatePostStatus(PRIVATE, editPostRepository) {}

        // assert
        assertThat(editPostRepository.dateCreated).isEqualTo(currentDate)
    }

    @Test
    fun `if the new PostStatus is PRIVATE & the old PostStatus is PENDING & date is not in future then the date created should be the same`() {
        // arrange
        val dateCreated = "2020-06-06T20:28:20+0200"
        whenever(postUtilsWrapper.isPublishDateInTheFuture(any())).thenReturn(false)
        editPostRepository.set {
            PostModel().apply {
                setDateCreated(dateCreated)
                setStatus(PostStatus.PENDING.toString())
            }
        }

        // act
        updatePostStatusUseCase.updatePostStatus(PRIVATE, editPostRepository) {}

        // assert
        assertThat(editPostRepository.dateCreated).isEqualTo(dateCreated)
    }
}
