package org.wordpress.android.ui.reader.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.repository.ReaderPostRepository
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
    lateinit var readerPostRepository: ReaderPostRepository

    @Mock
    lateinit var readerPostTableWrapper: ReaderPostTableWrapper

    @Mock
    lateinit var savedPostsRepository: ReaderSavedPostsRepository

    private val savedChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    @Before
    fun setup() {
        lenient().whenever(savedPostsRepository.changes).thenReturn(savedChanges)
        useCase = ReaderPostBookmarkUseCase(
            readerTracker,
            networkUtilsWrapper,
            readerPostRepository,
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
        verify(readerPostRepository, never()).setBookmarked(any(), any())
        verify(savedPostsRepository, never()).setSaved(any(), any(), anyOrNull())
    }

    @Test
    fun `when synced, saving follows the account state and saves locally and on the account`() = test {
        val dummyPost = init(isBookmarked = false)
        whenever(savedPostsRepository.isEnabled).thenReturn(true)
        whenever(savedPostsRepository.isSaved(dummyPost)).thenReturn(false)
        whenever(savedPostsRepository.setSaved(any(), eq(true), anyOrNull())).thenReturn(true)

        val result = useCase.toggleBookmark(post(), true, SOURCE).toList(mutableListOf())

        verify(readerPostRepository).setBookmarked(dummyPost, true)
        assertThat(result).containsExactly(BookmarkPostState.Success(true))
    }

    @Test
    fun `when synced, the change shows as soon as the write starts, before the server answers`() = test {
        val dummyPost = init(isBookmarked = false)
        whenever(savedPostsRepository.isEnabled).thenReturn(true)
        whenever(savedPostsRepository.isSaved(dummyPost)).thenReturn(false)
        whenever(savedPostsRepository.setSaved(any(), eq(true), anyOrNull())).thenAnswer {
            // what wordpress-rs does when the write starts
            savedChanges.tryEmit(Unit)
            true
        }

        val result = useCase.toggleBookmark(post(), true, SOURCE).toList(mutableListOf())

        assertThat(result).containsExactly(BookmarkPostState.Started(true), BookmarkPostState.Success(true))
    }

    @Test
    fun `when synced, unsaving a post saved elsewhere doesn't touch the local flag that's already clear`() = test {
        val dummyPost = init(isBookmarked = false)
        whenever(savedPostsRepository.isEnabled).thenReturn(true)
        whenever(savedPostsRepository.isSaved(dummyPost)).thenReturn(true)
        whenever(savedPostsRepository.setSaved(any(), eq(false), anyOrNull())).thenReturn(true)

        val result = useCase.toggleBookmark(post(), true, SOURCE).toList(mutableListOf())

        verify(readerPostRepository, never()).setBookmarked(any(), any())
        assertThat(result).containsExactly(BookmarkPostState.Success(false))
    }

    @Test
    fun `when synced and the account write fails, the local flag is untouched and Failed is emitted`() = test {
        val dummyPost = init(isBookmarked = false)
        whenever(savedPostsRepository.isEnabled).thenReturn(true)
        whenever(savedPostsRepository.isSaved(dummyPost)).thenReturn(false)
        whenever(savedPostsRepository.setSaved(any(), eq(true), anyOrNull())).thenReturn(false)

        val result = useCase.toggleBookmark(post(), false, SOURCE).toList(mutableListOf())

        verify(readerPostRepository, never()).setBookmarked(any(), any())
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
        verify(readerPostRepository).setBookmarked(dummyPost, true)
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
        verify(readerPostRepository).setBookmarked(dummyPost, false)
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
