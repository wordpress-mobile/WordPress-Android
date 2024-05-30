package org.wordpress.android.ui.reader.sources

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.only
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult
import org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderPostLocalSourceTest : BaseUnitTest() {
    @Mock
    lateinit var readerPostTableWrapper: ReaderPostTableWrapper

    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    private lateinit var localSource: ReaderPostLocalSource

    @Before
    fun setUp() {
        localSource = ReaderPostLocalSource(readerPostTableWrapper, appPrefsWrapper)
    }

    @Test
    fun `given no changes and no tag provided, when saveUpdatedPosts, then do nothing`() {
        // Given
        val serverPosts = ReaderPostList()
        val requestedTag = null
        whenever(readerPostTableWrapper.comparePosts(serverPosts)).thenReturn(UpdateResult.UNCHANGED)

        // it doesn't matter which update action was used, so let's test all of them
        ReaderPostServiceStarter.UpdateAction.values().forEach { updateAction ->
            clearInvocations(readerPostTableWrapper)

            // When
            val result = localSource.saveUpdatedPosts(serverPosts, updateAction, requestedTag)

            // Then
            verify(readerPostTableWrapper, only()).comparePosts(serverPosts) // only comparePosts should be

            assertThat(result).isEqualTo(UpdateResult.UNCHANGED)
        }
    }

    @Test
    fun `given no changes and tag provided, when saveUpdatedPosts, then do nothing`() {
        // Given
        val serverPosts = ReaderPostList()
        val requestedTag = ReaderTag("tag", "tag", "tag", "endpoint", ReaderTagType.FOLLOWED)

        whenever(readerPostTableWrapper.comparePosts(serverPosts)).thenReturn(UpdateResult.UNCHANGED)

        // if the action is any but REQUEST_OLDER_THAN_GAP we should not do anything
        ReaderPostServiceStarter.UpdateAction.values()
            .filterNot { it == ReaderPostServiceStarter.UpdateAction.REQUEST_OLDER_THAN_GAP }
            .forEach { updateAction ->
                clearInvocations(readerPostTableWrapper)

                // When
                val result = localSource.saveUpdatedPosts(serverPosts, updateAction, requestedTag)

                // Then
                verify(readerPostTableWrapper, only()).comparePosts(serverPosts) // only comparePosts should be

                assertThat(result).isEqualTo(UpdateResult.UNCHANGED)
            }
    }

    @Test
    fun `given no changes, tag provided and OLDER_THAN_GAP, when saveUpdatedPosts, then remove gap marker`() {
        // Given
        val serverPosts = ReaderPostList()
        val requestedTag = ReaderTag("tag", "tag", "tag", "endpoint", ReaderTagType.FOLLOWED)

        whenever(readerPostTableWrapper.comparePosts(serverPosts)).thenReturn(UpdateResult.UNCHANGED)

        // When
        val result = localSource.saveUpdatedPosts(
            serverPosts,
            ReaderPostServiceStarter.UpdateAction.REQUEST_OLDER_THAN_GAP,
            requestedTag,
        )

        // Then
        verify(readerPostTableWrapper).removeGapMarkerForTag(requestedTag)

        assertThat(result).isEqualTo(UpdateResult.UNCHANGED)
    }

    @Test
    fun `given new posts and no tag provided, when saveUpdatedPosts, then save posts`() {
        // Given
        val serverPosts = ReaderPostList()
        val requestedTag = null
        whenever(readerPostTableWrapper.comparePosts(serverPosts)).thenReturn(UpdateResult.HAS_NEW)

        // it doesn't matter which update action was used, so let's test all of them
        ReaderPostServiceStarter.UpdateAction.values().forEach { updateAction ->
            clearInvocations(readerPostTableWrapper)

            // When
            val result = localSource.saveUpdatedPosts(serverPosts, updateAction, requestedTag)

            // Then
            verify(readerPostTableWrapper).addOrUpdatePosts(requestedTag, serverPosts)

            assertThat(result).isEqualTo(UpdateResult.HAS_NEW)
        }
    }

    @Test
    fun `given posts changed, tag provided and OLDER_THAN_GAP, when saveUpdatedPosts, then remove gap marker`() {
        // Given
        val serverPosts = ReaderPostList()
        val requestedTag = ReaderTag("tag", "tag", "tag", "endpoint", ReaderTagType.FOLLOWED)

        listOf(UpdateResult.CHANGED, UpdateResult.HAS_NEW).forEach { updateResult ->
            whenever(readerPostTableWrapper.comparePosts(serverPosts)).thenReturn(updateResult)
            clearInvocations(readerPostTableWrapper)

            // When
            val result = localSource.saveUpdatedPosts(
                serverPosts,
                ReaderPostServiceStarter.UpdateAction.REQUEST_OLDER_THAN_GAP,
                requestedTag,
            )

            // Then
            verify(readerPostTableWrapper).deletePostsBeforeGapMarkerForTag(requestedTag)
            verify(readerPostTableWrapper).removeGapMarkerForTag(requestedTag)
            verify(readerPostTableWrapper).addOrUpdatePosts(requestedTag, serverPosts)

            assertThat(result).isEqualTo(updateResult)
        }
    }

    @Test
    fun `given posts changed, tag provided and REFRESH, when saveUpdatedPosts, then delete posts and save`() {
        // Given
        val serverPosts = ReaderPostList()
        val requestedTag = ReaderTag("tag", "tag", "tag", "endpoint", ReaderTagType.FOLLOWED)

        listOf(UpdateResult.CHANGED, UpdateResult.HAS_NEW).forEach { updateResult ->
            whenever(readerPostTableWrapper.comparePosts(serverPosts)).thenReturn(updateResult)
            clearInvocations(readerPostTableWrapper)

            // When
            val result = localSource.saveUpdatedPosts(
                serverPosts,
                ReaderPostServiceStarter.UpdateAction.REQUEST_REFRESH,
                requestedTag,
            )

            // Then
            verify(readerPostTableWrapper).deletePostsWithTag(requestedTag)
            verify(readerPostTableWrapper).addOrUpdatePosts(requestedTag, serverPosts)

            assertThat(result).isEqualTo(updateResult)
        }
    }

    @Test
    fun `given posts changed, tag provided and OLDER, when saveUpdatedPosts, then save`() {
        // Given
        val serverPosts = ReaderPostList()
        val requestedTag = ReaderTag("tag", "tag", "tag", "endpoint", ReaderTagType.FOLLOWED)

        listOf(UpdateResult.CHANGED, UpdateResult.HAS_NEW).forEach { updateResult ->
            whenever(readerPostTableWrapper.comparePosts(serverPosts)).thenReturn(updateResult)
            clearInvocations(readerPostTableWrapper)


            // When
            val result = localSource.saveUpdatedPosts(
                serverPosts,
                ReaderPostServiceStarter.UpdateAction.REQUEST_OLDER,
                requestedTag,
            )

            // Then
            verify(readerPostTableWrapper).addOrUpdatePosts(requestedTag, serverPosts)

            assertThat(result).isEqualTo(updateResult)
        }
    }

    @Test
    fun `given posts changed, tag provided and NEWER with no gap, when saveUpdatedPosts, then save`() {
        // Given
        val serverPosts = ReaderPostList().apply {
            repeat(4) { add(mock()) }
        }
        val requestedTag = ReaderTag("tag", "tag", "tag", "endpoint", ReaderTagType.FOLLOWED)

        whenever(readerPostTableWrapper.getNumPostsWithTag(requestedTag)).thenReturn(4)
        whenever(readerPostTableWrapper.hasOverlap(serverPosts, requestedTag)).thenReturn(true)

        listOf(UpdateResult.CHANGED, UpdateResult.HAS_NEW).forEach { updateResult ->
            whenever(readerPostTableWrapper.comparePosts(serverPosts)).thenReturn(updateResult)
            clearInvocations(readerPostTableWrapper)

            // When
            val result = localSource.saveUpdatedPosts(
                serverPosts,
                ReaderPostServiceStarter.UpdateAction.REQUEST_NEWER,
                requestedTag,
            )

            // Then
            verify(readerPostTableWrapper).addOrUpdatePosts(requestedTag, serverPosts)

            assertThat(result).isEqualTo(updateResult)
        }
    }

    @Test
    fun `given posts changed, tag provided and NEWER with gap, when saveUpdatedPosts, then save and set gap`() {
        // Given
        val serverPosts = ReaderPostList().apply {
            repeat(4) { add(mock()) }
        }
        val requestedTag = ReaderTag("tag", "tag", "tag", "endpoint", ReaderTagType.FOLLOWED)

        whenever(readerPostTableWrapper.getNumPostsWithTag(requestedTag)).thenReturn(4)
        whenever(readerPostTableWrapper.hasOverlap(serverPosts, requestedTag)).thenReturn(false)

        listOf(UpdateResult.CHANGED, UpdateResult.HAS_NEW).forEach { updateResult ->
            whenever(readerPostTableWrapper.comparePosts(serverPosts)).thenReturn(updateResult)
            clearInvocations(readerPostTableWrapper)

            // When
            val result = localSource.saveUpdatedPosts(
                serverPosts,
                ReaderPostServiceStarter.UpdateAction.REQUEST_NEWER,
                requestedTag,
            )

            // Then
            verify(readerPostTableWrapper, never()).deletePostsBeforeGapMarkerForTag(requestedTag)
            verify(readerPostTableWrapper, never()).removeGapMarkerForTag(requestedTag)
            verify(readerPostTableWrapper).addOrUpdatePosts(requestedTag, serverPosts)
            verify(readerPostTableWrapper).setGapMarkerForTag(any(), any(), eq(requestedTag))

            assertThat(result).isEqualTo(updateResult)
        }
    }

    @Test
    fun `given posts changed, tag provided and NEWER with gap, when saveUpdatedPosts, then keep 1 gap only and save`() {
        // Given
        val serverPosts = ReaderPostList().apply {
            repeat(4) { add(mock()) }
        }
        val requestedTag = ReaderTag("tag", "tag", "tag", "endpoint", ReaderTagType.FOLLOWED)

        whenever(readerPostTableWrapper.getNumPostsWithTag(requestedTag)).thenReturn(5)
        whenever(readerPostTableWrapper.hasOverlap(serverPosts, requestedTag)).thenReturn(false)
        whenever(readerPostTableWrapper.getGapMarkerIdsForTag(requestedTag)).thenReturn(mock())

        listOf(UpdateResult.CHANGED, UpdateResult.HAS_NEW).forEach { updateResult ->
            whenever(readerPostTableWrapper.comparePosts(serverPosts)).thenReturn(updateResult)
            clearInvocations(readerPostTableWrapper)

            // When
            val result = localSource.saveUpdatedPosts(
                serverPosts,
                ReaderPostServiceStarter.UpdateAction.REQUEST_NEWER,
                requestedTag,
            )

            // Then
            verify(readerPostTableWrapper).deletePostsBeforeGapMarkerForTag(requestedTag)
            verify(readerPostTableWrapper).removeGapMarkerForTag(requestedTag)
            verify(readerPostTableWrapper).addOrUpdatePosts(requestedTag, serverPosts)
            verify(readerPostTableWrapper).setGapMarkerForTag(any(), any(), eq(requestedTag))

            assertThat(result).isEqualTo(updateResult)
        }
    }

    @Test
    fun `given posts changed, tag provided and update bookmark, when saveUpdatedPosts, then update bookmark`() {
        // Given
        val serverPosts = ReaderPostList()
        val requestedTag = ReaderTag("tag", "tag", "tag", "endpoint", ReaderTagType.FOLLOWED)

        listOf(UpdateResult.CHANGED, UpdateResult.HAS_NEW).forEach { updateResult ->
            whenever(readerPostTableWrapper.comparePosts(serverPosts)).thenReturn(updateResult)
            whenever(appPrefsWrapper.shouldUpdateBookmarkPostsPseudoIds(requestedTag)).thenReturn(true)

            // it doesn't matter which update action was used, so let's test all of them
            ReaderPostServiceStarter.UpdateAction.values().forEach { updateAction ->
                clearInvocations(readerPostTableWrapper, appPrefsWrapper)

                // When
                val result = localSource.saveUpdatedPosts(
                    serverPosts,
                    updateAction,
                    requestedTag,
                )

                // Then
                verify(readerPostTableWrapper).addOrUpdatePosts(requestedTag, serverPosts)
                verify(readerPostTableWrapper).updateBookmarkedPostPseudoId(serverPosts)
                verify(appPrefsWrapper).setBookmarkPostsPseudoIdsUpdated()

                assertThat(result).isEqualTo(updateResult)
            }
        }
    }
}
