package org.wordpress.android.ui.uploads

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload

@RunWith(MockitoJUnitRunner::class)
class PostMediaHandlerTest {
    @Mock
    lateinit var mediaStore: MediaStore
    @Mock
    lateinit var dispatcher: Dispatcher
    private lateinit var postMediaHandler: PostMediaHandler
    private val site = SiteModel()
    private lateinit var post: PostModel
    private lateinit var firstMediaItem: MediaModel
    private lateinit var secondMediaItem: MediaModel
    private val actions = mutableListOf<Action<MediaPayload>>()

    @Before
    fun setUp() {
        postMediaHandler = PostMediaHandler(mediaStore, dispatcher)
        post = PostModel()
        firstMediaItem = MediaModel()
        secondMediaItem = MediaModel()
        actions.clear()
        doAnswer {
            actions.add(it.getArgument(0))
        }.whenever(dispatcher).dispatch(any())
    }

    @Test
    fun `emits media update action when media post ID is 0`() {
        // Arrange
        whenever(mediaStore.getMediaForPost(post)).thenReturn(listOf(firstMediaItem))
        val updatedPostId = 1L
        post.setRemotePostId(updatedPostId)
        firstMediaItem.postId = 0L

        // Act
        postMediaHandler.updateMediaWithoutPostId(site, post)

        // Assert
        verify(dispatcher).dispatch(any())
        assertThat(actions).hasSize(1)
        val emittedAction = actions.last()
        assertThat(emittedAction.payload.media.postId).isEqualTo(updatedPostId)
        assertThat(emittedAction.payload.site).isEqualTo(site)
    }

    @Test
    fun `emits media update action for multiple items`() {
        // Arrange
        whenever(mediaStore.getMediaForPost(post)).thenReturn(listOf(firstMediaItem, secondMediaItem))
        val updatedPostId = 1L
        post.setRemotePostId(updatedPostId)
        firstMediaItem.postId = 0L
        firstMediaItem.mediaId = 11L
        secondMediaItem.postId = 0L
        secondMediaItem.mediaId = 22L

        // Act
        postMediaHandler.updateMediaWithoutPostId(site, post)

        // Assert
        verify(dispatcher, times(2)).dispatch(any())
        assertThat(actions).hasSize(2)
        val firstAction = actions[0]
        assertThat(firstAction.payload.media.mediaId).isEqualTo(firstMediaItem.mediaId)
        assertThat(firstAction.payload.media.postId).isEqualTo(updatedPostId)
        assertThat(firstAction.payload.site).isEqualTo(site)
        val secondAction = actions[1]
        assertThat(secondAction.payload.media.mediaId).isEqualTo(secondMediaItem.mediaId)
        assertThat(secondAction.payload.media.postId).isEqualTo(updatedPostId)
        assertThat(secondAction.payload.site).isEqualTo(site)
    }

    @Test
    fun `does not emit when post remote ID is 0`() {
        // Arrange
        post.setRemotePostId(0L)
        firstMediaItem.postId = 0L

        // Act
        postMediaHandler.updateMediaWithoutPostId(site, post)

        // Assert
        verifyNoInteractions(dispatcher)
    }

    @Test
    fun `does not update media when media remote ID is not 0`() {
        // Arrange
        whenever(mediaStore.getMediaForPost(post)).thenReturn(listOf(firstMediaItem))
        val updatedPostId = 1L
        post.setRemotePostId(updatedPostId)
        firstMediaItem.postId = 2L

        // Act
        postMediaHandler.updateMediaWithoutPostId(site, post)

        // Assert
        verifyNoInteractions(dispatcher)
    }
}
