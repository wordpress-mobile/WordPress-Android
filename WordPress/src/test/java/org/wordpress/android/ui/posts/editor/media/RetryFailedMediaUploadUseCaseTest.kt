package org.wordpress.android.ui.posts.editor.media

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.QUEUED
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class RetryFailedMediaUploadUseCaseTest : BaseUnitTest() {
    @Test
    fun `Loads all failed media from db`() = test {
        // Arrange
        val getMediaModelUseCase = createGetMediaModelUseCase()
        val useCase = createUseCase(getMediaModelUseCase = getMediaModelUseCase)

        // Act
        useCase.retryFailedMediaAsync(mock(), FAILED_MEDIA_IDS)
        // Assert
        verify(getMediaModelUseCase).loadMediaByLocalId(FAILED_MEDIA_IDS)
    }

    @Test
    fun `Sets upload state to QUEUED to all media models`() = test {
        // Arrange
        val updateMediaModelUseCase = mock<UpdateMediaModelUseCase>()
        val useCase = createUseCase(updateMediaModelUseCase = updateMediaModelUseCase)

        // Act
        useCase.retryFailedMediaAsync(mock(), FAILED_MEDIA_IDS)

        // Assert
        verify(updateMediaModelUseCase, times(FAILED_MEDIA_IDS.size)).updateMediaModel(
            any(),
            anyOrNull(),
            eq(QUEUED)
        )
    }

    @Test
    fun `Invokes save and initiate upload on all posts`() = test {
        // Arrange
        val uploadMediaUseCase: UploadMediaUseCase = mock()
        val useCase = createUseCase(uploadMediaUseCase = uploadMediaUseCase)

        // Act
        useCase.retryFailedMediaAsync(mock(), FAILED_MEDIA_IDS)

        // Assert
        verify(uploadMediaUseCase).saveQueuedPostAndStartUpload(
            anyOrNull(),
            argThat { this.size == FAILED_MEDIA_IDS.size }
        )
    }

    @Test
    fun `Tracks upload retried event`() = test {
        // Arrange
        val trackerWrapper: AnalyticsTrackerWrapper = mock()
        val useCase = createUseCase(tracker = trackerWrapper)
        // Act
        useCase.retryFailedMediaAsync(mock(), FAILED_MEDIA_IDS)
        // Assert
        verify(trackerWrapper).track(Stat.EDITOR_UPLOAD_MEDIA_RETRIED)
    }

    @Test
    fun `Does NOT invoke save and initiate upload if the media list is empty`() = test {
        // Arrange
        val emptyList = listOf<Int>()
        val uploadMediaUseCase: UploadMediaUseCase = mock()
        val useCase = createUseCase(uploadMediaUseCase = uploadMediaUseCase)

        // Act
        useCase.retryFailedMediaAsync(mock(), emptyList)

        // Assert
        verify(uploadMediaUseCase, never()).saveQueuedPostAndStartUpload(anyOrNull(), anyOrNull())
    }

    @Test
    fun `Does NOT track upload retried event if the media list is empty`() = test {
        // Arrange
        val emptyList = listOf<Int>()
        val trackerWrapper: AnalyticsTrackerWrapper = mock()
        val useCase = createUseCase(tracker = trackerWrapper)
        // Act
        useCase.retryFailedMediaAsync(mock(), emptyList)
        // Assert
        verify(trackerWrapper, never()).track(Stat.EDITOR_UPLOAD_MEDIA_RETRIED)
    }

    private companion object Fixtures {
        private val FAILED_MEDIA_IDS = listOf(1, 2, 3)

        private fun createUseCase(
            getMediaModelUseCase: GetMediaModelUseCase = createGetMediaModelUseCase(),
            updateMediaModelUseCase: UpdateMediaModelUseCase = mock(),
            uploadMediaUseCase: UploadMediaUseCase = mock(),
            tracker: AnalyticsTrackerWrapper = mock()
        ): RetryFailedMediaUploadUseCase {
            return RetryFailedMediaUploadUseCase(
                getMediaModelUseCase,
                updateMediaModelUseCase,
                uploadMediaUseCase,
                tracker
            )
        }

        fun createGetMediaModelUseCase() = mock<GetMediaModelUseCase> {
            onBlocking { loadMediaByLocalId(any()) } doAnswer { invocation ->
                // Creates dummy media models from provided model ids
                (invocation.getArgument(0) as Iterable<Int>)
                    .map { createMediaModel(it) }
                    .toList()
            }
        }

        fun createMediaModel(mediaModelId: Int) =
            MediaModel().apply {
                this.id = mediaModelId
                this.uploadState = MediaUploadState.FAILED.name
            }
    }
}
