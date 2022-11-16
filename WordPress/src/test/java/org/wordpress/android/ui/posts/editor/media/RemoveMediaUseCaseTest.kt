package org.wordpress.android.ui.posts.editor.media

import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.test
import org.wordpress.android.ui.uploads.UploadServiceFacade
import org.wordpress.android.util.MediaUtilsWrapper

private const val DUMMY_MEDIA_ID = "1"

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class RemoveMediaUseCaseTest {
    private lateinit var removeMediaUseCase: RemoveMediaUseCase
    private val mediaStore: MediaStore = mock()
    private val dispatcher: Dispatcher = mock()
    private val mediaUtilsWrapper: MediaUtilsWrapper = mock()
    private val uploadServiceFacade: UploadServiceFacade = mock()

    @Before
    fun setUp() {
        removeMediaUseCase = RemoveMediaUseCase(
                mediaStore,
                dispatcher,
                mediaUtilsWrapper,
                uploadServiceFacade,
                TEST_DISPATCHER
        )
        whenever(mediaStore.getMediaWithLocalId(anyInt())).thenReturn(MediaModel().apply {
            uploadState = "non-empty-state"
        })
        whenever(mediaUtilsWrapper.isLocalFile(anyString())).thenReturn(true)
        whenever(uploadServiceFacade.isPendingOrInProgressMediaUpload(anyOrNull())).thenReturn(false)
    }

    @Test
    fun `Media is removed  when it's not uploading and is local file`() = test {
        // Arrange
        val mediaIds = listOf(DUMMY_MEDIA_ID)
        // Act
        removeMediaUseCase.removeMediaIfNotUploading(mediaIds = mediaIds)
        // Assert
        verify(dispatcher, times(1)).dispatch(any<Action<MediaModel>>())
    }

    @Test
    fun `Media is NOT removed  when it's being uploaded`() = test {
        // Arrange
        val mediaIds = listOf(DUMMY_MEDIA_ID)
        whenever(uploadServiceFacade.isPendingOrInProgressMediaUpload(anyOrNull())).thenReturn(true)
        // Act
        removeMediaUseCase.removeMediaIfNotUploading(mediaIds = mediaIds)
        // Assert
        verify(dispatcher, never()).dispatch(any<Action<MediaModel>>())
    }

    @Test
    fun `Media is NOT removed  when it's not a local file`() = test {
        // Arrange
        val mediaIds = listOf(DUMMY_MEDIA_ID)
        whenever(mediaUtilsWrapper.isLocalFile(anyString())).thenReturn(false)
        // Act
        removeMediaUseCase.removeMediaIfNotUploading(mediaIds = mediaIds)
        // Assert
        verify(dispatcher, never()).dispatch(any<Action<MediaModel>>())
    }

    @Test
    fun `Media is skipped when it's not found in media store`() = test {
        // Arrange
        val mediaIds = listOf(DUMMY_MEDIA_ID)
        whenever(mediaStore.getMediaWithLocalId(anyInt())).thenReturn(null)
        // Act
        removeMediaUseCase.removeMediaIfNotUploading(mediaIds = mediaIds)
        // Assert
        verify(dispatcher, never()).dispatch(any<Action<MediaModel>>())
        verify(uploadServiceFacade, never()).isPendingOrInProgressMediaUpload(anyOrNull())
        verify(mediaUtilsWrapper, never()).isLocalFile(anyOrNull())
    }
}
