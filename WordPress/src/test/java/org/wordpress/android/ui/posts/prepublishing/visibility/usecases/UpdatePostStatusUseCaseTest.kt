package org.wordpress.android.ui.posts.prepublishing.visibility.usecases

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
import org.wordpress.android.fluxc.model.post.PostStatus.PRIVATE
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.util.DateTimeUtilsWrapper

class UpdatePostStatusUseCaseTest : BaseUnitTest() {
    private lateinit var editPostRepository: EditPostRepository
    private lateinit var updatePostStatusUseCase: UpdatePostStatusUseCase
    @Mock lateinit var dateTimeUtilsWrapper: DateTimeUtilsWrapper

    @InternalCoroutinesApi
    @Before
    fun setup() {
        updatePostStatusUseCase = UpdatePostStatusUseCase(dateTimeUtilsWrapper)
        editPostRepository = EditPostRepository(mock(), mock(), mock(), TEST_DISPATCHER, TEST_DISPATCHER)
    }

    @Test
    fun `if the new PostStatus is PRIVATE & the old PostStatus is SCHEDULED then the date created should be now`() {
        // arrange
        val currentDate = "2020-06-06T20:28:20+0200"
        whenever(dateTimeUtilsWrapper.currentTimeInIso8601()).thenReturn(currentDate)
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
}
