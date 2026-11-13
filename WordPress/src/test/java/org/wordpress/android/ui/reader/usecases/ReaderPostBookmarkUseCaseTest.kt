package org.wordpress.android.ui.reader.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.actions.ReaderPostActionsWrapper
import org.wordpress.android.ui.reader.saved.ReaderSavedPostsRepository
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.usecases.BookmarkPostState.PreLoadPostContent
import org.wordpress.android.util.NetworkUtilsWrapper

private const val SOURCE = "source"

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderPostBookmarkUseCaseTest : BaseUnitTest() {
    lateinit var useCase: ReaderPostBookmarkUseCase

    @Mock
    lateinit var readerTracker: ReaderTracker

    @Mock
    lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    @Mock
    lateinit var readerPostActionsWrapper: ReaderPostActionsWrapper

    @Mock
    lateinit var readerPostTableWrapper: ReaderPostTableWrapper

    @Mock
    lateinit var savedPostsRepository: ReaderSavedPostsRepository

    @Before
    fun setup() {
        useCase = ReaderPostBookmarkUseCase(
            readerTracker,
            networkUtilsWrapper,
            readerPostActionsWrapper,
            readerPostTableWrapper,
            savedPostsRepository
        )
    }

    @Test
    fun `when synced and offline, nothing changes and NoNetwork is emitted`() = test {
        whenever(savedPostsRepository.isEnabled).thenReturn(true)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        val result = useCase.toggleBookmark(post(), false, SOURCE).toList(mutableListOf())

        assertThat(result).containsExactly(BookmarkPostState.NoNetwork)
        verify(readerPostActionsWrapper, never()).addToBookmarked(any())
        verify(savedPostsRepository, never()).setSaved(any<ReaderPost>(), any())
    }

    @Test
    fun `when synced, saving follows the account state and saves locally and on the account`() = test {
        val dummyPost = init(isBookmarked = false)
        whenever(savedPostsRepository.isEnabled).thenReturn(true)
        whenever(savedPostsRepository.isSaved(dummyPost)).thenReturn(false)
        whenever(savedPostsRepository.setSaved(any<ReaderPost>(), eq(true))).thenReturn(true)

        val result = useCase.toggleBookmark(post(), true, SOURCE).toList(mutableListOf())

        verify(readerPostActionsWrapper).addToBookmarked(dummyPost)
        assertThat(result).containsExactly(BookmarkPostState.Success(true))
    }

    @Test
    fun `when synced, unsaving a post saved elsewhere doesn't touch the local flag that's already clear`() = test {
        val dummyPost = init(isBookmarked = false)
        whenever(savedPostsRepository.isEnabled).thenReturn(true)
        whenever(savedPostsRepository.isSaved(dummyPost)).thenReturn(true)
        whenever(savedPostsRepository.setSaved(any<ReaderPost>(), eq(false))).thenReturn(true)

        val result = useCase.toggleBookmark(post(), true, SOURCE).toList(mutableListOf())

        verify(readerPostActionsWrapper, never()).removeFromBookmarked(any())
        assertThat(result).containsExactly(BookmarkPostState.Success(false))
    }

    @Test
    fun `when synced and the account write fails, the local flag is restored and Failed is emitted`() = test {
        val dummyPost = init(isBookmarked = false)
        whenever(savedPostsRepository.isEnabled).thenReturn(true)
        whenever(savedPostsRepository.isSaved(dummyPost)).thenReturn(false)
        whenever(savedPostsRepository.setSaved(any<ReaderPost>(), eq(true))).thenReturn(false)

        val result = useCase.toggleBookmark(post(), false, SOURCE).toList(mutableListOf())

        verify(readerPostActionsWrapper).addToBookmarked(dummyPost)
        verify(readerPostTableWrapper).setBookmarkFlag(anyLong(), anyLong(), eq(false))
        assertThat(result).containsExactly(BookmarkPostState.Failed)
    }

    @Test
    fun `bookmark action updates the bookmark state to true`() = test {
        // Arrange
        val dummyPost = init(isBookmarked = false)
        // Act
        useCase.toggleBookmark(
            post(),
            false,
            SOURCE
        ).toList(mutableListOf())

        // Assert
        verify(readerPostActionsWrapper).addToBookmarked(dummyPost)
    }

    @Test
    fun `unbookmark action updates the bookmark state to false`() = test {
        // Arrange
        val dummyPost = init(isBookmarked = true)
        // Act
        useCase.toggleBookmark(
            post(),
            false,
            SOURCE
        ).toList(mutableListOf())

        // Assert
        verify(readerPostActionsWrapper).removeFromBookmarked(dummyPost)
    }

    @Test
    fun `initiates content preload when network available`() = test {
        // Arrange
        init(isBookmarked = false, networkAvailable = true)

        // Act
        val result = useCase.toggleBookmark(
            post(),
            false,
            SOURCE
        ).toList(mutableListOf())

        // Assert
        assertThat(result.contains(PreLoadPostContent(0L, 0L))).isTrue
    }

    @Test
    fun `does not initiate content preload when network not available`() = test {
        // Arrange
        init(isBookmarked = false, networkAvailable = false)

        // Act
        val result = useCase.toggleBookmark(
            post(),
            false,
            SOURCE
        ).toList(mutableListOf())

        // Assert
        assertThat(result.contains(PreLoadPostContent(0L, 0L))).isFalse
    }

    @Test
    fun `does not initiate content preload on unbookmark action`() = test {
        // Arrange
        init(isBookmarked = true)

        // Act
        val result = useCase.toggleBookmark(
            post(),
            false,
            SOURCE
        ).toList(mutableListOf())

        // Assert
        assertThat(result.contains(PreLoadPostContent(0L, 0L))).isFalse
    }

    @Test
    fun `does not initiate content preload when on bookmarkList(savedTab)`() = test {
        // Arrange
        init()

        // Act
        val result = useCase.toggleBookmark(
            post(),
            true,
            SOURCE
        ).toList(mutableListOf())

        // Assert
        assertThat(result.contains(PreLoadPostContent(0L, 0L))).isFalse
    }

    private fun init(isBookmarked: Boolean = false, networkAvailable: Boolean = true): ReaderPost {
        val post = ReaderPost().apply { this.isBookmarked = isBookmarked }
        whenever(readerPostTableWrapper.getBlogPost(anyLong(), anyLong(), anyBoolean()))
            .thenReturn(post)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(networkAvailable)
        return post
    }

    private fun post(): ReaderPost {
        val post = ReaderPost()
        post.postId = 0L
        post.feedId = 0L
        post.postId = 0L
        post.isFollowedByCurrentUser = false
        return post
    }
}
