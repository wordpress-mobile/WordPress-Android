package org.wordpress.android.ui.stories.usecase

import androidx.lifecycle.Observer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.editor.media.AddExistingMediaToPostUseCase
import org.wordpress.android.ui.posts.editor.media.AddLocalMediaToPostUseCase
import org.wordpress.android.ui.posts.editor.media.EditorMediaListener
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.viewmodel.Event

@ExperimentalCoroutinesApi
class StoryEditorMediaTest : BaseUnitTest() {
    @Test
    fun `addNewMediaItemsToEditorAsync emits AddingSingleMedia for a single uri`() = test {
        // Arrange
        val editorMedia = createStoryEditorMedia()
        val captor = argumentCaptor<AddMediaToStoryPostUiState>()
        val observer: Observer<AddMediaToStoryPostUiState> = mock()
        editorMedia.uiState.observeForever(observer)

        // Act
        editorMedia.addNewMediaItemsToEditorAsync(mock(), false)

        // Assert
        verify(observer, times(3)).onChanged(captor.capture())
        assertThat(captor.firstValue).isEqualTo(AddMediaToStoryPostUiState.AddingMediaToStoryIdle)
        assertThat(captor.secondValue).isEqualTo(AddMediaToStoryPostUiState.AddingSingleMediaToStory)
        assertThat(captor.thirdValue).isEqualTo(AddMediaToStoryPostUiState.AddingMediaToStoryIdle)
    }

    @Test
    fun `addNewMediaItemsToEditorAsync shows snackbar when a media fails`() = test {
        // Arrange
        val addLocalMediaToPostUseCase = createAddLocalMediaToPostUseCase(
            resultForAddNewMediaToEditorAsync = false
        )
        val editorMedia = createStoryEditorMedia(addLocalMediaToPostUseCase = addLocalMediaToPostUseCase)

        val captor = argumentCaptor<Event<SnackbarMessageHolder>>()
        val observer: Observer<Event<SnackbarMessageHolder>> = mock()
        editorMedia.snackBarMessage.observeForever(observer)

        // Act
        editorMedia.addNewMediaItemsToEditorAsync(mock(), false)

        // Assert
        verify(observer, times(1)).onChanged(captor.capture())
        val message = captor.firstValue.getContentIfNotHandled()?.message as? UiStringRes
        assertThat(message?.stringRes).isEqualTo(R.string.gallery_error)
    }

    @Test
    fun `addNewMediaItemsToEditorAsync does NOT show snackbar when all media succeed`() = test {
        // Arrange
        val addLocalMediaToPostUseCase = createAddLocalMediaToPostUseCase(
            resultForAddNewMediaToEditorAsync = true
        )
        val editorMedia = createStoryEditorMedia(addLocalMediaToPostUseCase = addLocalMediaToPostUseCase)

        val captor = argumentCaptor<Event<SnackbarMessageHolder>>()
        val observer: Observer<Event<SnackbarMessageHolder>> = mock()
        editorMedia.snackBarMessage.observeForever(observer)

        // Act
        editorMedia.addNewMediaItemsToEditorAsync(mock(), false)
        // Assert
        verify(observer, never()).onChanged(captor.capture())
    }

    private companion object Fixtures {
        fun createStoryEditorMedia(
            addLocalMediaToPostUseCase: AddLocalMediaToPostUseCase = createAddLocalMediaToPostUseCase(),
            addExistingMediaToPostUseCase: AddExistingMediaToPostUseCase = mock(),
            siteModel: SiteModel = mock(),
            editorMediaListener: EditorMediaListener = mock()
        ): StoryEditorMedia {
            val editorMedia = StoryEditorMedia(
                addLocalMediaToPostUseCase,
                addExistingMediaToPostUseCase,
                UnconfinedTestDispatcher()
            )
            editorMedia.start(siteModel, editorMediaListener)
            return editorMedia
        }

        fun createAddLocalMediaToPostUseCase(resultForAddNewMediaToEditorAsync: Boolean = true) =
            mock<AddLocalMediaToPostUseCase> {
                onBlocking {
                    addNewMediaToEditorAsync(
                        anyOrNull(),
                        anyOrNull(),
                        anyBoolean(),
                        anyOrNull(),
                        anyBoolean(),
                        anyBoolean()
                    )
                }.thenReturn(resultForAddNewMediaToEditorAsync)
            }
    }
}
