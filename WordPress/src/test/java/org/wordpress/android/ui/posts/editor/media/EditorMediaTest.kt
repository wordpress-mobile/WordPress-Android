package org.wordpress.android.ui.posts.editor.media

import android.net.Uri
import androidx.lifecycle.Observer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore.CancelMediaPayload
import org.wordpress.android.fluxc.store.MediaStore.FetchMediaListPayload
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.editor.media.EditorMedia.AddMediaToPostUiState
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.MediaUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.helpers.ToastMessageHolder

@ExperimentalCoroutinesApi
class EditorMediaTest : BaseUnitTest() {
    @Test
    fun `addNewMediaItemsToEditorAsync emits AddingSingleMedia for a single uri`() = test {
        // Arrange
        val editorMedia = createEditorMedia()
        val captor = argumentCaptor<AddMediaToPostUiState>()
        val observer: Observer<AddMediaToPostUiState> = mock()
        editorMedia.uiState.observeForever(observer)

        // Act
        editorMedia.addNewMediaItemsToEditorAsync(mock(), false)
        // Assert
        verify(observer, times(3)).onChanged(captor.capture())
        assertThat(captor.firstValue).isEqualTo(AddMediaToPostUiState.AddingMediaIdle)
        assertThat(captor.secondValue).isEqualTo(AddMediaToPostUiState.AddingSingleMedia)
        assertThat(captor.thirdValue).isEqualTo(AddMediaToPostUiState.AddingMediaIdle)
    }

    @Test
    fun `addNewMediaItemsToEditorAsync shows snackbar when a media fails`() = test {
        // Arrange
        val addLocalMediaToPostUseCase = createAddLocalMediaToPostUseCase(
            resultForAddNewMediaToEditorAsync = false
        )
        val editorMedia = createEditorMedia(addLocalMediaToPostUseCase = addLocalMediaToPostUseCase)

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
        val editorMedia = createEditorMedia(addLocalMediaToPostUseCase = addLocalMediaToPostUseCase)

        val captor = argumentCaptor<Event<SnackbarMessageHolder>>()
        val observer: Observer<Event<SnackbarMessageHolder>> = mock()
        editorMedia.snackBarMessage.observeForever(observer)

        // Act
        editorMedia.addNewMediaItemsToEditorAsync(mock(), false)
        // Assert
        verify(observer, never()).onChanged(captor.capture())
    }

    @Test
    fun `addGifMediaToPostAsync invokes addLocalMediaToEditorAsync with all media`() = test {
        // Arrange
        val localIdArray = listOf(1, 2, 3).toIntArray()
        val addLocalMediaToPostUseCase = createAddLocalMediaToPostUseCase()

        // Act
        createEditorMedia(
            addLocalMediaToPostUseCase = addLocalMediaToPostUseCase
        )
            .addGifMediaToPostAsync(localIdArray)
        // Assert
        verify(addLocalMediaToPostUseCase).addLocalMediaToEditorAsync(
            eq(localIdArray.toList()),
            anyOrNull(),
            anyBoolean()
        )
    }

    @Test
    fun `addExistingMediaToEditorAsync passes mediaId to addExistingMediaToPostUseCase`() =
        test {
            // Arrange
            val mediaModels = listOf(createMediaModel(mediaId = MEDIA_MODEL_REMOTE_ID))
            val addExistingMediaModelUseCase = mock<AddExistingMediaToPostUseCase>()
            // Act
            createEditorMedia(
                addExistingMediaToPostUseCase = addExistingMediaModelUseCase
            )
                .addExistingMediaToEditorAsync(mediaModels, mock())
            // Assert
            verify(addExistingMediaModelUseCase)
                .addMediaExistingInRemoteToEditorAsync(
                    anyOrNull(),
                    anyOrNull(),
                    eq(listOf(MEDIA_MODEL_REMOTE_ID)),
                    anyOrNull()
                )
        }

