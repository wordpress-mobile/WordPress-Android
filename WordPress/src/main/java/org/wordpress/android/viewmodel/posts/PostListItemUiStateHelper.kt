package org.wordpress.android.viewmodel.posts

import android.text.TextUtils
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat.POST_LIST_BUTTON_PRESSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.POST_LIST_ITEM_SELECTED
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.DRAFT
import org.wordpress.android.fluxc.model.post.PostStatus.PENDING
import org.wordpress.android.fluxc.model.post.PostStatus.PRIVATE
import org.wordpress.android.fluxc.model.post.PostStatus.PUBLISHED
import org.wordpress.android.fluxc.model.post.PostStatus.SCHEDULED
import org.wordpress.android.fluxc.model.post.PostStatus.TRASHED
import org.wordpress.android.fluxc.model.post.PostStatus.UNKNOWN
import org.wordpress.android.ui.posts.AuthorFilterSelection
import org.wordpress.android.ui.posts.AuthorFilterSelection.EVERYONE
import org.wordpress.android.ui.posts.AuthorFilterSelection.ME
import org.wordpress.android.ui.posts.PostModelUploadStatusTracker
import org.wordpress.android.ui.posts.PostUtils
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.uploads.UploadUtils
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.POSTS
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.NothingToUpload
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadFailed
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadQueued
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadWaitingForConnection
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadingMedia
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadingPost
import org.wordpress.android.viewmodel.pages.PostPageListLabelColorUseCase
import org.wordpress.android.viewmodel.posts.PostListItemIdentifier.LocalPostId
import org.wordpress.android.viewmodel.posts.PostListItemIdentifier.RemotePostId
import org.wordpress.android.viewmodel.posts.PostListItemType.PostListItemUiState
import org.wordpress.android.viewmodel.uistate.ProgressBarUiState
import org.wordpress.android.widgets.PostListButtonType
import org.wordpress.android.widgets.PostListButtonType.BUTTON_CANCEL_PENDING_AUTO_UPLOAD
import org.wordpress.android.widgets.PostListButtonType.BUTTON_DELETE
import org.wordpress.android.widgets.PostListButtonType.BUTTON_DELETE_PERMANENTLY
import org.wordpress.android.widgets.PostListButtonType.BUTTON_EDIT
import org.wordpress.android.widgets.PostListButtonType.BUTTON_MOVE_TO_DRAFT
import org.wordpress.android.widgets.PostListButtonType.BUTTON_PREVIEW
import org.wordpress.android.widgets.PostListButtonType.BUTTON_PUBLISH
import org.wordpress.android.widgets.PostListButtonType.BUTTON_RETRY
import org.wordpress.android.widgets.PostListButtonType.BUTTON_SHOW_MOVE_TRASHED_POST_TO_DRAFT_DIALOG
import org.wordpress.android.widgets.PostListButtonType.BUTTON_STATS
import org.wordpress.android.widgets.PostListButtonType.BUTTON_SUBMIT
import org.wordpress.android.widgets.PostListButtonType.BUTTON_SYNC
import org.wordpress.android.widgets.PostListButtonType.BUTTON_TRASH
import org.wordpress.android.widgets.PostListButtonType.BUTTON_VIEW
import javax.inject.Inject

private const val MAX_NUMBER_OF_VISIBLE_ACTIONS_STANDARD = 3
/**
 * Helper class which encapsulates logic for creating UiStates for items in the PostsList.
 */
