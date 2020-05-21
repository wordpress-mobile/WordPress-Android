package org.wordpress.android.viewmodel.pages

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.NothingToUpload
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadFailed
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadQueued
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadWaitingForConnection
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadingMedia
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadingPost
import org.wordpress.android.viewmodel.uistate.ProgressBarUiState

@RunWith(MockitoJUnitRunner::class)
class PageItemProgressUiStateUseCaseTest {
    @Mock private lateinit var appPrefsWrapper: AppPrefsWrapper
    private lateinit var useCase: PageItemProgressUiStateUseCase

    @Before
    fun setUp() {
        useCase = PageItemProgressUiStateUseCase(appPrefsWrapper)
    }

    @Test
    fun `show progress when post is uploading`() {
        // Arrange
        val uploadState = UploadingPost(false)
        // Act
        val (progressState, _) = useCase.getProgressStateForPage(uploadState)
        // Assert
        assertThat(progressState).isEqualTo(ProgressBarUiState.Indeterminate)
    }

    @Test
    fun `show progress when post is queued`() {
        // Arrange
        val uploadState = UploadQueued
        // Act
        val (progressState, _) = useCase.getProgressStateForPage(uploadState)
        // Assert
        assertThat(progressState).isEqualTo(ProgressBarUiState.Indeterminate)
    }

    @Test
    fun `show progress when uploading media`() {
        // Arrange
        val uploadState = UploadingMedia(0)
        // Act
        val (progressState, _) = useCase.getProgressStateForPage(uploadState)
        // Assert
        assertThat(progressState).isInstanceOf(ProgressBarUiState.Determinate::class.java)
    }

    @Test
    fun `do not show progress when upload failed`() {
        // Arrange
        val uploadState = UploadFailed(mock(), isEligibleForAutoUpload = false, retryWillPushChanges = false)
        // Act
        val (progressState, _) = useCase.getProgressStateForPage(uploadState)
        // Assert
        assertThat(progressState).isEqualTo(ProgressBarUiState.Hidden)
    }

    @Test
    fun `do not show progress when nothing to upload`() {
        // Arrange
        val uploadState = NothingToUpload
        // Act
        val (progressState, _) = useCase.getProgressStateForPage(uploadState)
        // Assert
        assertThat(progressState).isEqualTo(ProgressBarUiState.Hidden)
    }

    @Test
    fun `do not show progress when upload waiting for a connection`() {
        // Arrange
        val uploadState = UploadWaitingForConnection(mock())
        // Act
        val (progressState, _) = useCase.getProgressStateForPage(uploadState)
        // Assert
        assertThat(progressState).isEqualTo(ProgressBarUiState.Hidden)
    }

    @Test
    fun `show overlay when uploading post`() {
        // Arrange
        val uploadState = UploadingPost(false)
        // Act
        val (_, showOverlay) = useCase.getProgressStateForPage(uploadState)
        // Assert
        assertThat(showOverlay).isTrue()
    }

    @Test
    fun `show overlay when uploading media and aztec is disabled`() {
        // Arrange
        val uploadState = UploadingMedia(0)
        whenever(appPrefsWrapper.isAztecEditorEnabled).thenReturn(false)
        // Act
        val (_, showOverlay) = useCase.getProgressStateForPage(uploadState)
        // Assert
        assertThat(showOverlay).isTrue()
    }

    @Test
    fun `do NOT show overlay when uploading media and aztec is enabled`() {
        // Arrange
        val uploadState = UploadingMedia(0)
        whenever(appPrefsWrapper.isAztecEditorEnabled).thenReturn(true)
        // Act
        val (_, showOverlay) = useCase.getProgressStateForPage(uploadState)
        // Assert
        assertThat(showOverlay).isFalse()
    }

    @Test
    fun `do NOT show overlay when upload queued`() {
        // Arrange
        val uploadState = UploadQueued
        // Act
        val (_, showOverlay) = useCase.getProgressStateForPage(uploadState)
        // Assert
        assertThat(showOverlay).isFalse()
    }

    @Test
    fun `do NOT show overlay when upload waiting for a connection`() {
        // Arrange
        val uploadState = UploadWaitingForConnection(mock())
        // Act
        val (_, showOverlay) = useCase.getProgressStateForPage(uploadState)
        // Assert
        assertThat(showOverlay).isFalse()
    }

    @Test
    fun `do NOT show overlay when nothing to upload`() {
        // Arrange
        val uploadState = NothingToUpload
        // Act
        val (_, showOverlay) = useCase.getProgressStateForPage(uploadState)
        // Assert
        assertThat(showOverlay).isFalse()
    }

    @Test
    fun `do NOT show overlay when upload fails`() {
        // Arrange
        val uploadState = UploadFailed(
                mock(),
                false,
                retryWillPushChanges = false
        )
        // Act
        val (_, showOverlay) = useCase.getProgressStateForPage(uploadState)
        // Assert
        assertThat(showOverlay).isFalse()
    }
}
