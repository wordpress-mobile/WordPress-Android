package org.wordpress.android.viewmodel.posts

import android.support.annotation.ColorRes
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat.POST_LIST_BUTTON_PRESSED
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.PENDING
import org.wordpress.android.fluxc.model.post.PostStatus.PRIVATE
import org.wordpress.android.ui.posts.PostAdapterItemUploadStatus
import org.wordpress.android.ui.posts.PostUtils
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.uploads.UploadUtils
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.AppLog
import org.wordpress.android.widgets.PostListButtonType
import org.wordpress.android.widgets.PostListButtonType.BUTTON_EDIT
import javax.inject.Inject

class PostListItemUiStateHelper @Inject constructor(private val appPrefsWrapper: AppPrefsWrapper) {
    fun createPostListItemUiModel(
        post: PostModel,
        uploadStatus: PostAdapterItemUploadStatus,
        unhandledConflicts: Boolean,
        capabilitiesToPublish: Boolean,
        statsSupported: Boolean,
        featuredImageUrl: String?,
        onAction: (PostModel, PostListButtonType, AnalyticsTracker.Stat) -> Unit
    ): PostListItemUiState {
        val title = if (post.title.isNotBlank()) {
            UiStringText(StringEscapeUtils.unescapeHtml4(post.title))
        } else UiStringRes(string.untitled_in_parentheses)

        val excerpt = PostUtils.getPostListExcerptFromPost(post)
                .takeIf { !it.isNullOrBlank() }
                ?.let { StringEscapeUtils.unescapeHtml4(it) }
                ?.let { PostUtils.collapseShortcodes(it) }
                ?.let { UiStringText(it) }

        return PostListItemUiState(
                post.remotePostId,
                post.id,
                title,
                excerpt,
                featuredImageUrl,
                UiStringText(PostUtils.getFormattedDate(post)),  // TODO How do I get name of the author
                getStatusLabels(post, uploadStatus, unhandledConflicts),
                getStatusLabelsColor(post, uploadStatus, unhandledConflicts),
                createActions(post, onAction, uploadStatus, capabilitiesToPublish, statsSupported),
                showProgress = shouldShowProgress(uploadStatus),
                showOverlay = shouldShowOverlay(uploadStatus),
                onSelected = {
                    onAction.invoke(post, BUTTON_EDIT, AnalyticsTracker.Stat.POST_LIST_ITEM_SELECTED)
                }
        )
    }

    private fun shouldShowProgress(uploadStatus: PostAdapterItemUploadStatus): Boolean {
        return !uploadStatus.isUploadFailed && (uploadStatus.isUploadingOrQueued || uploadStatus.hasInProgressMediaUpload)
    }

    private fun shouldShowOverlay(uploadStatus: PostAdapterItemUploadStatus): Boolean {
        // show overlay when post upload is in progress or (media upload is in progress and the user is not using Aztec)
        return uploadStatus.isUploading || (!appPrefsWrapper.isAztecEditorEnabled && uploadStatus.isUploadingOrQueued)
    }

    private fun getStatusLabels(
        post: PostModel,
        uploadStatus: PostAdapterItemUploadStatus,
        hasUnhandledConflicts: Boolean
    ): UiString? {
        val postStatus: PostStatus = PostStatus.fromPost(post)
        // TODO how can a post be published and a localDraft at the same time?
        val uploadError = uploadStatus.uploadError

        return if (uploadError != null && !uploadStatus.hasInProgressMediaUpload) {
            when {
                uploadError.mediaError != null -> UiStringRes(string.error_media_recover_post)
                uploadError.postError != null -> UploadUtils.getErrorMessageResIdFromPostError(
                        false,
                        uploadError.postError
                )
                else -> {
                    AppLog.e(AppLog.T.POSTS, "MediaError and postError are both null.")
                    null
                }
            }
        } else if (uploadStatus.isUploading) {
            UiStringRes(string.post_uploading)
        } else if (uploadStatus.hasInProgressMediaUpload) {
            UiStringRes(string.uploading_media)
        } else if (uploadStatus.isQueued || uploadStatus.hasPendingMediaUpload) {
            // the Post (or its related media if such a thing exist) *is strictly* queued
            UiStringRes(string.post_queued)
        } else if (hasUnhandledConflicts) {
            UiStringRes(string.local_post_is_conflicted)
        } else if (post.isLocalDraft) {
            UiStringRes(string.local_draft)
        } else if (post.isLocallyChanged) {
            UiStringRes(string.local_changes)
        } else {
            when (postStatus) {
                PostStatus.PRIVATE -> UiStringRes(string.post_status_post_private)
                PostStatus.PENDING -> UiStringRes(string.post_status_pending_review)
                PostStatus.UNKNOWN, // TODO Unknown PostStatus
                PostStatus.DRAFT,
                PostStatus.SCHEDULED,
                PostStatus.TRASHED,
                PostStatus.PUBLISHED ->
                    null
            }
        }
    }

