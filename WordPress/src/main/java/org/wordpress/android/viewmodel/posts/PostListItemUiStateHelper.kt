package org.wordpress.android.viewmodel.posts

import android.support.annotation.ColorRes
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat.POST_LIST_BUTTON_PRESSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.POST_LIST_ITEM_SELECTED
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.PENDING
import org.wordpress.android.fluxc.model.post.PostStatus.PRIVATE
import org.wordpress.android.fluxc.model.post.PostStatus.SCHEDULED
import org.wordpress.android.fluxc.store.UploadStore.UploadError
import org.wordpress.android.ui.posts.PostUtils
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.uploads.UploadUtils
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.POSTS
import org.wordpress.android.viewmodel.posts.PostListItemIdentifier.LocalPostId
import org.wordpress.android.viewmodel.posts.PostListItemIdentifier.RemotePostId
import org.wordpress.android.viewmodel.posts.PostListItemType.PostListItemUiState
import org.wordpress.android.widgets.PostListButtonType
import org.wordpress.android.widgets.PostListButtonType.BUTTON_EDIT
import org.wordpress.android.widgets.PostListButtonType.BUTTON_PREVIEW
import org.wordpress.android.widgets.PostListButtonType.BUTTON_PUBLISH
import org.wordpress.android.widgets.PostListButtonType.BUTTON_RETRY
import org.wordpress.android.widgets.PostListButtonType.BUTTON_SUBMIT
import org.wordpress.android.widgets.PostListButtonType.BUTTON_SYNC
import org.wordpress.android.widgets.PostListButtonType.BUTTON_VIEW
import java.lang.Math.max
import javax.inject.Inject

private const val MAX_NUMBER_OF_VISIBLE_ACTIONS_STANDARD = 3
const val ERROR_COLOR = R.color.error
const val PROGRESS_INFO_COLOR = R.color.neutral_500
const val STATE_INFO_COLOR = R.color.warning_dark

/**
 * Helper class which encapsulates logic for creating UiStates for items in the PostsList.
 */
class PostListItemUiStateHelper @Inject constructor(private val appPrefsWrapper: AppPrefsWrapper) {
    fun createPostListItemUiState(
        post: PostModel,
        uploadStatus: PostListItemUploadStatus,
        unhandledConflicts: Boolean,
        capabilitiesToPublish: Boolean,
        statsSupported: Boolean,
        featuredImageUrl: String?,
        formattedDate: String,
        performingCriticalAction: Boolean,
        onAction: (PostModel, PostListButtonType, AnalyticsTracker.Stat) -> Unit
    ): PostListItemUiState {
        val postStatus: PostStatus = PostStatus.fromPost(post)

        val onButtonClicked = { buttonType: PostListButtonType ->
            onAction.invoke(post, buttonType, POST_LIST_BUTTON_PRESSED)
        }
        val buttonTypes = createButtonTypes(
                postStatus = postStatus,
                isLocalDraft = post.isLocalDraft,
                isLocallyChanged = post.isLocallyChanged,
                uploadStatus = uploadStatus,
                siteHasCapabilitiesToPublish = capabilitiesToPublish,
                statsSupported = statsSupported
        )
        val defaultActions = createDefaultViewActions(buttonTypes, onButtonClicked)
        val compactActions = createCompactViewActions(buttonTypes, onButtonClicked)

        val remotePostId = RemotePostId(RemoteId(post.remotePostId))
        val localPostId = LocalPostId(LocalId(post.id))
        val title = getTitle(post = post)
        val date = UiStringText(text = formattedDate)
        val statuses = getStatuses(
                postStatus = postStatus,
                isLocalDraft = post.isLocalDraft,
                isLocallyChanged = post.isLocallyChanged,
                uploadStatus = uploadStatus,
                hasUnhandledConflicts = unhandledConflicts
        )
        val statusesColor = getStatusesColor(
                postStatus = postStatus,
                isLocalDraft = post.isLocalDraft,
                isLocallyChanged = post.isLocallyChanged,
                uploadStatus = uploadStatus,
                hasUnhandledConflicts = unhandledConflicts
        )
        val statusesDelimeter = UiStringRes(R.string.multiple_status_label_delimiter)
        val onSelected = {
            onAction.invoke(post, BUTTON_EDIT, POST_LIST_ITEM_SELECTED)
        }
        val itemUiData = PostListItemUiStateData(
                remotePostId = remotePostId,
                localPostId = localPostId,
                title = title,
                excerpt = getExcerpt(post = post),
                imageUrl = featuredImageUrl,
                date = date,
                statuses = statuses,
                statusesColor = statusesColor,
                statusesDelimiter = statusesDelimeter,
                showProgress = shouldShowProgress(
                        uploadStatus = uploadStatus,
                        performingCriticalAction = performingCriticalAction
                ),
                showOverlay = shouldShowOverlay(
                        uploadStatus = uploadStatus,
                        performingCriticalAction = performingCriticalAction
                )
        )

        return PostListItemUiState(
                data = itemUiData,
                actions = defaultActions,
                compactActions = compactActions,
                onSelected = onSelected
        )
    }