    @Test
    fun `addExistingMediaToEditorAsync converts array to list and invokes addExistingMediaToPostUseCase`() =
        test {
            // Arrange
            val ids = listOf(MEDIA_MODEL_REMOTE_ID).toLongArray()
            val addExistingMediaModelUseCase = mock<AddExistingMediaToPostUseCase>()
            // Act
            createEditorMedia(
                addExistingMediaToPostUseCase = addExistingMediaModelUseCase
            )
                .addExistingMediaToEditorAsync(mock(), ids)
            // Assert
            verify(addExistingMediaModelUseCase)
                .addMediaExistingInRemoteToEditorAsync(
                    anyOrNull(),
                    anyOrNull(),
                    eq(listOf(MEDIA_MODEL_REMOTE_ID)),
                    anyOrNull()
                )
        }

    @Test
    fun `cancelMediaUploadAsync dispatches newCancelMediaUploadAction`() =
        test {
            // Arrange
            val expectedId = MEDIA_MODEL_LOCAL_ID
            val expectedDeletedFlag = true
            val getMediaModelUseCase = createGetMediaModelUseCase()
            val dispatcher = mock<Dispatcher>()
            val captor = argumentCaptor<Action<CancelMediaPayload>>()
            // Act
            createEditorMedia(
                getMediaModelUseCase = getMediaModelUseCase,
                dispatcher = dispatcher
            )
                .cancelMediaUploadAsync(expectedId, expectedDeletedFlag)
            // Assert
            verify(dispatcher).dispatch(captor.capture())
            assertThat(captor.firstValue.payload.media.id).isEqualTo(expectedId)
            assertThat(captor.firstValue.payload.delete).isEqualTo(expectedDeletedFlag)
        }

    @Test
    fun `refreshBlogMedia dispatches newFetchMediaListAction when online`() {
        // Arrange
        val networkUtilsWrapper = createNetworkUtilsWrapper(isOnline = true)
        val dispatcher = mock<Dispatcher>()
        // Act
        createEditorMedia(
            networkUtilsWrapper = networkUtilsWrapper,
            dispatcher = dispatcher
        )
            .refreshBlogMedia()
        // Assert
        verify(dispatcher).dispatch(any<Action<FetchMediaListPayload>>())
    }

    @Test
    fun `refreshBlogMedia does NOT dispatch newFetchMediaListAction when offline`() {
        // Arrange
        val networkUtilsWrapper = createNetworkUtilsWrapper(isOnline = false)
        val dispatcher = mock<Dispatcher>()
        // Act
        createEditorMedia(
            networkUtilsWrapper = networkUtilsWrapper,
            dispatcher = dispatcher
        )
            .refreshBlogMedia()
        // Assert
        verify(dispatcher, never()).dispatch(any<Action<FetchMediaListPayload>>())
    }

    @Test
    fun `refreshBlogMedia displays error Toast when offline`() = test {
        // Arrange
        val networkUtilsWrapper = createNetworkUtilsWrapper(isOnline = false)
        val dispatcher = mock<Dispatcher>()

        val editorMedia = createEditorMedia(
            networkUtilsWrapper = networkUtilsWrapper,
            dispatcher = dispatcher
        )
        val captor = argumentCaptor<Event<ToastMessageHolder>>()
        val observer: Observer<Event<ToastMessageHolder>> = mock()
        editorMedia.toastMessage.observeForever(observer)

        // Act
        editorMedia.refreshBlogMedia()
        // Assert
        verify(observer, times(1)).onChanged(captor.capture())
        assertThat(captor.firstValue.getContentIfNotHandled()?.messageRes)
            .isEqualTo(R.string.error_media_refresh_no_connection)
    }

    @Test
    fun `retryFailedMediaAsync invokes retry and passes provided mediaIds`() = test {
        // Arrange
        val expectedIds = listOf(1, 2, 3)
        val retryFailedMediaUploadUseCase = mock<RetryFailedMediaUploadUseCase>()
        // Act
        createEditorMedia(retryFailedMediaUploadUseCase = retryFailedMediaUploadUseCase)
            .retryFailedMediaAsync(expectedIds)

        // Assert
        verify(retryFailedMediaUploadUseCase).retryFailedMediaAsync(anyOrNull(), eq(expectedIds))
    }

