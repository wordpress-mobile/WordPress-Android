package org.wordpress.android.viewmodel.pages

import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState

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

    private fun dummyPostModel(): PostModel {
        val post = PostModel()
        post.setDateCreated("1970-01-01'T'00:00:01Z")
        return post
    }

    private fun failedUpload() = PostUploadUiState.UploadFailed(mock(), false, false)

    private fun mediaUploadInProgress() = PostUploadUiState.UploadingMedia(0)
}