    private fun getTitle(post: PostModel): UiString {
        return if (post.title.isNotBlank()) {
            UiStringText(StringEscapeUtils.unescapeHtml4(post.title))
        } else UiStringRes(string.untitled_in_parentheses)
    }

    private fun getExcerpt(post: PostModel): UiString? =
            PostUtils.getPostListExcerptFromPost(post)
                    .takeIf { !it.isNullOrBlank() }
                    ?.let { StringEscapeUtils.unescapeHtml4(it) }
                    ?.let { PostUtils.collapseShortcodes(it) }
                    ?.let { UiStringText(it) }

    private fun shouldShowProgress(uploadStatus: PostListItemUploadStatus, performingCriticalAction: Boolean): Boolean {
        return performingCriticalAction || (!uploadStatus.isUploadFailed &&
                (uploadStatus.isUploadingOrQueued || uploadStatus.hasInProgressMediaUpload))
    }

    private fun getStatuses(
        postStatus: PostStatus,
        isLocalDraft: Boolean,
        isLocallyChanged: Boolean,
        uploadStatus: PostListItemUploadStatus,
        hasUnhandledConflicts: Boolean
    ): List<UiString> {
        val labels: MutableList<UiString> = ArrayList()

        val isError = uploadStatus.uploadError != null && !uploadStatus.hasInProgressMediaUpload

        when {
            isError && uploadStatus.uploadError != null -> {
                getErrorLabel(uploadStatus.uploadError)?.let { labels.add(it) }
            }
            uploadStatus.isUploading -> labels.add(UiStringRes(string.post_uploading))
            uploadStatus.hasInProgressMediaUpload -> labels.add(UiStringRes(string.uploading_media))
            uploadStatus.isQueued || uploadStatus.hasPendingMediaUpload -> labels.add(UiStringRes(string.post_queued))
            hasUnhandledConflicts -> labels.add(UiStringRes(string.local_post_is_conflicted))
        }

        // we want to show either single error/progress label or 0-n info labels.
        if (labels.isEmpty()) {
            if (isLocalDraft) {
                labels.add(UiStringRes(string.local_draft))
            }
            if (isLocallyChanged) {
                labels.add(UiStringRes(string.local_changes))
            }
            if (postStatus == PRIVATE) {
                labels.add(UiStringRes(string.post_status_post_private))
            }
            if (postStatus == PENDING) {
                labels.add(UiStringRes(string.post_status_pending_review))
            }
        }
        return labels
    }

    private fun getErrorLabel(uploadError: UploadError): UiString? {
        return when {
            uploadError.mediaError != null -> UiStringRes(string.error_media_recover_post)
            uploadError.postError != null -> UploadUtils.getErrorMessageResIdFromPostError(
                    false,
                    uploadError.postError
            )
            else -> {
                val errorMsg = "MediaError and postError are both null."
                if (BuildConfig.DEBUG) {
                    throw IllegalStateException(errorMsg)
                } else {
                    AppLog.e(POSTS, errorMsg)
                }
                UiStringRes(R.string.error_generic)
            }
        }
    }

    @ColorRes private fun getStatusesColor(
        postStatus: PostStatus,
        isLocalDraft: Boolean,
        isLocallyChanged: Boolean,
        uploadStatus: PostListItemUploadStatus,
        hasUnhandledConflicts: Boolean
    ): Int? {
        val isError = (uploadStatus.uploadError != null && !uploadStatus.hasInProgressMediaUpload) ||
                hasUnhandledConflicts
        val isProgressInfo = uploadStatus.isQueued || uploadStatus.hasPendingMediaUpload ||
                uploadStatus.hasInProgressMediaUpload || uploadStatus.isUploading
        val isStateInfo = isLocalDraft || isLocallyChanged || postStatus == PRIVATE || postStatus == PENDING

        return when {
            isError -> ERROR_COLOR
            isProgressInfo -> PROGRESS_INFO_COLOR
            isStateInfo -> STATE_INFO_COLOR
            else -> null
        }
    }

