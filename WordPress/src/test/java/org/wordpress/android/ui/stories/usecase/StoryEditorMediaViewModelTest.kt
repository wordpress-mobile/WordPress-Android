package org.wordpress.android.ui.stories.usecase

import android.net.Uri
import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.test
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.editor.media.AddExistingMediaToPostUseCase
import org.wordpress.android.ui.posts.editor.media.AddLocalMediaToPostUseCase
import org.wordpress.android.ui.posts.editor.media.EditorMediaListener
import org.wordpress.android.ui.stories.media.StoryEditorMediaViewModel
import org.wordpress.android.ui.stories.media.StoryEditorMediaViewModel.AddMediaToStoryPostUiState
import org.wordpress.android.util.MediaUtilsWrapper
import org.wordpress.android.viewmodel.Event

@UseExperimental(InternalCoroutinesApi::class)
class StoryEditorMediaViewModelTest : BaseUnitTest() {
    @Test
    fun `advertiseImageOptimisationAndAddMedia shows dialog when shouldAdvertiseImageOptimization is true`() {
        // Arrange
        val editorMediaListener = mock<EditorMediaListener>()
        val mediaUtilsWrapper = createMediaUtilsWrapper(shouldAdvertiseImageOptimization = true)

        // Act
        createStoryEditorMediaViewModel(
                editorMediaListener = editorMediaListener,
                mediaUtilsWrapper = mediaUtilsWrapper
        ).advertiseImageOptimisationAndAddMedia(mock())

        // Assert
        verify(editorMediaListener).advertiseImageOptimization(anyOrNull())
    }

    @Test
    fun `advertiseImageOptimisationAndAddMedia does NOT show dialog when shouldAdvertiseImageOptimization is false`() {
        // Arrange
        val editorMediaListener = mock<EditorMediaListener>()
        val mediaUtilsWrapper = createMediaUtilsWrapper(shouldAdvertiseImageOptimization = false)

        // Act
        createStoryEditorMediaViewModel(
                editorMediaListener = editorMediaListener,
                mediaUtilsWrapper = mediaUtilsWrapper
        )
                .advertiseImageOptimisationAndAddMedia(mock())
        // Assert
        verify(editorMediaListener, never()).advertiseImageOptimization(anyOrNull())
    }

    @Test
    fun `addNewMediaItemsToEditorAsync emits AddingSingleMedia for a single uri`() = test {
        // Arrange
        val storyEditorMediaViewModel = createStoryEditorMediaViewModel()
        val captor = argumentCaptor<AddMediaToStoryPostUiState>()
        val observer: Observer<AddMediaToStoryPostUiState> = mock()
        storyEditorMediaViewModel.uiState.observeForever(observer)

        // Act
        storyEditorMediaViewModel.addNewMediaItemsToEditorAsync(mock(), false)

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
        val storyEditorMediaViewModel =
                createStoryEditorMediaViewModel(addLocalMediaToPostUseCase = addLocalMediaToPostUseCase)

        val captor = argumentCaptor<Event<SnackbarMessageHolder>>()
        val observer: Observer<Event<SnackbarMessageHolder>> = mock()
        storyEditorMediaViewModel.snackBarMessage.observeForever(observer)

        // Act
        storyEditorMediaViewModel.addNewMediaItemsToEditorAsync(mock(), false)

        // Assert
        verify(observer, times(1)).onChanged(captor.capture())
        assertThat(captor.firstValue.getContentIfNotHandled()?.messageRes).isEqualTo(R.string.gallery_error)
    }