    @Test
    fun `reattachUploadingMedia is called for Aztec editor`() {
        // Arrange
        val reattachUploadingMediaUseCase = mock<ReattachUploadingMediaUseCase>()
        // Act
        createEditorMedia(reattachUploadingMediaUseCase = reattachUploadingMediaUseCase)
            .reattachUploadingMediaForAztec(mock(), true, mock())
        // Assert
        verify(reattachUploadingMediaUseCase).reattachUploadingMediaForAztec(anyOrNull(), anyOrNull())
    }

    @Test
    fun `reattachUploadingMedia is NOT called for other editors`() {
        // Arrange
        val reattachUploadingMediaUseCase = mock<ReattachUploadingMediaUseCase>()
        // Act
        createEditorMedia(reattachUploadingMediaUseCase = reattachUploadingMediaUseCase)
            .reattachUploadingMediaForAztec(mock(), false, mock())
        // Assert
        verify(reattachUploadingMediaUseCase, never()).reattachUploadingMediaForAztec(anyOrNull(), anyOrNull())
    }

    private companion object Fixtures {
        private val VIDEO_URI = mock<Uri>()
        private val IMAGE_URI = mock<Uri>()
        private const val MEDIA_MODEL_REMOTE_ID = 123L
        private const val MEDIA_MODEL_LOCAL_ID = 1

        fun createEditorMedia(
            updateMediaModelUseCase: UpdateMediaModelUseCase = mock(),
            getMediaModelUseCase: GetMediaModelUseCase = createGetMediaModelUseCase(),
            dispatcher: Dispatcher = mock(),
            mediaUtilsWrapper: MediaUtilsWrapper = createMediaUtilsWrapper(),
            networkUtilsWrapper: NetworkUtilsWrapper = mock(),
            addLocalMediaToPostUseCase: AddLocalMediaToPostUseCase = createAddLocalMediaToPostUseCase(),
            addExistingMediaToPostUseCase: AddExistingMediaToPostUseCase = mock(),
            retryFailedMediaUploadUseCase: RetryFailedMediaUploadUseCase = mock(),
            siteModel: SiteModel = mock(),
            editorMediaListener: EditorMediaListener = mock(),
            removeMediaUseCase: RemoveMediaUseCase = mock(),
            cleanUpMediaToPostAssociationUseCase: CleanUpMediaToPostAssociationUseCase = mock(),
            reattachUploadingMediaUseCase: ReattachUploadingMediaUseCase = mock(),
            analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock(),
            analyticsUtilsWrapper: AnalyticsUtilsWrapper = mock()
        ): EditorMedia {
            val editorMedia = EditorMedia(
                updateMediaModelUseCase,
                getMediaModelUseCase,
                dispatcher,
                mediaUtilsWrapper,
                networkUtilsWrapper,
                addLocalMediaToPostUseCase,
                addExistingMediaToPostUseCase,
                retryFailedMediaUploadUseCase,
                cleanUpMediaToPostAssociationUseCase,
                removeMediaUseCase,
                reattachUploadingMediaUseCase,
                analyticsUtilsWrapper,
                analyticsTrackerWrapper,
                UnconfinedTestDispatcher(),
                UnconfinedTestDispatcher()
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
                        anyBoolean(),
                        anyBoolean()
                    )
                }.thenReturn(resultForAddNewMediaToEditorAsync)
            }

        fun createGetMediaModelUseCase(remoteMediaId: Long = MEDIA_MODEL_REMOTE_ID) =
            mock<GetMediaModelUseCase> {
                onBlocking { loadMediaByLocalId(anyOrNull()) } doAnswer { invocation ->
                    // Creates dummy media models from provided model ids
                    (invocation.getArgument(0) as Iterable<Int>)
                        .map {
                            createMediaModel(it, remoteMediaId)
                        }
                        .toList()
                }
            }

        fun createNetworkUtilsWrapper(isOnline: Boolean = true) = mock<NetworkUtilsWrapper> {
            on { isNetworkAvailable() }.thenReturn(isOnline)
        }

        fun createMediaModel(localId: Int = 1, mediaId: Long) = MediaModel(0, mediaId).apply { id = localId }
    }
}
