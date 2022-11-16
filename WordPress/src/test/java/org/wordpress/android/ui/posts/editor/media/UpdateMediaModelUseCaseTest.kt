package org.wordpress.android.ui.posts.editor.media

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.MediaAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState
import org.wordpress.android.fluxc.model.PostImmutableModel

@RunWith(MockitoJUnitRunner::class)
class UpdateMediaModelUseCaseTest {
    @Test
    fun `Update media action is dispatched`() {
        // Arrange
        val mediaModel = createMediaModel()
        val captor: KArgumentCaptor<Action<MediaModel>> = argumentCaptor()
        val dispatcher: Dispatcher = mock()

        // Act
        createUpdateMediaModelUseCase(dispatcher).updateMediaModel(mediaModel, mock(), mock())
        verify(dispatcher).dispatch(captor.capture())

        // Assert
        assertThat(captor.firstValue.type).isEqualTo(MediaAction.UPDATE_MEDIA)
        assertThat(captor.firstValue.payload).isEqualTo(mediaModel)
    }

    @Test
    fun `Remote post id gets set`() {
        // Arrange
        val mediaModel = createMediaModel()
        val postData = createPostData(remoteId = REMOTE_ID)

        // Act
        createUpdateMediaModelUseCase().updateMediaModel(mediaModel, postData, mock())

        // Assert
        assertThat(mediaModel.postId).isEqualTo(REMOTE_ID)
    }

    @Test
    fun `Local post id gets set`() {
        // Arrange
        val mediaModel = createMediaModel()
        val postData = createPostData(localId = LOCAL_ID)

        // Act
        createUpdateMediaModelUseCase().updateMediaModel(mediaModel, postData, mock())

        // Assert
        assertThat(mediaModel.localPostId).isEqualTo(LOCAL_ID)
    }

    @Test
    fun `Upload state gets set`() {
        // Arrange
        val mediaModel = createMediaModel()
        val uploadState = MediaUploadState.FAILED

        // Act
        createUpdateMediaModelUseCase().updateMediaModel(mediaModel, mock(), uploadState)

        // Assert
        assertThat(mediaModel.uploadState).isEqualTo(uploadState.name)
    }

    private companion object Fixtures {
        private const val REMOTE_ID = 999L
        private const val LOCAL_ID = 111

        private fun createUpdateMediaModelUseCase(dispatcher: Dispatcher = mock()): UpdateMediaModelUseCase {
            return UpdateMediaModelUseCase(dispatcher)
        }

        fun createPostData(localId: Int = LOCAL_ID, remoteId: Long = REMOTE_ID) =
                mock<PostImmutableModel> {
                    on { remotePostId }.thenReturn(remoteId)
                    on { id }.thenReturn(localId)
                }

        fun createMediaModel(uploadState: MediaUploadState = MediaUploadState.QUEUED) =
                MediaModel().apply {
                    this.uploadState = uploadState.name
                    this.id = 1
                    this.postId = 2
                    this.localPostId = 3
                }
    }
}