    @Test
    fun `addNewMediaItemsToEditorAsync does NOT show snackbar when all media succeed`() = test {
        // Arrange
        val addLocalMediaToPostUseCase = createAddLocalMediaToPostUseCase(
                resultForAddNewMediaToEditorAsync = true
        )
        val storyEditorMediaViewModel =
                createStoryEditorMediaViewModel(addLocalMediaToPostUseCase = addLocalMediaToPostUseCase)

        val captor = argumentCaptor<Event<SnackbarMessageHolder>>()
        val observer: Observer<Event<SnackbarMessageHolder>> = mock()
        storyEditorMediaViewModel.snackBarMessage.observeForever(observer)

        // Act
        storyEditorMediaViewModel.addNewMediaItemsToEditorAsync(mock(), false)
        // Assert
        verify(observer, never()).onChanged(captor.capture())
    }

    @Test
    fun `onPhotoPickerMediaChosen does NOT invoke shouldAdvertiseImageOptimization when only video files`() =
            test {
                // Arrange
                val uris = listOf(VIDEO_URI, VIDEO_URI, VIDEO_URI, VIDEO_URI)
                val editorMediaListener = mock<EditorMediaListener>()

                val mediaUtilsWrapper = createMediaUtilsWrapper()

                // Act
                createStoryEditorMediaViewModel(
                        mediaUtilsWrapper = mediaUtilsWrapper,
                        editorMediaListener = editorMediaListener
                )
                        .onPhotoPickerMediaChosen(uris)
                // Assert
                verify(editorMediaListener, never()).advertiseImageOptimization(anyOrNull())
                verify(mediaUtilsWrapper, never()).shouldAdvertiseImageOptimization()
            }

    @Test
    fun `onPhotoPickerMediaChosen invokes shouldAdvertiseImageOptimization when at least 1 image file`() =
            test {
                // Arrange
                val uris = listOf(VIDEO_URI, VIDEO_URI, IMAGE_URI, VIDEO_URI)
                val editorMediaListener = mock<EditorMediaListener>()

                val mediaUtilsWrapper = createMediaUtilsWrapper()

                // Act
                createStoryEditorMediaViewModel(
                        mediaUtilsWrapper = mediaUtilsWrapper,
                        editorMediaListener = editorMediaListener
                )
                        .onPhotoPickerMediaChosen(uris)
                // Assert
                verify(mediaUtilsWrapper).shouldAdvertiseImageOptimization()
            }

    private companion object Fixtures {
        private val VIDEO_URI = mock<Uri>()
        private val IMAGE_URI = mock<Uri>()

        fun createStoryEditorMediaViewModel(
            mediaUtilsWrapper: MediaUtilsWrapper = createMediaUtilsWrapper(),
            addLocalMediaToPostUseCase: AddLocalMediaToPostUseCase = createAddLocalMediaToPostUseCase(),
            addExistingMediaToPostUseCase: AddExistingMediaToPostUseCase = mock(),
            siteModel: SiteModel = mock(),
            editorMediaListener: EditorMediaListener = mock()
        ): StoryEditorMediaViewModel {
            val editorMedia = StoryEditorMediaViewModel(
                    mediaUtilsWrapper,
                    addLocalMediaToPostUseCase,
                    addExistingMediaToPostUseCase,
                    TEST_DISPATCHER
            )
            editorMedia.start(siteModel, editorMediaListener)
            return editorMedia
        }

        fun createMediaUtilsWrapper(
            shouldAdvertiseImageOptimization: Boolean = false
        ) =
                mock<MediaUtilsWrapper> {
                    on { shouldAdvertiseImageOptimization() }
                            .thenReturn(shouldAdvertiseImageOptimization)
                    on { isVideo(VIDEO_URI.toString()) }.thenReturn(true)
                    on { isVideo(IMAGE_URI.toString()) }.thenReturn(false)
                }

        fun createAddLocalMediaToPostUseCase(resultForAddNewMediaToEditorAsync: Boolean = true) =
                mock<AddLocalMediaToPostUseCase> {
                    onBlocking {
                        addNewMediaToEditorAsync(
                                anyOrNull(),
                                anyOrNull(),
                                anyBoolean(),
                                anyOrNull(),
                                anyBoolean()
                        )
                    }.thenReturn(resultForAddNewMediaToEditorAsync)
                }
    }
}
