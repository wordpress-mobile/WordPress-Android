package org.wordpress.android.viewmodel.pages

import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadQueued
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadingPost

@RunWith(MockitoJUnitRunner::class)
class PostPageListLabelColorUseCaseTest {
    private lateinit var useCase: PostPageListLabelColorUseCase

    @Before
    fun setUp() {
        useCase = PostPageListLabelColorUseCase()
    }

    @Test
    fun `label has error color on upload error`() {
        // Arrange
        val uploadState = failedUpload()
        // Act
        val labelsColor = useCase.getLabelsColor(
                dummyPostModel(),
                uploadState,
                hasUnhandledConflicts = false,
                hasUnhandledAutoSave = false
        )
        // Assert
        assertThat(labelsColor).isEqualTo(ERROR_COLOR)
    }

    @Test
    fun `label has progress color on when media upload in progress`() {
        // Arrange
        val uploadState = mediaUploadInProgress()
        // Act
        val labelsColor = useCase.getLabelsColor(
                dummyPostModel(),
                uploadState,
                hasUnhandledConflicts = false,
                hasUnhandledAutoSave = false
        )
        // Assert
        assertThat(labelsColor).isEqualTo(PROGRESS_INFO_COLOR)
    }

    @Test
    fun `label has progress color when post queued`() {
        // Arrange
        val uploadState = UploadQueued
        // Act
        val labelsColor = useCase.getLabelsColor(
                dummyPostModel(),
                uploadState,
                hasUnhandledConflicts = false,
                hasUnhandledAutoSave = false
        )
        // Assert
        assertThat(labelsColor).isEqualTo(PROGRESS_INFO_COLOR)
    }

    @Test
    fun `label has progress color when uploading post`() {
        // Arrange
        val uploadState = UploadingPost(false)
        // Act
        val labelsColor = useCase.getLabelsColor(
                dummyPostModel(),
                uploadState,
                hasUnhandledConflicts = false,
                hasUnhandledAutoSave = false
        )
        // Assert
        assertThat(labelsColor).isEqualTo(PROGRESS_INFO_COLOR)
    }

    @Test
    fun `label has state info color after failed upload but eligible for auto upload`() {
        // Arrange
        val uploadState = failedUpload(isEligibleForAutoUpload = true)
        // Act
        val labelsColor = useCase.getLabelsColor(
                dummyPostModel(),
                uploadState,
                hasUnhandledConflicts = false,
                hasUnhandledAutoSave = false
        )
        // Assert
        assertThat(labelsColor).isEqualTo(STATE_INFO_COLOR)
    }

    @Test
    fun `label has error color after failed upload when not eligible for auto upload`() {
        // Arrange
        val uploadState = failedUpload(isEligibleForAutoUpload = false)
        // Act
        val labelsColor = useCase.getLabelsColor(
                dummyPostModel(),
                uploadState,
                hasUnhandledConflicts = false,
                hasUnhandledAutoSave = false
        )
        // Assert
        assertThat(labelsColor).isEqualTo(ERROR_COLOR)
    }

    @Test
    fun `label has state info color on auto-save conflict`() {
        // Arrange
        val hasUnhandledAutoSave = true
        // Act
        val labelsColor = useCase.getLabelsColor(
                dummyPostModel(),
                mock(),
                hasUnhandledConflicts = false,
                hasUnhandledAutoSave = hasUnhandledAutoSave
        )
        // Assert
        assertThat(labelsColor).isEqualTo(STATE_INFO_COLOR)
    }

    @Test
    fun `label has error color on version conflict`() {
        // Arrange
        val hasUnhandledConflicts = true
        // Act
        val labelsColor = useCase.getLabelsColor(
                dummyPostModel(),
                mock(),
                hasUnhandledConflicts = hasUnhandledConflicts,
                hasUnhandledAutoSave = false
        )
        // Assert
        assertThat(labelsColor).isEqualTo(ERROR_COLOR)
    }

    private fun dummyPostModel(): PostModel {
        val post = PostModel()
        post.setDateCreated("1970-01-01'T'00:00:01Z")
        return post
    }

    private fun failedUpload(isEligibleForAutoUpload: Boolean = false) =
            PostUploadUiState.UploadFailed(mock(), isEligibleForAutoUpload, false)

    private fun mediaUploadInProgress() = PostUploadUiState.UploadingMedia(0)
}