class PostListItemUiStateHelper @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val uploadUiStateUseCase: PostModelUploadUiStateUseCase,
    private val labelColorUseCase: PostPageListLabelColorUseCase
) {
    fun createPostListItemUiState(
        authorFilterSelection: AuthorFilterSelection,
        post: PostModel,
        site: SiteModel,
        unhandledConflicts: Boolean,
        hasAutoSave: Boolean,
        capabilitiesToPublish: Boolean,
        statsSupported: Boolean,
        featuredImageUrl: String?,
        formattedDate: String,
        performingCriticalAction: Boolean,
        uploadStatusTracker: PostModelUploadStatusTracker,
        onAction: (PostModel, PostListButtonType, AnalyticsTracker.Stat) -> Unit
    ): PostListItemUiState {
        val postStatus: PostStatus = PostStatus.fromPost(post)
        val uploadUiState = uploadUiStateUseCase.createUploadUiState(post, site, uploadStatusTracker)

        val onButtonClicked = { buttonType: PostListButtonType ->
            onAction.invoke(post, buttonType, POST_LIST_BUTTON_PRESSED)
        }
        val buttonTypes = createButtonTypes(
                postStatus = postStatus,
                isLocalDraft = post.isLocalDraft,
                isLocallyChanged = post.isLocallyChanged,
                uploadUiState = uploadUiState,
                siteHasCapabilitiesToPublish = capabilitiesToPublish,
                statsSupported = statsSupported
        )
        val defaultActions = createDefaultViewActions(buttonTypes, onButtonClicked)
        val compactActions = createCompactViewActions(buttonTypes, onButtonClicked)

        val remotePostId = RemotePostId(RemoteId(post.remotePostId))
        val localPostId = LocalPostId(LocalId(post.id))
        val title = getTitle(post = post)
        val dateAndAuthor = getDateAndAuthorLabel(formattedDate, post.authorDisplayName, authorFilterSelection)
        val statuses = getStatuses(
                postStatus = postStatus,
                isLocalDraft = post.isLocalDraft,
                isLocallyChanged = post.isLocallyChanged,
                uploadUiState = uploadUiState,
                hasUnhandledConflicts = unhandledConflicts,
                hasAutoSave = hasAutoSave
        )
        val statusesColor = labelColorUseCase.getLabelsColor(post, uploadUiState, unhandledConflicts, hasAutoSave)
        val statusesDelimeter = UiStringRes(R.string.multiple_status_label_delimiter)
        val onSelected = {
            when (postStatus) {
                TRASHED -> {
                    onAction.invoke(
                            post,
                            BUTTON_SHOW_MOVE_TRASHED_POST_TO_DRAFT_DIALOG,
                            POST_LIST_ITEM_SELECTED
                    )
                }
                UNKNOWN, PUBLISHED, DRAFT, PRIVATE, PENDING, SCHEDULED -> onAction.invoke(
                        post,
                        BUTTON_EDIT,
                        POST_LIST_ITEM_SELECTED
                )
            }
        }
        val itemUiData = PostListItemUiStateData(
                remotePostId = remotePostId,
                localPostId = localPostId,
                title = title,
                excerpt = getExcerpt(post = post),
                imageUrl = featuredImageUrl,
                dateAndAuthor = dateAndAuthor,
                statuses = statuses,
                statusesColor = statusesColor,
                statusesDelimiter = statusesDelimeter,
                progressBarUiState = getProgressBarState(
                        uploadUiState = uploadUiState,
                        performingCriticalAction = performingCriticalAction
                ),
                showOverlay = shouldShowOverlay(
                        uploadUiState = uploadUiState,
                        performingCriticalAction = performingCriticalAction
                ),
                disableRippleEffect = postStatus == PostStatus.TRASHED
        )

        return PostListItemUiState(
                data = itemUiData,
                actions = defaultActions,
                compactActions = compactActions,
                onSelected = onSelected
        )
    }

    private fun getDateAndAuthorLabel(
        formattedDate: String,
        displayName: String?,
        authorFilterSelection: AuthorFilterSelection
    ): UiString {
        return when (authorFilterSelection) {
            EVERYONE -> {
                val joinedStrings = listOf(formattedDate, displayName).filterNot { TextUtils.isEmpty(it) }
                        .joinToString(separator = "  ·  ")
                UiStringText(joinedStrings)
            }
            ME -> UiStringText(formattedDate)
        }
    }

    private fun getTitle(post: PostModel): UiString {
        return if (post.title.isNotBlank()) {
            UiStringText(StringEscapeUtils.unescapeHtml4(post.title))
        } else UiStringRes(R.string.untitled_in_parentheses)
    }

    private fun getExcerpt(post: PostModel): UiString? =
            PostUtils.getPostListExcerptFromPost(post)
                    .takeIf { !it.isNullOrBlank() }
                    ?.let { StringEscapeUtils.unescapeHtml4(it) }
                    ?.let { PostUtils.collapseShortcodes(it) }
                    ?.let { UiStringText(it) }

    private fun getProgressBarState(
        uploadUiState: PostUploadUiState,
        performingCriticalAction: Boolean
    ): ProgressBarUiState {
        return if (shouldShowProgress(uploadUiState, performingCriticalAction)) {
            if (uploadUiState is UploadingMedia) {
                ProgressBarUiState.Determinate(uploadUiState.progress)
            } else {
                ProgressBarUiState.Indeterminate
            }
        } else {
            ProgressBarUiState.Hidden
        }
    }

    private fun shouldShowProgress(uploadUiState: PostUploadUiState, performingCriticalAction: Boolean): Boolean {
        return performingCriticalAction || uploadUiState is UploadingPost || uploadUiState is UploadingMedia ||
                uploadUiState is UploadQueued
    }

    private fun getStatuses(
        postStatus: PostStatus,
        isLocalDraft: Boolean,
        isLocallyChanged: Boolean,
        uploadUiState: PostUploadUiState,
        hasUnhandledConflicts: Boolean,
        hasAutoSave: Boolean
    ): List<UiString> {
        val labels: MutableList<UiString> = ArrayList()
        when {
            uploadUiState is PostUploadUiState.UploadFailed -> {
                getErrorLabel(uploadUiState, postStatus)?.let { labels.add(it) }
            }
            uploadUiState is UploadingPost -> if (uploadUiState.isDraft) {
                labels.add(UiStringRes(R.string.post_uploading_draft))
            } else {
                labels.add(UiStringRes(R.string.post_uploading))
            }
            uploadUiState is UploadingMedia -> labels.add(UiStringRes(R.string.uploading_media))
            uploadUiState is UploadQueued -> labels.add(UiStringRes(R.string.post_queued))
            uploadUiState is UploadWaitingForConnection -> {
                when (uploadUiState.postStatus) {
                    UNKNOWN, PUBLISHED -> labels.add(UiStringRes(R.string.post_waiting_for_connection_publish))
                    PRIVATE -> labels.add(UiStringRes(R.string.post_waiting_for_connection_private))
                    PENDING -> labels.add(UiStringRes(R.string.post_waiting_for_connection_pending))
                    SCHEDULED -> labels.add(UiStringRes(R.string.post_waiting_for_connection_scheduled))
                    DRAFT -> labels.add(UiStringRes(R.string.post_waiting_for_connection_draft))
                    TRASHED -> AppLog.e(
                            POSTS,
                            "Developer error: This state shouldn't happen. Trashed post is in " +
                                    "UploadWaitingForConnection state."
                    )
                }
            }
            hasUnhandledConflicts -> labels.add(UiStringRes(R.string.local_post_is_conflicted))
            hasAutoSave -> labels.add(UiStringRes(R.string.local_post_autosave_revision_available))
        }

        // we want to show either single error/progress label or 0-n info labels.
        if (labels.isEmpty()) {
            if (isLocalDraft) {
                labels.add(UiStringRes(R.string.local_draft))
            } else if (isLocallyChanged) {
                labels.add(UiStringRes(R.string.local_changes))
            }
            if (postStatus == PRIVATE) {
                labels.add(UiStringRes(R.string.post_status_post_private))
            }
            if (postStatus == PENDING) {
                labels.add(UiStringRes(R.string.post_status_pending_review))
            }
        }
        return labels
    }

    private fun getErrorLabel(uploadUiState: UploadFailed, postStatus: PostStatus): UiString? {
        return when {
            uploadUiState.error.mediaError != null -> getMediaUploadErrorMessage(uploadUiState, postStatus)
            uploadUiState.error.postError != null -> UploadUtils.getErrorMessageResIdFromPostError(
                    postStatus,
                    false,
                    uploadUiState.error.postError,
                    uploadUiState.isEligibleForAutoUpload
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

    private fun getMediaUploadErrorMessage(uploadUiState: UploadFailed, postStatus: PostStatus): UiStringRes {
        return when {
            uploadUiState.isEligibleForAutoUpload -> when (postStatus) {
                PUBLISHED -> UiStringRes(R.string.error_media_recover_post_not_published_retrying)
                PRIVATE -> UiStringRes(R.string.error_media_recover_post_not_published_retrying_private)
                SCHEDULED -> UiStringRes(R.string.error_media_recover_post_not_scheduled_retrying)
                PENDING -> UiStringRes(R.string.error_media_recover_post_not_submitted_retrying)
                DRAFT, TRASHED, UNKNOWN -> UiStringRes(R.string.error_generic_error_retrying)
            }
            uploadUiState.retryWillPushChanges -> when (postStatus) {
                PUBLISHED -> UiStringRes(R.string.error_media_recover_post_not_published)
                PRIVATE -> UiStringRes(R.string.error_media_recover_post_not_published_private)
                SCHEDULED -> UiStringRes(R.string.error_media_recover_post_not_scheduled)
                PENDING -> UiStringRes(R.string.error_media_recover_post_not_submitted)
                DRAFT, TRASHED, UNKNOWN -> UiStringRes(R.string.error_media_recover_post)
            }
            else -> UiStringRes(R.string.error_media_recover_post)
        }
    }

    private fun shouldShowOverlay(uploadUiState: PostUploadUiState, performingCriticalAction: Boolean): Boolean {
        // show overlay when post upload is in progress or (media upload is in progress and the user is not using Aztec)
        return performingCriticalAction ||
                (uploadUiState is UploadingPost ||
                        (!appPrefsWrapper.isAztecEditorEnabled && uploadUiState is UploadingMedia))
    }

    private fun createButtonTypes(
        postStatus: PostStatus,
        isLocalDraft: Boolean,
        isLocallyChanged: Boolean,
        uploadUiState: PostUploadUiState,
        siteHasCapabilitiesToPublish: Boolean,
        statsSupported: Boolean
    ): List<PostListButtonType> {
        val canRetryUpload = uploadUiState is PostUploadUiState.UploadFailed
        val canCancelPendingAutoUpload = (uploadUiState is UploadWaitingForConnection ||
                (uploadUiState is PostUploadUiState.UploadFailed && uploadUiState.isEligibleForAutoUpload))
        val canPublishPost = (canRetryUpload || uploadUiState is NothingToUpload || !canCancelPendingAutoUpload) &&
                (isLocallyChanged || isLocalDraft || postStatus == DRAFT ||
                        (siteHasCapabilitiesToPublish && postStatus == PENDING))

        val canShowStats = statsSupported &&
                postStatus == PUBLISHED &&
                !isLocalDraft &&
                !isLocallyChanged
        val canShowViewButton = !canRetryUpload && postStatus != PostStatus.TRASHED
        val canShowPublishButton = canRetryUpload || canPublishPost
        val buttonTypes = ArrayList<PostListButtonType>()

        if (postStatus != TRASHED) {
            buttonTypes.add(BUTTON_EDIT)
        }

        if (canCancelPendingAutoUpload) {
            buttonTypes.add(BUTTON_CANCEL_PENDING_AUTO_UPLOAD)
        }

        if (canShowPublishButton) {
            buttonTypes.add(
                    if (canRetryUpload) {
                        BUTTON_RETRY
                    } else if (!siteHasCapabilitiesToPublish) {
                        BUTTON_SUBMIT
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
            isLocalDraft -> buttonTypes.add(BUTTON_DELETE)
            postStatus == TRASHED -> {
                buttonTypes.add(BUTTON_MOVE_TO_DRAFT)
                buttonTypes.add(BUTTON_DELETE_PERMANENTLY)
            }
            postStatus != TRASHED -> buttonTypes.add(BUTTON_TRASH)
        }

        if (canShowStats) {
            buttonTypes.add(BUTTON_STATS)
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
                    kotlin.math.max(MAX_NUMBER_OF_VISIBLE_ACTIONS_STANDARD - 1, 0),
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