    private fun shouldShowOverlay(uploadStatus: PostListItemUploadStatus, performingCriticalAction: Boolean): Boolean {
        // show overlay when post upload is in progress or (media upload is in progress and the user is not using Aztec)
        return performingCriticalAction ||
                (uploadStatus.isUploading ||
                        (!appPrefsWrapper.isAztecEditorEnabled && uploadStatus.isUploadingOrQueued))
    }

    private fun createButtonTypes(
        postStatus: PostStatus,
        isLocalDraft: Boolean,
        isLocallyChanged: Boolean,
        uploadStatus: PostListItemUploadStatus,
        siteHasCapabilitiesToPublish: Boolean,
        statsSupported: Boolean
    ): List<PostListButtonType> {
        val canRetryUpload = uploadStatus.uploadError != null && !uploadStatus.hasInProgressMediaUpload
        val canPublishPost = !uploadStatus.isUploadingOrQueued &&
                (isLocallyChanged || isLocalDraft || postStatus == PostStatus.DRAFT)
        val canShowStats = statsSupported &&
                postStatus == PostStatus.PUBLISHED &&
                !isLocalDraft &&
                !isLocallyChanged
        val canShowViewButton = !canRetryUpload
        val canShowPublishButton = canRetryUpload || canPublishPost
        val buttonTypes = ArrayList<PostListButtonType>()

        buttonTypes.add(PostListButtonType.BUTTON_EDIT)
        if (canShowPublishButton) {
            buttonTypes.add(
                    if (!siteHasCapabilitiesToPublish) {
                        BUTTON_SUBMIT
                    } else if (canRetryUpload) {
                        BUTTON_RETRY
                    } else if (postStatus == SCHEDULED && isLocallyChanged) {
                        BUTTON_SYNC
                    } else {
                        BUTTON_PUBLISH
                    }
            )
        }

        if (canShowViewButton) {
            buttonTypes.add(
                    if (isLocalDraft || isLocallyChanged) {
                        BUTTON_PREVIEW
                    } else {
                        BUTTON_VIEW
                    }
            )
        }

        when {
            isLocalDraft -> buttonTypes.add(PostListButtonType.BUTTON_DELETE)
            postStatus == PostStatus.TRASHED -> {
                buttonTypes.add(PostListButtonType.BUTTON_DELETE)
                buttonTypes.add(PostListButtonType.BUTTON_MOVE_TO_DRAFT)
            }
            postStatus != PostStatus.TRASHED -> buttonTypes.add(PostListButtonType.BUTTON_TRASH)
        }

        if (canShowStats) {
            buttonTypes.add(PostListButtonType.BUTTON_STATS)
        }

        return buttonTypes
    }

    private fun createDefaultViewActions(
        buttonTypes: List<PostListButtonType>,
        onButtonClicked: (PostListButtonType) -> Unit
    ): List<PostListItemAction> {
        val createSinglePostListItem = { buttonType: PostListButtonType ->
            PostListItemAction.SingleItem(buttonType, onButtonClicked)
        }
        return if (buttonTypes.size > MAX_NUMBER_OF_VISIBLE_ACTIONS_STANDARD) {
            val visibleItems = buttonTypes.take(MAX_NUMBER_OF_VISIBLE_ACTIONS_STANDARD - 1)
                    .map(createSinglePostListItem)
            val itemsUnderMore = buttonTypes.subList(
                    max(MAX_NUMBER_OF_VISIBLE_ACTIONS_STANDARD - 1, 0),
                    buttonTypes.size
            )
                    .map(createSinglePostListItem)

            visibleItems.plus(PostListItemAction.MoreItem(itemsUnderMore, onButtonClicked))
        } else {
            buttonTypes.map(createSinglePostListItem)
        }
    }

    private fun createCompactViewActions(
        buttonTypes: List<PostListButtonType>,
        onButtonClicked: (PostListButtonType) -> Unit
    ): PostListItemAction.MoreItem {
        val allItems = buttonTypes.map { buttonType: PostListButtonType ->
            PostListItemAction.SingleItem(buttonType, onButtonClicked)
        }
        return PostListItemAction.MoreItem(allItems, onButtonClicked)
    }
}
