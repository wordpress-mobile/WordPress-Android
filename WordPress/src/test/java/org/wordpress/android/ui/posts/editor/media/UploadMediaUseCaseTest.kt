package org.wordpress.android.ui.posts.editor.media

import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
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
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.FAILED
import org.wordpress.android.ui.posts.EditPostActivity.AfterSavePostListener
import org.wordpress.android.ui.uploads.UploadServiceFacade

@RunWith(MockitoJUnitRunner::class)
class UploadMediaUseCaseTest {
    @Mock lateinit var uploadServiceFacade: UploadServiceFacade
    @Mock lateinit var editorMediaListener: EditorMediaListener

    private lateinit var uploadMediaUseCase: UploadMediaUseCase

    @Before
    fun setUp() {
        uploadMediaUseCase = UploadMediaUseCase(uploadServiceFacade)
        whenever(editorMediaListener.syncPostObjectWithUiAndSaveIt(any())).thenAnswer { invocation ->
            (invocation.getArgument(0) as AfterSavePostListener).onPostSave()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Starting an upload with a not-queued post throws an exception`() {
        // Arrange
        val models = listOf(createMediaModel(uploadState = FAILED))

        // Act
        uploadMediaUseCase.saveQueuedPostAndStartUpload(mock(), models)

        // Assert
        // results in exception
    }

    @Test
    fun `Post is synced and saved before initiating an upload`() {
        // Arrange
        val models = listOf(createMediaModel(), createMediaModel())

        // Act
        uploadMediaUseCase.saveQueuedPostAndStartUpload(editorMediaListener, models)

        // Assert
        verify(editorMediaListener).syncPostObjectWithUiAndSaveIt(any())
    }

    @Test
    fun `Upload is initiated after the post is saved`() {
        // Arrange
        val models = listOf(createMediaModel(), createMediaModel())

        // Act
        uploadMediaUseCase.saveQueuedPostAndStartUpload(editorMediaListener, models)

        // Assert
        verify(uploadServiceFacade).uploadMediaFromEditor(any())
    }

    @Test
    fun `Upload is initiated with all the provided media models`() {
        // Arrange
        val models = listOf(createMediaModel(), createMediaModel())
        val modelListCaptor: KArgumentCaptor<ArrayList<MediaModel>> = argumentCaptor()

        // Act
        uploadMediaUseCase.saveQueuedPostAndStartUpload(editorMediaListener, models)

        // Assert
        verify(uploadServiceFacade).uploadMediaFromEditor(modelListCaptor.capture())
        assertThat(modelListCaptor.firstValue).isEqualTo(models)
    }

    private fun createMediaModel(uploadState: MediaUploadState = MediaUploadState.QUEUED) =
            MediaModel().apply { this.uploadState = uploadState.name }
}
