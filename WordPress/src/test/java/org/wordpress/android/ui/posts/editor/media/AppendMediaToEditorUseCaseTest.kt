package org.wordpress.android.ui.posts.editor.media

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.util.FluxCUtilsWrapper
import org.wordpress.android.util.helpers.MediaFile

@RunWith(MockitoJUnitRunner::class)
class AppendMediaToEditorUseCaseTest {
    @Test
    fun `invokes appendMediaFile for all mediaModels`() {
        // Arrange
        val mediaModels = listOf(
            createMediaModel(url = "1"),
            createMediaModel(url = "2"),
            createMediaModel(url = "3")
        )
        val editorMediaListener = mock<EditorMediaListener>()
        // Act
        createAppendMediaToEditorUseCase().addMediaToEditor(editorMediaListener, mediaModels)
        // Assert
        verify(editorMediaListener).appendMediaFiles(argThat { this.size == mediaModels.size })
    }

    @Test
    fun `uses url if available over filePath`() {
        // Arrange
        val expectedUrl = "test_url"
        val mediaModels = listOf(createMediaModel(filePath = "test_file_path", url = expectedUrl))
        val editorMediaListener = mock<EditorMediaListener>()
        // Act
        createAppendMediaToEditorUseCase().addMediaToEditor(editorMediaListener, mediaModels)
        // Assert
        verify(editorMediaListener).appendMediaFiles(argThat { this[expectedUrl] != null })
    }

    @Test
    fun `uses filePath if url is null or empty`() {
        // Arrange
        val expectedFilePath = "test_path"
        val mediaModels = listOf(createMediaModel(url = null, filePath = expectedFilePath))
        val editorMediaListener = mock<EditorMediaListener>()
        // Act
        createAppendMediaToEditorUseCase().addMediaToEditor(editorMediaListener, mediaModels)
        // Assert
        verify(editorMediaListener).appendMediaFiles(argThat { this[expectedFilePath] != null })
    }

    @Test
    fun `filters out mediaModel if both url and filepath is null`() {
        // Arrange
        val mediaModels = listOf(createMediaModel(url = null, filePath = null))
        val editorMediaListener = mock<EditorMediaListener>()
        // Act
        createAppendMediaToEditorUseCase().addMediaToEditor(editorMediaListener, mediaModels)
        // Assert
        verify(editorMediaListener).appendMediaFiles(argThat { this.isEmpty() })
    }

    @Test
    fun `invokes appendMediaFile with mediafile retrieved from mediaFileFromMediaModel`() {
        // Arrange
        val url = "expected_url"
        val mediaModels = listOf(createMediaModel(url = url))
        val mediaFile = mock<MediaFile>()
        val fluxCUtilsWrapper = createFluxCUtilsWrapper(mediaFile)

        val editorMediaListener = mock<EditorMediaListener>()
        // Act
        createAppendMediaToEditorUseCase(fluxCUtilsWrapper = fluxCUtilsWrapper)
            .addMediaToEditor(editorMediaListener, mediaModels)
        // Assert
        verify(editorMediaListener).appendMediaFiles(argThat { this[url] == mediaFile })
    }

    @Test
    fun `filters out mediaModel if mediaFileFromMediaModel returns null`() {
        // Arrange
        val mediaModels = listOf(createMediaModel())
        val fluxCUtilsWrapper = createFluxCUtilsWrapper(null)

        val editorMediaListener = mock<EditorMediaListener>()
        // Act
        createAppendMediaToEditorUseCase(fluxCUtilsWrapper = fluxCUtilsWrapper)
            .addMediaToEditor(editorMediaListener, mediaModels)
        // Assert
        verify(editorMediaListener).appendMediaFiles(argThat { this.isEmpty() })
    }

    private companion object Fixtures {
        fun createAppendMediaToEditorUseCase(
            fluxCUtilsWrapper: FluxCUtilsWrapper = createFluxCUtilsWrapper()
        ): AppendMediaToEditorUseCase {
            return AppendMediaToEditorUseCase(fluxCUtilsWrapper)
        }

        fun createFluxCUtilsWrapper(mediaFile: MediaFile? = mock()) = mock<FluxCUtilsWrapper> {
            on { mediaFileFromMediaModel(any()) }.thenReturn(mediaFile)
        }

        fun createMediaModel(url: String? = "", filePath: String? = ""): MediaModel {
            return MediaModel().apply {
                this.url = url
                this.filePath = filePath
            }
        }
    }
}
