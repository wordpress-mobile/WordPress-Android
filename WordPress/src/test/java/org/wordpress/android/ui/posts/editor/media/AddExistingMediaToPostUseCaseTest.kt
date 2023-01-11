package org.wordpress.android.ui.posts.editor.media

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.ui.posts.editor.EditorTracker

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class AddExistingMediaToPostUseCaseTest : BaseUnitTest() {
    @Test
    fun `addMediaToEditor called on all models returned from loadMediaByRemoteId`() = test {
        // Arrange
        val remoteIds = listOf(1L, 2L, 3L)
        val getMediaModelUseCase = createGetMediaModelUseCase()
        val appendMediaToEditorUseCase = mock<AppendMediaToEditorUseCase>()
        val captor = argumentCaptor<List<MediaModel>>()
        // Act
        createAddExistingMediaToPostUseCase(
            getMediaModelUseCase = getMediaModelUseCase,
            appendMediaToEditorUseCase = appendMediaToEditorUseCase
        ).addMediaExistingInRemoteToEditorAsync(
            mock(),
            mock(),
            remoteIds,
            mock()
        )
        // Assert
        verify(appendMediaToEditorUseCase).addMediaToEditor(anyOrNull(), captor.capture())
        assertThat(captor.firstValue.map { it.mediaId }).isEqualTo(remoteIds)
    }

    @Test
    fun `syncPostObjectWithUiAndSaveIt is called after addMediaToEditor`() = test {
        // Arrange
        val remoteIds = listOf(1L, 2L, 3L)
        val getMediaModelUseCase = createGetMediaModelUseCase()
        val appendMediaToEditorUseCase = mock<AppendMediaToEditorUseCase>()
        val editorMediaListener = mock<EditorMediaListener>()

        val inOrder = inOrder(appendMediaToEditorUseCase, editorMediaListener)
        // Act
        createAddExistingMediaToPostUseCase(
            getMediaModelUseCase = getMediaModelUseCase,
            appendMediaToEditorUseCase = appendMediaToEditorUseCase
        ).addMediaExistingInRemoteToEditorAsync(
            mock(),
            mock(),
            remoteIds,
            editorMediaListener
        )
        // Assert
        inOrder.verify(appendMediaToEditorUseCase).addMediaToEditor(anyOrNull(), anyOrNull())
        inOrder.verify(editorMediaListener).syncPostObjectWithUiAndSaveIt(anyOrNull())
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun `trackAddMediaEvent called on all models returned from loadMediaByRemoteId`() = test {
        // Arrange
        val remoteIds = listOf(1L, 2L, 3L)
        val editorTracker = mock<EditorTracker>()

        // Act
        createAddExistingMediaToPostUseCase(
            editorTracker = editorTracker
        ).addMediaExistingInRemoteToEditorAsync(
            mock(),
            mock(),
            remoteIds,
            mock()
        )
        // Assert
        verify(editorTracker, times(remoteIds.size)).trackAddMediaEvent(any(), any(), any())
    }

    private companion object Fixtures {
        fun createAddExistingMediaToPostUseCase(
            editorTracker: EditorTracker = mock(),
            getMediaModelUseCase: GetMediaModelUseCase = createGetMediaModelUseCase(),
            appendMediaToEditorUseCase: AppendMediaToEditorUseCase = mock()
        ): AddExistingMediaToPostUseCase {
            return AddExistingMediaToPostUseCase(
                editorTracker,
                getMediaModelUseCase,
                appendMediaToEditorUseCase
            )
        }

        private fun createGetMediaModelUseCase(): GetMediaModelUseCase = mock {
            onBlocking { loadMediaByRemoteId(anyOrNull(), anyOrNull()) } doAnswer { invocation ->
                // Creates dummy media models from provided model ids
                (invocation.getArgument(1) as Iterable<Long>)
                    .map { createMediaModel(it) }
                    .toList()
            }
        }

        fun createMediaModel(mediaModelId: Long) =
            MediaModel().apply {
                this.mediaId = mediaModelId
            }
    }
}
