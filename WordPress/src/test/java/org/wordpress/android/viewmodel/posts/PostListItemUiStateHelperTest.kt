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
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType.AUTHORIZATION_REQUIRED
import org.wordpress.android.fluxc.store.PostStore.PostError
import org.wordpress.android.fluxc.store.PostStore.PostErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.UploadStore.UploadError
import org.wordpress.android.ui.posts.AuthorFilterSelection
import org.wordpress.android.ui.posts.AuthorFilterSelection.EVERYONE
import org.wordpress.android.ui.posts.AuthorFilterSelection.ME
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.viewmodel.posts.PostListItemAction.MoreItem
import org.wordpress.android.viewmodel.posts.PostListItemType.PostListItemUiState
import org.wordpress.android.widgets.PostListButtonType

private const val FORMATTER_DATE = "January 1st, 1:35pm"

private val POST_STATE_PUBLISH = PostStatus.PUBLISHED.toString()
private val POST_STATE_SCHEDULED = PostStatus.SCHEDULED.toString()
private val POST_STATE_PRIVATE = PostStatus.PRIVATE.toString()
private val POST_STATE_PENDING = PostStatus.PENDING.toString()
private val POST_STATE_DRAFT = PostStatus.DRAFT.toString()
private val POST_STATE_TRASHED = PostStatus.TRASHED.toString()

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
    fun `verify draft actions`() {
        val state = createPostListItemUiState(
                post = createPostModel(status = POST_STATE_DRAFT)
        )

        assertThat(state.actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_EDIT)
        assertThat(state.actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_PUBLISH)
        assertThat(state.actions[2].buttonType).isEqualTo(PostListButtonType.BUTTON_MORE)
        assertThat(state.actions).hasSize(3)

        assertThat((state.actions[2] as MoreItem).actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_VIEW)
        assertThat((state.actions[2] as MoreItem).actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_TRASH)
        assertThat((state.actions[2] as MoreItem).actions).hasSize(2)
    }

    @Test
    fun `verify local draft actions`() {
        val state = createPostListItemUiState(
                post = createPostModel(status = POST_STATE_DRAFT, isLocalDraft = true)
        )

        assertThat(state.actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_EDIT)
        assertThat(state.actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_PUBLISH)
        assertThat(state.actions[2].buttonType).isEqualTo(PostListButtonType.BUTTON_MORE)
        assertThat(state.actions).hasSize(3)

        assertThat((state.actions[2] as MoreItem).actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_PREVIEW)
        assertThat((state.actions[2] as MoreItem).actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_DELETE)
        assertThat((state.actions[2] as MoreItem).actions).hasSize(2)
    }

    @Test
    fun `verify draft actions without publishing rights`() {
        val state = createPostListItemUiState(
                post = createPostModel(status = POST_STATE_DRAFT),
                capabilitiesToPublish = false
        )

        assertThat(state.actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_EDIT)
        assertThat(state.actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_SUBMIT)
        assertThat(state.actions[2].buttonType).isEqualTo(PostListButtonType.BUTTON_MORE)
        assertThat(state.actions).hasSize(3)

        assertThat((state.actions[2] as MoreItem).actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_VIEW)
        assertThat((state.actions[2] as MoreItem).actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_TRASH)
        assertThat((state.actions[2] as MoreItem).actions).hasSize(2)
    }

    @Test
    fun `verify local draft actions without publishing rights`() {
        val state = createPostListItemUiState(
                post = createPostModel(status = POST_STATE_DRAFT, isLocalDraft = true),
                capabilitiesToPublish = false
        )

        assertThat(state.actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_EDIT)
        assertThat(state.actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_SUBMIT)
        assertThat(state.actions[2].buttonType).isEqualTo(PostListButtonType.BUTTON_MORE)
        assertThat(state.actions).hasSize(3)

        assertThat((state.actions[2] as MoreItem).actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_PREVIEW)
        assertThat((state.actions[2] as MoreItem).actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_DELETE)
        assertThat((state.actions[2] as MoreItem).actions).hasSize(2)
    }

    @Test
    fun `verify draft actions on failed upload`() {
        val state = createPostListItemUiState(
                post = createPostModel(status = POST_STATE_DRAFT),
                uploadStatus = createUploadStatus(uploadError = UploadError(PostError(GENERIC_ERROR)))
        )

        assertThat(state.actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_EDIT)
        assertThat(state.actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_RETRY)
        assertThat(state.actions[2].buttonType).isEqualTo(PostListButtonType.BUTTON_TRASH)
        assertThat(state.actions).hasSize(3)
    }

    @Test
    fun `verify local draft actions on failed upload`() {
        val state = createPostListItemUiState(
                post = createPostModel(status = POST_STATE_DRAFT, isLocalDraft = true),
                uploadStatus = createUploadStatus(uploadError = UploadError(PostError(GENERIC_ERROR)))
        )

        assertThat(state.actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_EDIT)
        assertThat(state.actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_RETRY)
        assertThat(state.actions[2].buttonType).isEqualTo(PostListButtonType.BUTTON_DELETE)
        assertThat(state.actions).hasSize(3)
    }

    @Test
    fun `verify draft actions on failed upload without publishing rights`() {
        val state = createPostListItemUiState(
                post = createPostModel(status = POST_STATE_DRAFT),
                capabilitiesToPublish = false,
                uploadStatus = createUploadStatus(uploadError = UploadError(PostError(GENERIC_ERROR)))
        )

        assertThat(state.actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_EDIT)
        assertThat(state.actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_RETRY)
        assertThat(state.actions[2].buttonType).isEqualTo(PostListButtonType.BUTTON_TRASH)
        assertThat(state.actions).hasSize(3)
    }

    @Test
    fun `verify local draft actions on failed upload without publishing rights`() {
        val state = createPostListItemUiState(
                post = createPostModel(status = POST_STATE_DRAFT, isLocalDraft = true),
                capabilitiesToPublish = false,
                uploadStatus = createUploadStatus(uploadError = UploadError(PostError(GENERIC_ERROR)))
        )

        assertThat(state.actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_EDIT)
        assertThat(state.actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_RETRY)
        assertThat(state.actions[2].buttonType).isEqualTo(PostListButtonType.BUTTON_DELETE)
        assertThat(state.actions).hasSize(3)
    }

    @Test
    fun `verify published post actions`() {
        val state = createPostListItemUiState(
                post = createPostModel(status = POST_STATE_PUBLISH)
        )

        assertThat(state.actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_EDIT)
        assertThat(state.actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_VIEW)
        assertThat(state.actions[2].buttonType).isEqualTo(PostListButtonType.BUTTON_MORE)
        assertThat(state.actions).hasSize(3)

        assertThat((state.actions[2] as MoreItem).actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_TRASH)
        assertThat((state.actions[2] as MoreItem).actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_STATS)
        assertThat((state.actions[2] as MoreItem).actions).hasSize(2)
    }

    @Test
    fun `verify published post with changes actions`() {
        val state = createPostListItemUiState(
                post = createPostModel(status = POST_STATE_PUBLISH, isLocallyChanged = true)
        )

        assertThat(state.actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_EDIT)
        assertThat(state.actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_PUBLISH)
        assertThat(state.actions[2].buttonType).isEqualTo(PostListButtonType.BUTTON_MORE)
        assertThat(state.actions).hasSize(3)

        assertThat((state.actions[2] as MoreItem).actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_PREVIEW)
        assertThat((state.actions[2] as MoreItem).actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_TRASH)
        assertThat((state.actions[2] as MoreItem).actions).hasSize(2)
    }

    @Test
    fun `verify published post with failed upload actions`() {
        val state = createPostListItemUiState(
                post = createPostModel(status = POST_STATE_PUBLISH, isLocallyChanged = true),
                uploadStatus = createUploadStatus(uploadError = UploadError(PostError(GENERIC_ERROR)))
        )

        assertThat(state.actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_EDIT)
        assertThat(state.actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_RETRY)
        assertThat(state.actions[2].buttonType).isEqualTo(PostListButtonType.BUTTON_TRASH)
        assertThat(state.actions).hasSize(3)
    }

    @Test
    fun `verify trashed post actions`() {
        val state = createPostListItemUiState(
                post = createPostModel(status = POST_STATE_TRASHED)
        )

        assertThat(state.actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_MOVE_TO_DRAFT)
        assertThat(state.actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_DELETE_PERMANENTLY)
        assertThat(state.actions).hasSize(2)
    }

    @Test
    fun `verify scheduled post actions`() {
        val state = createPostListItemUiState(
                post = createPostModel(status = POST_STATE_SCHEDULED)
        )

        assertThat(state.actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_EDIT)
        assertThat(state.actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_VIEW)
        assertThat(state.actions[2].buttonType).isEqualTo(PostListButtonType.BUTTON_TRASH)
        assertThat(state.actions).hasSize(3)
    }

    @Test
    fun `verify scheduled post with changes actions`() {
        val state = createPostListItemUiState(
                post = createPostModel(status = POST_STATE_SCHEDULED, isLocallyChanged = true)
        )

        assertThat(state.actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_EDIT)
        assertThat(state.actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_SYNC)
        assertThat(state.actions[2].buttonType).isEqualTo(PostListButtonType.BUTTON_MORE)
        assertThat(state.actions).hasSize(3)

        assertThat((state.actions[2] as MoreItem).actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_PREVIEW)
        assertThat((state.actions[2] as MoreItem).actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_TRASH)
        assertThat((state.actions[2] as MoreItem).actions).hasSize(2)
    }

    @Test
    fun `verify scheduled post with failed upload actions`() {
        val state = createPostListItemUiState(
                post = createPostModel(status = POST_STATE_SCHEDULED, isLocallyChanged = true),
                uploadStatus = createUploadStatus(uploadError = UploadError(PostError(GENERIC_ERROR)))
        )

        assertThat(state.actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_EDIT)
        assertThat(state.actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_RETRY)
        assertThat(state.actions[2].buttonType).isEqualTo(PostListButtonType.BUTTON_TRASH)
        assertThat(state.actions).hasSize(3)
    }

    @Test
    fun `verify post pending review with publishing rights actions`() {
        val state = createPostListItemUiState(
                post = createPostModel(status = POST_STATE_PENDING)
        )

        assertThat(state.actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_EDIT)
        assertThat(state.actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_PUBLISH)
        assertThat(state.actions[2].buttonType).isEqualTo(PostListButtonType.BUTTON_MORE)
        assertThat(state.actions).hasSize(3)
        assertThat((state.actions[2] as MoreItem).actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_VIEW)
        assertThat((state.actions[2] as MoreItem).actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_TRASH)
        assertThat((state.actions[2] as MoreItem).actions).hasSize(2)
    }

    @Test
    fun `verify post pending review without publishing rights`() {
        val state = createPostListItemUiState(
                post = createPostModel(status = POST_STATE_PENDING),
                capabilitiesToPublish = false
        )

        assertThat(state.actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_EDIT)
        assertThat(state.actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_VIEW)
        assertThat(state.actions[2].buttonType).isEqualTo(PostListButtonType.BUTTON_TRASH)
        assertThat(state.actions).hasSize(3)
    }

    @Test
    fun `verify published post with local changes eligible for auto upload`() {
        val state = createPostListItemUiState(
                post = createPostModel(status = POST_STATE_PUBLISH, isLocallyChanged = true),
                uploadStatus = createUploadStatus(isEligibleForAutoUpload = true)
        )

        assertThat(state.actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_EDIT)
        assertThat(state.actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_CANCEL_PENDING_AUTO_UPLOAD)
        assertThat(state.actions[2].buttonType).isEqualTo(PostListButtonType.BUTTON_MORE)
        assertThat(state.actions).hasSize(3)
        assertThat((state.actions[2] as MoreItem).actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_PREVIEW)
        assertThat((state.actions[2] as MoreItem).actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_TRASH)
        assertThat((state.actions[2] as MoreItem).actions).hasSize(2)
    }

    @Test
    fun `verify scheduled post with local changes eligible for auto upload`() {
        val state = createPostListItemUiState(
                post = createPostModel(status = POST_STATE_SCHEDULED, isLocallyChanged = true),
                uploadStatus = createUploadStatus(isEligibleForAutoUpload = true)
        )

        assertThat(state.actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_EDIT)
        assertThat(state.actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_CANCEL_PENDING_AUTO_UPLOAD)
        assertThat(state.actions[2].buttonType).isEqualTo(PostListButtonType.BUTTON_MORE)
        assertThat(state.actions).hasSize(3)
        assertThat((state.actions[2] as MoreItem).actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_PREVIEW)
        assertThat((state.actions[2] as MoreItem).actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_TRASH)
        assertThat((state.actions[2] as MoreItem).actions).hasSize(2)
    }

    @Test
    fun `verify published private post with local changes eligible for auto upload`() {
        val state = createPostListItemUiState(
                post = createPostModel(status = POST_STATE_PRIVATE, isLocallyChanged = true),
                uploadStatus = createUploadStatus(isEligibleForAutoUpload = true)
        )

        assertThat(state.actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_EDIT)
        assertThat(state.actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_CANCEL_PENDING_AUTO_UPLOAD)
        assertThat(state.actions[2].buttonType).isEqualTo(PostListButtonType.BUTTON_MORE)
        assertThat(state.actions).hasSize(3)
        assertThat((state.actions[2] as MoreItem).actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_PREVIEW)
        assertThat((state.actions[2] as MoreItem).actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_TRASH)
        assertThat((state.actions[2] as MoreItem).actions).hasSize(2)
    }

    @Test
    fun `verify draft with local changes eligible for auto upload`() {
        val state = createPostListItemUiState(
                post = createPostModel(status = POST_STATE_DRAFT, isLocallyChanged = true),
                uploadStatus = createUploadStatus(isEligibleForAutoUpload = true)
        )

        assertThat(state.actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_EDIT)
        assertThat(state.actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_CANCEL_PENDING_AUTO_UPLOAD)
        assertThat(state.actions[2].buttonType).isEqualTo(PostListButtonType.BUTTON_MORE)
        assertThat(state.actions).hasSize(3)

        assertThat((state.actions[2] as MoreItem).actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_PREVIEW)
        assertThat((state.actions[2] as MoreItem).actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_TRASH)
        assertThat((state.actions[2] as MoreItem).actions).hasSize(2)
    }

    @Test
    fun `verify published with local changes eligible for auto upload after a failed upload`() {
        val state = createPostListItemUiState(
                post = createPostModel(status = POST_STATE_PUBLISH, isLocallyChanged = true),
                uploadStatus = createUploadStatus(
                        isEligibleForAutoUpload = true,
                        uploadError = UploadError(MediaError(AUTHORIZATION_REQUIRED))
                )
        )

        assertThat(state.actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_EDIT)
        assertThat(state.actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_CANCEL_PENDING_AUTO_UPLOAD)

        assertThat(state.actions[2].buttonType).isEqualTo(PostListButtonType.BUTTON_MORE)
        assertThat(state.actions).hasSize(3)

        assertThat((state.actions[2] as MoreItem).actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_RETRY)
        assertThat((state.actions[2] as MoreItem).actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_TRASH)
        assertThat((state.actions[2] as MoreItem).actions).hasSize(2)
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

    @Test
    fun `label has state info color after failed upload but eligible for auto upload`() {
        val state = createPostListItemUiState(
                post = createPostModel(status = POST_STATE_PUBLISH, isLocallyChanged = true),
                uploadStatus = createUploadStatus(
                        isEligibleForAutoUpload = true,
                        uploadError = UploadError(MediaError(AUTHORIZATION_REQUIRED))
                )
        )
        assertThat(state.data.statusesColor).isEqualTo(STATE_INFO_COLOR)
    }

    @Test
    fun `label has error color after failed upload when not eligible for auto upload`() {
        val state = createPostListItemUiState(
                post = createPostModel(status = POST_STATE_PUBLISH, isLocallyChanged = true),
                uploadStatus = createUploadStatus(
                        isEligibleForAutoUpload = false,
                        uploadError = UploadError(MediaError(AUTHORIZATION_REQUIRED))
                )
        )
        assertThat(state.data.statusesColor).isEqualTo(ERROR_COLOR)
    }

    @Test
    fun `label has error color on version conflict`() {
        val state = createPostListItemUiState(unhandledConflicts = true)
        assertThat(state.data.statusesColor).isEqualTo(ERROR_COLOR)
    }

    @Test
    fun `label has state info color on auto-save conflict`() {
        val state = createPostListItemUiState(hasAutoSave = true)
        assertThat(state.data.statusesColor).isEqualTo(STATE_INFO_COLOR)
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
    fun `unhandled auto-save label shown for posts with existing auto-save`() {
        val state = createPostListItemUiState(hasAutoSave = true)
        assertThat(state.data.statuses).contains(UiStringRes(R.string.local_post_autosave_revision_available))
    }

    @Test
    fun `uploading post label shown when the post is being uploaded`() {
        val state = createPostListItemUiState(uploadStatus = createUploadStatus(isUploading = true))
        assertThat(state.data.statuses).contains(UiStringRes(R.string.post_uploading))
    }

    @Test
    fun `uploading draft label shown when the draft is being uploaded`() {
        val state = createPostListItemUiState(
                uploadStatus = createUploadStatus(isUploading = true),
                post = createPostModel(status = POST_STATE_DRAFT)
        )
        assertThat(state.data.statuses).contains(UiStringRes(R.string.post_uploading_draft))
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
    fun `generic error message shown when upload fails from unknown reason`() {
        val errorMsg = "testing error message"
        val state = createPostListItemUiState(
                uploadStatus = createUploadStatus(uploadError = UploadError(PostError(GENERIC_ERROR, errorMsg)))
        )
        assertThat(state.data.statuses).contains(UiStringRes(R.string.error_generic_error))
    }

    @Test
    fun `given a mix of info and error statuses, only the error status is shown`() {
        val state = createPostListItemUiState(
                post = createPostModel(isLocallyChanged = true, status = POST_STATE_PRIVATE),
                uploadStatus = createUploadStatus(uploadError = UploadError(MediaError(AUTHORIZATION_REQUIRED)))
        )
        assertThat(state.data.statuses).containsOnly(UiStringRes(R.string.error_media_recover_post))
    }

    @Test
    fun `media upload error shown with specific message for pending post eligible for auto-upload`() {
        val state = createPostListItemUiState(
                post = createPostModel(isLocallyChanged = true, status = POST_STATE_PENDING),
                uploadStatus = createUploadStatus(
                        uploadError = UploadError(MediaError(AUTHORIZATION_REQUIRED)),
                        isEligibleForAutoUpload = true
                )
        )
        assertThat(state.data.statuses)
                .containsOnly(UiStringRes(R.string.error_media_recover_post_not_submitted_retrying))
    }

    @Test
    fun `media upload error shown with specific message for pending post not eligible for auto-upload`() {
        val state = createPostListItemUiState(
                post = createPostModel(isLocallyChanged = true, status = POST_STATE_PENDING),
                uploadStatus = createUploadStatus(
                        uploadError = UploadError(MediaError(AUTHORIZATION_REQUIRED)),
                        isEligibleForAutoUpload = false,
                        retryWillPushChanges = true
                )
        )
        assertThat(state.data.statuses).containsOnly(UiStringRes(R.string.error_media_recover_post_not_submitted))
    }

    @Test
    fun `media upload error shown with specific message for scheduled post eligible for auto-upload`() {
        val state = createPostListItemUiState(
                post = createPostModel(isLocallyChanged = true, status = POST_STATE_SCHEDULED),
                uploadStatus = createUploadStatus(
                        uploadError = UploadError(MediaError(AUTHORIZATION_REQUIRED)),
                        isEligibleForAutoUpload = true
                )
        )
        assertThat(state.data.statuses)
                .containsOnly(UiStringRes(R.string.error_media_recover_post_not_scheduled_retrying))
    }

    @Test
    fun `media upload error shown with specific message for scheduled post not eligible for auto-upload`() {
        val state = createPostListItemUiState(
                post = createPostModel(isLocallyChanged = true, status = POST_STATE_SCHEDULED),
                uploadStatus = createUploadStatus(
                        uploadError = UploadError(MediaError(AUTHORIZATION_REQUIRED)),
                        isEligibleForAutoUpload = false,
                        retryWillPushChanges = true
                )
        )
        assertThat(state.data.statuses).containsOnly(UiStringRes(R.string.error_media_recover_post_not_scheduled))
    }

    @Test
    fun `retrying media upload shown for draft eligible for auto-upload`() {
        val state = createPostListItemUiState(
                post = createPostModel(isLocallyChanged = true, status = POST_STATE_DRAFT),
                uploadStatus = createUploadStatus(
                        uploadError = UploadError(MediaError(AUTHORIZATION_REQUIRED)),
                        isEligibleForAutoUpload = true
                )
        )
        assertThat(state.data.statuses).containsOnly(UiStringRes(R.string.error_generic_error_retrying))
    }

    @Test
    fun `base media upload error shown for draft not eligible for auto-upload`() {
        val state = createPostListItemUiState(
                post = createPostModel(isLocallyChanged = true, status = POST_STATE_DRAFT),
                uploadStatus = createUploadStatus(
                        uploadError = UploadError(MediaError(AUTHORIZATION_REQUIRED)),
                        isEligibleForAutoUpload = false,
                        retryWillPushChanges = false
                )
        )
        assertThat(state.data.statuses).containsOnly(UiStringRes(R.string.error_media_recover_post))
    }

    @Test
    fun `base upload error shown on GENERIC ERROR and not eligible for auto upload`() {
        val state = createPostListItemUiState(
                post = createPostModel(isLocallyChanged = true, status = POST_STATE_DRAFT),
                uploadStatus = createUploadStatus(
                        uploadError = UploadError(PostError(GENERIC_ERROR)),
                        isEligibleForAutoUpload = false,
                        retryWillPushChanges = false
                )
        )
        assertThat(state.data.statuses).containsOnly(UiStringRes(R.string.error_generic_error))
    }

    @Test
    fun `retrying upload shown for draft eligible for auto-upload`() {
        val state = createPostListItemUiState(
                post = createPostModel(isLocallyChanged = true, status = POST_STATE_DRAFT),
                uploadStatus = createUploadStatus(
                        uploadError = UploadError(PostError(GENERIC_ERROR)),
                        isEligibleForAutoUpload = true,
                        retryWillPushChanges = true
                )
        )
        assertThat(state.data.statuses).containsOnly(UiStringRes(R.string.error_generic_error_retrying))
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

    @Test
    fun `show only delete and move to draft buttons on trashed posts`() {
        val state = createPostListItemUiState(post = createPostModel(POST_STATE_TRASHED))
        assertThat(state.actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_MOVE_TO_DRAFT)
        assertThat(state.actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_DELETE_PERMANENTLY)
        assertThat(state.actions).hasSize(2)
    }

    @Test
    fun `show delete button on local draft with a media upload error`() {
        val state = createPostListItemUiState(
                post = createPostModel(isLocalDraft = true, isLocallyChanged = true),
                uploadStatus = createUploadStatus(uploadError = UploadError(MediaError(MediaErrorType.GENERIC_ERROR)))
        )
        assertThat(state.actions[0].buttonType).isEqualTo(PostListButtonType.BUTTON_EDIT)
        assertThat(state.actions[1].buttonType).isEqualTo(PostListButtonType.BUTTON_RETRY)
        assertThat(state.actions[2].buttonType).isEqualTo(PostListButtonType.BUTTON_DELETE)
        assertThat(state.actions).hasSize(3)
    }

    @Test
    fun `pending publish post label shown when post eligible for auto-upload`() {
        val state = createPostListItemUiState(
                post = createPostModel(isLocallyChanged = true, status = POST_STATE_PUBLISH),
                uploadStatus = createUploadStatus(isEligibleForAutoUpload = true)
        )

        assertThat((state.data.statuses[0] as UiStringRes).stringRes)
                .isEqualTo(R.string.post_waiting_for_connection_publish)
    }

    @Test
    fun `pending schedule label shown when post eligible for auto-upload`() {
        val state = createPostListItemUiState(
                post = createPostModel(isLocallyChanged = true, status = POST_STATE_SCHEDULED),
                uploadStatus = createUploadStatus(isEligibleForAutoUpload = true)
        )

        assertThat((state.data.statuses[0] as UiStringRes).stringRes)
                .isEqualTo(R.string.post_waiting_for_connection_scheduled)
    }

    @Test
    fun `pending publish private post label shown when post eligible for auto-upload`() {
        val state = createPostListItemUiState(
                post = createPostModel(isLocallyChanged = true, status = POST_STATE_PRIVATE),
                uploadStatus = createUploadStatus(isEligibleForAutoUpload = true)
        )

        assertThat((state.data.statuses[0] as UiStringRes).stringRes)
                .isEqualTo(R.string.post_waiting_for_connection_private)
    }

    @Test
    fun `pending submit post label shown when post eligible for auto-upload`() {
        val state = createPostListItemUiState(
                post = createPostModel(isLocallyChanged = true, status = POST_STATE_PENDING),
                uploadStatus = createUploadStatus(isEligibleForAutoUpload = true)
        )

        assertThat((state.data.statuses[0] as UiStringRes).stringRes)
                .isEqualTo(R.string.post_waiting_for_connection_pending)
    }

    @Test
    fun `local changes post label shown when draft eligible for auto-upload`() {
        val state = createPostListItemUiState(
                post = createPostModel(isLocallyChanged = true, status = POST_STATE_DRAFT),
                uploadStatus = createUploadStatus(isEligibleForAutoUpload = true)
        )

        assertThat((state.data.statuses[0] as UiStringRes).stringRes)
                .isEqualTo(R.string.post_waiting_for_connection_draft)
    }

    fun `date and author label contains both date and authorDisplayName`() {
        // Arrange
        val authorDisplayName = "John Novak"
        val state = createPostListItemUiState(
                post = createPostModel(
                        authorDisplayName = authorDisplayName
                ), formattedDate = FORMATTER_DATE
        )

        // Assert
        assertThat((state.data.dateAndAuthor as UiStringText).text).isEqualTo("$FORMATTER_DATE  ·  $authorDisplayName")
    }

    @Test
    fun `date and author label contains only date when author name is null`() {
        // Arrange
        val state = createPostListItemUiState(
                post = createPostModel(
                        authorDisplayName = null
                ), formattedDate = FORMATTER_DATE
        )

        // Assert
        assertThat((state.data.dateAndAuthor as UiStringText).text).isEqualTo(FORMATTER_DATE)
    }

    @Test
    fun `date and author label contains only date when author name is empty`() {
        // Arrange
        val state = createPostListItemUiState(
                post = createPostModel(
                        authorDisplayName = ""
                ), formattedDate = FORMATTER_DATE
        )

        // Assert
        assertThat((state.data.dateAndAuthor as UiStringText).text).isEqualTo(FORMATTER_DATE)
    }

    @Test
    fun `author name is displayed when author filter is EVERYONE`() {
        // Arrange
        val authorDisplayName = "John Novak"
        val state = createPostListItemUiState(
                authorFilterSelection = EVERYONE,
                post = createPostModel(
                        authorDisplayName = authorDisplayName
                ), formattedDate = FORMATTER_DATE
        )

        // Assert
        assertThat((state.data.dateAndAuthor as UiStringText).text).isEqualTo("$FORMATTER_DATE  ·  $authorDisplayName")
    }

    @Test
    fun `author name is NOT displayed when author filter is ME`() {
        // Arrange
        val authorDisplayName = "John Novak"
        val state = createPostListItemUiState(
                authorFilterSelection = ME,
                post = createPostModel(
                        authorDisplayName = authorDisplayName
                ), formattedDate = FORMATTER_DATE
        )

        // Assert
        assertThat((state.data.dateAndAuthor as UiStringText).text).isEqualTo(FORMATTER_DATE)
    }

    @Test
    fun `when a post is locally changed and is local draft only "Local draft" label is displayed`() {
        // Arrange
        val state = createPostListItemUiState(
                post = createPostModel(isLocallyChanged = true, isLocalDraft = true)
        )

        // Assert
        assertThat(state.data.statuses).containsOnly(UiStringRes(R.string.local_draft))
    }

    private fun createPostModel(
        status: String = POST_STATE_PUBLISH,
        isLocalDraft: Boolean = false,
        isLocallyChanged: Boolean = false,
        authorDisplayName: String? = null
    ): PostModel {
        val post = PostModel()
        post.setStatus(status)
        post.setIsLocalDraft(isLocalDraft)
        post.setIsLocallyChanged(isLocallyChanged)
        post.setAuthorDisplayName(authorDisplayName)
        return post
    }

    private fun createPostListItemUiState(
        authorFilterSelection: AuthorFilterSelection = EVERYONE,
        post: PostModel = PostModel(),
        uploadStatus: PostListItemUploadStatus = createUploadStatus(),
        unhandledConflicts: Boolean = false,
        hasAutoSave: Boolean = false,
        capabilitiesToPublish: Boolean = true,
        statsSupported: Boolean = true,
        featuredImageUrl: String? = null,
        formattedDate: String = FORMATTER_DATE,
        performingCriticalAction: Boolean = false,
        onAction: (PostModel, PostListButtonType, AnalyticsTracker.Stat) -> Unit = { _, _, _ -> }
    ): PostListItemUiState = helper.createPostListItemUiState(
            authorFilterSelection,
            post = post,
            uploadStatus = uploadStatus,
            unhandledConflicts = unhandledConflicts,
            hasAutoSave = hasAutoSave,
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
        hasPendingMediaUpload: Boolean = false,
        isEligibleForAutoUpload: Boolean = false,
        retryWillPushChanges: Boolean = false
    ): PostListItemUploadStatus =
            PostListItemUploadStatus(
                    uploadError = uploadError,
                    mediaUploadProgress = mediaUploadProgress,
                    isUploading = isUploading,
                    isUploadingOrQueued = isUploadingOrQueued,
                    isQueued = isQueued,
                    isUploadFailed = isUploadFailed,
                    hasInProgressMediaUpload = hasInProgressMediaUpload,
                    hasPendingMediaUpload = hasPendingMediaUpload,
                    isEligibleForAutoUpload = isEligibleForAutoUpload,
                    uploadWillPushChanges = retryWillPushChanges
                    )

    private fun createGenericError(): UploadError = UploadError(PostError(GENERIC_ERROR))
}
