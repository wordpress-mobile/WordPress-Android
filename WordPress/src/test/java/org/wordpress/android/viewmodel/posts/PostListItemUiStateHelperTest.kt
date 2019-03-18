package org.wordpress.android.viewmodel.posts

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.store.PostStore.PostError
import org.wordpress.android.fluxc.store.PostStore.PostErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.UploadStore.UploadError
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.widgets.PostListButtonType

private const val FORMATTER_DATE = "January 1st, 1:35pm"

@RunWith(MockitoJUnitRunner::class)
class PostListItemUiStateHelperTest {
    @Mock private lateinit var appPrefsWrapper: AppPrefsWrapper
    private lateinit var helper: PostListItemUiStateHelper

    @Before
    fun setup() {
        helper = PostListItemUiStateHelper(appPrefsWrapper)
        whenever(appPrefsWrapper.isAztecEditorEnabled).thenReturn(true)
    }

    @Test
    fun `featureImgUrl is propagated`() {
        val testUrl = "https://example.com"
        val state = createPostListItemUiState(featuredImageUrl = testUrl)
        assertThat(state.imageUrl).isEqualTo(testUrl)
    }

    @Test
    fun `label has error color on upload error`() {
        val state = createPostListItemUiState(uploadStatus = createUploadStatus(uploadError = createGenericError()))
        assertThat(state.statusLabelsColor).isEqualTo(ERROR_COLOR)
    }

    @Test
    fun `label has warning color on error when media upload in progress`() {
        val state = createPostListItemUiState(
                uploadStatus = createUploadStatus(
                        uploadError = createGenericError(),
                        hasInProgressMediaUpload = true
                )
        )
        assertThat(state.statusLabelsColor).isEqualTo(PROGRESS_INFO_COLOR)
    }

    @Test
    fun `label has warning color when post queued`() {
        val state = createPostListItemUiState(uploadStatus = createUploadStatus(isQueued = true))
        assertThat(state.statusLabelsColor).isEqualTo(PROGRESS_INFO_COLOR)
    }

    @Test
    fun `label has warning color when media queued`() {
        val state = createPostListItemUiState(uploadStatus = createUploadStatus(hasPendingMediaUpload = true))
        assertThat(state.statusLabelsColor).isEqualTo(PROGRESS_INFO_COLOR)
    }

    @Test
    fun `label has warning color when uploading media`() {
        val state = createPostListItemUiState(uploadStatus = createUploadStatus(hasInProgressMediaUpload = true))
        assertThat(state.statusLabelsColor).isEqualTo(PROGRESS_INFO_COLOR)
    }

    @Test
    fun `label has warning color when uploading post`() {
        val state = createPostListItemUiState(unhandledConflicts = true)
        assertThat(state.statusLabelsColor).isEqualTo(PROGRESS_INFO_COLOR)
    }

    private fun createPostListItemUiState(
        post: PostModel = PostModel(),
        uploadStatus: PostListItemUploadStatus = createUploadStatus(),
        unhandledConflicts: Boolean = false,
        capabilitiesToPublish: Boolean = false,
        statsSupported: Boolean = false,
        featuredImageUrl: String? = null,
        formattedDate: String = FORMATTER_DATE,
        onAction: (PostModel, PostListButtonType, AnalyticsTracker.Stat) -> Unit = { _, _, _ -> }
    ): PostListItemUiState = helper.createPostListItemUiState(
            post,
            uploadStatus,
            unhandledConflicts,
            capabilitiesToPublish,
            statsSupported,
            featuredImageUrl,
            formattedDate,
            onAction
    )

    private fun createUploadStatus(
        uploadError: UploadError? = null,
        mediaUploadProgress: Int = 0,
        isUploading: Boolean = false,
        isUploadingOrQueued: Boolean = false,
        isQueued: Boolean = false,
        isUploadFailed: Boolean = false,
        hasInProgressMediaUpload: Boolean = false,
        hasPendingMediaUpload: Boolean = false
    ): PostListItemUploadStatus =
            PostListItemUploadStatus(
                    uploadError,
                    mediaUploadProgress,
                    isUploading,
                    isUploadingOrQueued,
                    isQueued,
                    isUploadFailed,
                    hasInProgressMediaUpload,
                    hasPendingMediaUpload
            )

    private fun createGenericError(): UploadError = UploadError(PostError(GENERIC_ERROR))
}
