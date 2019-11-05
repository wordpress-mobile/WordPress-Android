package org.wordpress.android.ui.posts.editor.media

import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.MediaAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState

@RunWith(MockitoJUnitRunner::class)
class UpdateMediaModelUseCaseTest {
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var postData: EditorMediaPostData

    private lateinit var updateMediaUseCase: UpdateMediaModelUseCase
    @Before
    fun setUp() {
        updateMediaUseCase = UpdateMediaModelUseCase(dispatcher)
    }

    @Test
    fun `Update media action is dispatched`() {
        // Arrange
        val mediaModel = createMediaModel()
        val captor: KArgumentCaptor<Action<MediaModel>> = argumentCaptor()

        // Act
        updateMediaUseCase.updateMediaModel(mediaModel, mock(), mock())
        verify(dispatcher).dispatch(captor.capture())

        // Assert
        assertThat(captor.firstValue.type).isEqualTo(MediaAction.UPDATE_MEDIA)
        assertThat(captor.firstValue.payload).isEqualTo(mediaModel)
    }

    @Test
    fun `Remote post id gets set`() {
        // Arrange
        val mediaModel = createMediaModel()
        val id = 999L
        whenever(postData.remotePostId).thenReturn(id)

        // Act
        updateMediaUseCase.updateMediaModel(mediaModel, postData, mock())

        // Assert
        assertThat(mediaModel.postId).isEqualTo(id)
    }

    @Test
    fun `Local post id gets set`() {
        // Arrange
        val mediaModel = createMediaModel()
        val id = 123
        whenever(postData.localPostId).thenReturn(id)

        // Act
        updateMediaUseCase.updateMediaModel(mediaModel, postData, mock())

        // Assert
        assertThat(mediaModel.localPostId).isEqualTo(id)
    }

    @Test
    fun `Upload state gets set`() {
        // Arrange
        val mediaModel = createMediaModel()
        val uploadState = MediaUploadState.FAILED

        // Act
        updateMediaUseCase.updateMediaModel(mediaModel, postData, uploadState)

        // Assert
        assertThat(mediaModel.uploadState).isEqualTo(uploadState.name)
    }

    private fun createMediaModel(uploadState: MediaUploadState = MediaUploadState.QUEUED) =
            MediaModel().apply {
                this.uploadState = uploadState.name
                this.id = 1
                this.postId = 2
                this.localPostId = 3
            }
}
