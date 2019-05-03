package org.wordpress.android.viewmodel.posts

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.MediaStore.MediaError
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType.AUTHORIZATION_REQUIRED
import org.wordpress.android.fluxc.store.PostStore.PostError
import org.wordpress.android.fluxc.store.PostStore.PostErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.UploadStore.UploadError
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.viewmodel.posts.PostListItemType.PostListItemUiState
import org.wordpress.android.widgets.PostListButtonType

private const val FORMATTER_DATE = "January 1st, 1:35pm"

private val POST_STATE_PUBLISH = PostStatus.PUBLISHED.toString()
private val POST_STATE_PRIVATE = PostStatus.PRIVATE.toString()
private val POST_STATE_PENDING = PostStatus.PENDING.toString()

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
        assertThat(state.data.imageUrl).isEqualTo(testUrl)
    }

    @Test
    fun `label has error color on upload error`() {
        val state = createPostListItemUiState(uploadStatus = createUploadStatus(uploadError = createGenericError()))
        assertThat(state.data.statusesColor).isEqualTo(ERROR_COLOR)
    }

    @Test
    fun `label has progress color on error when media upload in progress`() {
        val state = createPostListItemUiState(
                uploadStatus = createUploadStatus(
                        uploadError = createGenericError(),
                        hasInProgressMediaUpload = true
                )
        )
        assertThat(state.data.statusesColor).isEqualTo(PROGRESS_INFO_COLOR)
    }

    @Test
    fun `label has progress color when post queued`() {
        val state = createPostListItemUiState(uploadStatus = createUploadStatus(isQueued = true))
        assertThat(state.data.statusesColor).isEqualTo(PROGRESS_INFO_COLOR)
    }

    @Test
    fun `label has progress color when media queued`() {
        val state = createPostListItemUiState(uploadStatus = createUploadStatus(hasPendingMediaUpload = true))
        assertThat(state.data.statusesColor).isEqualTo(PROGRESS_INFO_COLOR)
    }

    @Test
    fun `label has progress color when uploading media`() {
        val state = createPostListItemUiState(uploadStatus = createUploadStatus(hasInProgressMediaUpload = true))
        assertThat(state.data.statusesColor).isEqualTo(PROGRESS_INFO_COLOR)
    }

    @Test
    fun `label has progress color when uploading post`() {
        val state = createPostListItemUiState(uploadStatus = createUploadStatus(isUploading = true))
        assertThat(state.data.statusesColor).isEqualTo(PROGRESS_INFO_COLOR)
    }

    fun `label has error color on version conflict`() {
        val state = createPostListItemUiState(unhandledConflicts = true)
        assertThat(state.data.statusesColor).isEqualTo(ERROR_COLOR)
    }

    @Test
    fun `private label shown for private posts`() {
        val state = createPostListItemUiState(post = createPostModel(status = POST_STATE_PRIVATE))
        assertThat(state.data.statuses).contains(UiStringRes(R.string.post_status_post_private))
    }

    @Test
    fun `pending review label shown for posts pending review`() {
        val state = createPostListItemUiState(post = createPostModel(status = POST_STATE_PENDING))
        assertThat(state.data.statuses).contains(UiStringRes(R.string.post_status_pending_review))
    }

    @Test
    fun `local draft label shown for local posts`() {
        val state = createPostListItemUiState(post = createPostModel(isLocalDraft = true))
        assertThat(state.data.statuses).contains(UiStringRes(R.string.local_draft))
    }

    @Test
    fun `locally changed label shown for locally changed posts`() {
        val state = createPostListItemUiState(post = createPostModel(isLocallyChanged = true))
        assertThat(state.data.statuses).contains(UiStringRes(R.string.local_changes))
    }

    @Test
    fun `version conflict label shown for posts with version conflict`() {
        val state = createPostListItemUiState(unhandledConflicts = true)
        assertThat(state.data.statuses).contains(UiStringRes(R.string.local_post_is_conflicted))
    }

    @Test
    fun `uploading post label shown when the post is being uploaded`() {
        val state = createPostListItemUiState(uploadStatus = createUploadStatus(isUploading = true))
        assertThat(state.data.statuses).contains(UiStringRes(R.string.post_uploading))
    }

    @Test
    fun `uploading media label shown when the post's media is being uploaded`() {
        val state = createPostListItemUiState(uploadStatus = createUploadStatus(hasInProgressMediaUpload = true))
        assertThat(state.data.statuses).contains(UiStringRes(R.string.uploading_media))
    }

    @Test
    fun `queued post label shown when the post has pending media uploads`() {
        val state = createPostListItemUiState(uploadStatus = createUploadStatus(hasPendingMediaUpload = true))
        assertThat(state.data.statuses).contains(UiStringRes(R.string.post_queued))
    }

    @Test
    fun `queued post label shown when the post is queued for upload`() {
        val state = createPostListItemUiState(uploadStatus = createUploadStatus(isQueued = true))
        assertThat(state.data.statuses).contains(UiStringRes(R.string.post_queued))
    }

    @Test
    fun `error uploading media label shown when the media upload fails`() {
        val state = createPostListItemUiState(
                uploadStatus = createUploadStatus(uploadError = UploadError(MediaError(AUTHORIZATION_REQUIRED)))
        )
        assertThat(state.data.statuses).contains(UiStringRes(R.string.error_media_recover_post))
    }

    @Test
    fun `error uploading post label shown when the post upload fails`() {
        val errorMsg = "testing error message"
        val state = createPostListItemUiState(
                uploadStatus = createUploadStatus(uploadError = UploadError(PostError(GENERIC_ERROR, errorMsg)))
        )
        assertThat(state.data.statuses).contains(UiStringText(errorMsg))
    }

    @Test
    fun `given a mix of info and error statuses, only the error status is shown`() {
        val state = createPostListItemUiState(
                post = createPostModel(isLocallyChanged = true, status = POST_STATE_PRIVATE),
                uploadStatus = createUploadStatus(uploadError = UploadError(MediaError(AUTHORIZATION_REQUIRED)))
        )
        assertThat(state.data.statuses).contains(UiStringRes(R.string.error_media_recover_post))
        assertThat(state.data.statuses).hasSize(1)
    }

    @Test
    fun `multiple info labels are being shown together`() {
        val state = createPostListItemUiState(
                post = createPostModel(isLocallyChanged = true, status = POST_STATE_PRIVATE)
        )
        assertThat(state.data.statuses).contains(UiStringRes(R.string.local_changes))
        assertThat(state.data.statuses).contains(UiStringRes(R.string.post_status_post_private))
    }

    @Test
    fun `show progress when performing critical action`() {
        val state = createPostListItemUiState(performingCriticalAction = true)
        assertThat(state.data.progressBarState).isEqualTo(PostListItemProgressBar.Indeterminate)
    }

    @Test
    fun `show progress when post is uploading or queued`() {
        val state = createPostListItemUiState(uploadStatus = createUploadStatus(isUploadingOrQueued = true))
        assertThat(state.data.progressBarState).isEqualTo(PostListItemProgressBar.Indeterminate)
    }

    @Test
    fun `show progress when uploading media`() {
        val state = createPostListItemUiState(uploadStatus = createUploadStatus(hasInProgressMediaUpload = true))
        assertThat(state.data.progressBarState).isInstanceOf(PostListItemProgressBar.Determinate::class.java)
    }

    @Test
    fun `do not show progress when upload failed`() {
        val state = createPostListItemUiState(
                uploadStatus = createUploadStatus(
                        isUploadFailed = true
                )
        )
        assertThat(state.data.progressBarState).isEqualTo(PostListItemProgressBar.Hidden)
    }

    @Test
    fun `show progress when upload failed and retrying`() {
        val state = createPostListItemUiState(
                uploadStatus = createUploadStatus(
                        isUploadFailed = true,
                        isUploadingOrQueued = true,
                        hasInProgressMediaUpload = true
                )
        )
        assertThat(state.data.progressBarState).isInstanceOf(PostListItemProgressBar.Determinate::class.java)
    }

    @Test
    fun `show overlay when performing critical action`() {
        val state = createPostListItemUiState(performingCriticalAction = true)
        assertThat(state.data.showOverlay).isTrue()
    }

    @Test
    fun `show overlay when uploading post`() {
        val state = createPostListItemUiState(uploadStatus = createUploadStatus(isUploading = true))
        assertThat(state.data.showOverlay).isTrue()
    }

    private fun createPostModel(
        status: String = POST_STATE_PUBLISH,
        isLocalDraft: Boolean = false,
        isLocallyChanged: Boolean = false
    ): PostModel {
        val post = PostModel()
        post.status = status
        post.setIsLocalDraft(isLocalDraft)
        post.setIsLocallyChanged(isLocallyChanged)
        return post
    }

    private fun createPostListItemUiState(
        post: PostModel = PostModel(),
        uploadStatus: PostListItemUploadStatus = createUploadStatus(),
        unhandledConflicts: Boolean = false,
        capabilitiesToPublish: Boolean = false,
        statsSupported: Boolean = false,
        featuredImageUrl: String? = null,
        formattedDate: String = FORMATTER_DATE,
        performingCriticalAction: Boolean = false,
        onAction: (PostModel, PostListButtonType, AnalyticsTracker.Stat) -> Unit = { _, _, _ -> }
    ): PostListItemUiState = helper.createPostListItemUiState(
            post = post,
            uploadStatus = uploadStatus,
            unhandledConflicts = unhandledConflicts,
            capabilitiesToPublish = capabilitiesToPublish,
            statsSupported = statsSupported,
            featuredImageUrl = featuredImageUrl,
            formattedDate = formattedDate,
            onAction = onAction,
            performingCriticalAction = performingCriticalAction
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
                    uploadError = uploadError,
                    mediaUploadProgress = mediaUploadProgress,
                    isUploading = isUploading,
                    isUploadingOrQueued = isUploadingOrQueued,
                    isQueued = isQueued,
                    isUploadFailed = isUploadFailed,
                    hasInProgressMediaUpload = hasInProgressMediaUpload,
                    hasPendingMediaUpload = hasPendingMediaUpload
            )

    private fun createGenericError(): UploadError = UploadError(PostError(GENERIC_ERROR))
}
