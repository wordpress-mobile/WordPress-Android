package org.wordpress.android.ui.posts.editor.media

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.editor.EditorMediaUploadListener
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.uploads.UploadServiceFacade

@RunWith(MockitoJUnitRunner::class)
class ReattachUploadingMediaUseCaseTest {
    private lateinit var useCase: ReattachUploadingMediaUseCase
    private val editPostRepository: EditPostRepository = mock()
    private val uploadServiceFacade: UploadServiceFacade = mock()
    private val editorMediaUploadListener: EditorMediaUploadListener = mock()

    private val mediaModel1 = MediaModel().apply { id = 1 }
    private val mediaModel2 = MediaModel().apply { id = 2 }

    @Before
    fun setUp() {
        useCase = ReattachUploadingMediaUseCase(uploadServiceFacade)
    }

    @Test
    fun `Media from both db and uploadHandler get reattached`() {
        // Arrange
        whenever(editPostRepository.getPendingMediaForPost())
                .thenReturn(setOf(mediaModel1))
        whenever(editPostRepository.getPendingOrInProgressMediaUploadsForPost())
                .thenReturn(listOf(mediaModel2))

        // Act
        useCase.reattachUploadingMediaForAztec(editPostRepository, editorMediaUploadListener)

        // Assert
        verify(editorMediaUploadListener).onMediaUploadReattached(mediaModel1.id.toString(), 0f)
        verify(editorMediaUploadListener).onMediaUploadReattached(mediaModel2.id.toString(), 0f)
    }

    @Test
    fun `Media from db and uploadHandler are merged using union strategy`() {
        // Arrange
        whenever(editPostRepository.getPendingMediaForPost())
                .thenReturn(setOf(mediaModel1))
        whenever(editPostRepository.getPendingOrInProgressMediaUploadsForPost())
                .thenReturn(listOf(mediaModel1, mediaModel2))

        // Act
        useCase.reattachUploadingMediaForAztec(editPostRepository, editorMediaUploadListener)

        // Assert
        verify(editorMediaUploadListener, times(1)).onMediaUploadReattached(mediaModel1.id.toString(), 0f)
        verify(editorMediaUploadListener, times(1)).onMediaUploadReattached(mediaModel2.id.toString(), 0f)
    }

    @Test
    fun `Upload progress is propagated to EditorMediaUploadListener`() {
        // Arrange
        whenever(editPostRepository.getPendingMediaForPost())
                .thenReturn(setOf(mediaModel1))
        whenever(editPostRepository.getPendingOrInProgressMediaUploadsForPost())
                .thenReturn(listOf())

        val progress = 15.4f
        whenever(uploadServiceFacade.getUploadProgressForMedia(mediaModel1)).thenReturn(progress)

        // Act
        useCase.reattachUploadingMediaForAztec(editPostRepository, editorMediaUploadListener)

        // Assert
        verify(editorMediaUploadListener, times(1)).onMediaUploadReattached(mediaModel1.id.toString(), progress)
    }
}