    @ColorRes private fun getStatusLabelsColor(
        post: PostModel,
        uploadStatus: PostAdapterItemUploadStatus,
        hasUnhandledConflicts: Boolean
    ): Int? {
        val postStatus: PostStatus = PostStatus.fromPost(post)

        return if (uploadStatus.uploadError != null
                && !uploadStatus.hasInProgressMediaUpload
        ) {
            R.color.alert_red
        } else if (uploadStatus.isQueued
                || uploadStatus.hasPendingMediaUpload
                || uploadStatus.hasInProgressMediaUpload
                || uploadStatus.isUploading
                || hasUnhandledConflicts
        ) {
            R.color.wp_grey_darken_20
        } else if (post.isLocalDraft
                || post.isLocallyChanged
                || postStatus == PRIVATE
                || postStatus == PENDING
        ) {
            R.color.alert_yellow_dark
        } else {
            null
        }
    }

    private fun createActions(
        post: PostModel,
        onAction: (PostModel, PostListButtonType, AnalyticsTracker.Stat) -> Unit,
        uploadStatus: PostAdapterItemUploadStatus,
        siteHasCapabilitiesToPublish: Boolean,
        statsSupported: Boolean
    ): List<PostListItemAction> {
        val postStatus: PostStatus = PostStatus.fromPost(post)
        val canRetryUpload = uploadStatus.uploadError != null && !uploadStatus.hasInProgressMediaUpload
        val canPublishPost = !uploadStatus.isUploadingOrQueued
                && (post.isLocallyChanged || post.isLocalDraft || postStatus == PostStatus.DRAFT)
        val canShowStats = statsSupported
                && postStatus == PostStatus.PUBLISHED
                && !post.isLocalDraft
                && !post.isLocallyChanged
        val canShowViewButton = !canRetryUpload
        val canShowPublishButton = canRetryUpload || canPublishPost

        val buttonTypes = ArrayList<PostListButtonType>()
        buttonTypes.add(PostListButtonType.BUTTON_EDIT)

        // publish button is re-purposed depending on the situation
        if (canShowPublishButton) {
            buttonTypes.add(
                    if (!siteHasCapabilitiesToPublish) {
                        PostListButtonType.BUTTON_SUBMIT
                    } else if (canRetryUpload) {
                        PostListButtonType.BUTTON_RETRY
                    } else if (postStatus == PostStatus.SCHEDULED && post.isLocallyChanged) {
                        PostListButtonType.BUTTON_SYNC
                    } else {
                        PostListButtonType.BUTTON_PUBLISH
                    }
            )
        }

        if (canShowViewButton) {
            buttonTypes.add(
                    if (post.isLocalDraft || post.isLocallyChanged) {
                        PostListButtonType.BUTTON_PREVIEW
                    } else {
                        PostListButtonType.BUTTON_VIEW
                    }
            )
        }

        if (postStatus != PostStatus.TRASHED) {
            buttonTypes.add(PostListButtonType.BUTTON_TRASH)
        } else {
            buttonTypes.add(PostListButtonType.BUTTON_DELETE)
            buttonTypes.add(PostListButtonType.BUTTON_RESTORE)
        }

        if (canShowStats) {
            buttonTypes.add(PostListButtonType.BUTTON_STATS)
        }

        // TODO if buttonTypes > 3 -> decide whether we want to show more instead

        return buttonTypes.map {
            PostListItemAction(it) { btnType ->
                onAction.invoke(post, btnType, POST_LIST_BUTTON_PRESSED)
            }
        }
    }
}
