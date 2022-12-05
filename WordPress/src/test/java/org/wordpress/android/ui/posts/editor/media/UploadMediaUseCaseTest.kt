package org.wordpress.android.ui.posts.editor.media

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.FAILED
import org.wordpress.android.ui.posts.EditPostActivity.OnPostUpdatedFromUIListener
import org.wordpress.android.ui.uploads.UploadServiceFacade

@RunWith(MockitoJUnitRunner::class)
class UploadMediaUseCaseTest {
    @Test(expected = IllegalArgumentException::class)
    fun `Starting an upload with a not-queued post throws an exception`() {
        // Arrange
        val models = listOf(createMediaModel(uploadState = FAILED))

        // Act
        createUploadMediaUseCase().saveQueuedPostAndStartUpload(mock(), models)

        // Assert
        // results in exception
    }

    @Test
    fun `Post is synced and saved before initiating an upload`() {
        // Arrange
        val models = listOf(createMediaModel(), createMediaModel())
        val editorMediaListener = createEditorMediaListener()

        // Act
        createUploadMediaUseCase().saveQueuedPostAndStartUpload(editorMediaListener, models)

        // Assert
        verify(editorMediaListener).syncPostObjectWithUiAndSaveIt(any())
    }

    @Test
    fun `Upload is initiated after the post is saved`() {
        // Arrange
        val models = listOf(createMediaModel(), createMediaModel())
        val uploadServiceFacade: UploadServiceFacade = mock()

        // Act
        createUploadMediaUseCase(uploadServiceFacade = uploadServiceFacade).saveQueuedPostAndStartUpload(
                createEditorMediaListener(),
                models
        )

        // Assert
        verify(uploadServiceFacade).uploadMediaFromEditor(any())
    }

    @Test
    fun `Upload is initiated with all the provided media models`() {
        // Arrange
        val models = listOf(createMediaModel(), createMediaModel())
        val modelListCaptor: KArgumentCaptor<ArrayList<MediaModel>> = argumentCaptor()
        val uploadServiceFacade: UploadServiceFacade = mock()

        // Act
        createUploadMediaUseCase(uploadServiceFacade = uploadServiceFacade).saveQueuedPostAndStartUpload(
                createEditorMediaListener(),
                models
        )

        // Assert
        verify(uploadServiceFacade).uploadMediaFromEditor(modelListCaptor.capture())
        assertThat(modelListCaptor.firstValue).isEqualTo(models)
    }

    private companion object Fixtures {
        private fun createUploadMediaUseCase(uploadServiceFacade: UploadServiceFacade = mock()): UploadMediaUseCase {
            return UploadMediaUseCase(uploadServiceFacade)
        }

        private fun createEditorMediaListener() = mock<EditorMediaListener> {
            on { syncPostObjectWithUiAndSaveIt(any()) }.thenAnswer { invocation ->
                (invocation.getArgument(0) as OnPostUpdatedFromUIListener).onPostUpdatedFromUI(null)
            }
        }

        private fun createMediaModel(uploadState: MediaUploadState = MediaUploadState.QUEUED) =
                MediaModel().apply { this.uploadState = uploadState.name }
    }
}
