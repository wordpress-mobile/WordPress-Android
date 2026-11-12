package org.wordpress.android.ui.rs

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.PageStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.notifications.utils.PendingDraftsNotificationsUtilsWrapper
import org.wordpress.android.ui.posts.PostModelUploadStatusTracker
import org.wordpress.android.ui.posts.PostUtils
import org.wordpress.android.ui.uploads.UploadServiceFacade
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.HtmlUtils
import org.wordpress.android.viewmodel.pages.CreatePageListItemLabelsUseCase
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState
import org.wordpress.android.viewmodel.posts.CreatePostListItemLabelsUseCase
import java.util.Date
import javax.inject.Inject

/** A post or page that exists only on this device, as the rs lists render it. */
data class RsLocalDraftUiModel(
    val localId: Int,
    val title: String,
    val excerpt: String,
    val dateLabel: String,
    /** The upload error or progress, plus any status qualifiers such as "Pending review". */
    val statusLabels: List<UiString>,
    val isUploadFailed: Boolean,
) {
    /** Retry is offered only after a failure, as in the legacy list; otherwise an upload is due or under way. */
    val canRetry: Boolean get() = isUploadFailed
}

/**
 * Reads, labels, retries and deletes the posts or pages that never reached the server.
 *
 * The rs lists render the server's posts, which these can't be among, so without this they had no row
 * at all. Labels and upload state come from the same use cases as the legacy lists, so a failure reads
 * the same in both.
 */
class RsLocalDrafts @Inject constructor(
    private val appContext: Context,
    private val postStore: PostStore,
    private val pageStore: PageStore,
    private val dispatcher: Dispatcher,
    private val uploadStatusTracker: PostModelUploadStatusTracker,
    private val uploadUiStateUseCase: PostModelUploadUiStateUseCase,
    private val postLabels: CreatePostListItemLabelsUseCase,
    private val pageLabels: CreatePageListItemLabelsUseCase,
    private val uploadServiceFacade: UploadServiceFacade,
    private val pendingDraftsNotifications: PendingDraftsNotificationsUtilsWrapper,
) {
    /**
     * Local drafts for [site], most recently changed first, keeping only those whose title or content
     * contains [query] when it isn't blank. [dateFormat] wraps the date, e.g. "Modified: %s".
     */
    suspend fun load(
        site: SiteModel,
        isPage: Boolean,
        query: String,
        nowLabel: String,
        dateFormat: String?
    ): List<RsLocalDraftUiModel> = withContext(Dispatchers.IO) {
        val drafts = if (isPage) pageStore.getLocalDraftPages(site) else postStore.getLocalDraftPosts(site)
        // The tracker caches per post, and every load follows a change, so nothing cached is current.
        uploadStatusTracker.invalidateUploadStatus(drafts.map { it.id })
        drafts.filter { it.matches(query) }
            .sortedByDescending { it.dateLocallyChangedMillis() }
            .map { it.toUiModel(site, isPage, nowLabel, dateFormat) }
    }

    /** Starts the upload again; the change listener reports its progress. */
    fun retry(site: SiteModel, localId: Int) {
        val post = find(site, localId) ?: return
        uploadServiceFacade.uploadPost(appContext, post, trackAnalytics = true, sourceForLogging = SOURCE_RETRY)
    }

    /**
     * Removes the draft from the device. Nothing exists on the server to delete. Its reminders and any
     * upload-failure notification go with it, since both would otherwise lead to a draft that's gone.
     */
    fun delete(site: SiteModel, localId: Int) {
        val post = find(site, localId) ?: return
        pendingDraftsNotifications.cancelPendingDraftAlarms(appContext, post.id)
        pendingDraftsNotifications.dismissPendingDraftNotification(appContext, post.id)
        uploadServiceFacade.cancelFinalNotification(post)
        dispatcher.dispatch(PostActionBuilder.newRemovePostAction(post))
    }

    /**
     * The draft to hand the editor, or null when it was uploaded or deleted since the list loaded. A
     * queued upload is cancelled first, as the legacy list does, so it can't race the edit; media
     * already uploading carries on.
     */
    fun open(site: SiteModel, localId: Int): PostModel? {
        val post = find(site, localId) ?: return null
        if (uploadServiceFacade.isPostUploadingOrQueued(post)) uploadServiceFacade.cancelQueuedPostUpload(post)
        return post
    }

    /**
     * Whether the draft is being sent to the server right now. Cancelling only stops a queued upload, so
     * editing or deleting it then would race the push - as the legacy lists' overlay prevents.
     */
    fun isUploading(site: SiteModel, localId: Int): Boolean =
        find(site, localId)?.let { uploadServiceFacade.isPostUploading(it) } == true

    private fun find(site: SiteModel, localId: Int): PostModel? =
        postStore.getPostByLocalPostId(localId)?.takeIf { it.isLocalDraft && it.localSiteId == site.id }

    private fun PostModel.matches(query: String): Boolean = query.isBlank() ||
        title.contains(query, ignoreCase = true) ||
        content.contains(query, ignoreCase = true)

    private fun PostModel.dateLocallyChangedMillis(): Long =
        dateLocallyChanged.takeIf { it.isNotBlank() }
            ?.let { DateTimeUtils.timestampFromIso8601Millis(it) }
            ?: 0L

    private fun PostModel.toUiModel(
        site: SiteModel,
        isPage: Boolean,
        nowLabel: String,
        dateFormat: String?
    ): RsLocalDraftUiModel {
        val uploadUiState = uploadUiStateUseCase.createUploadUiState(this, site, uploadStatusTracker)
        val labels = if (isPage) {
            pageLabels.createLabels(this, uploadUiState).first
        } else {
            postLabels.getStatuses(
                postStatus = PostStatus.fromPost(this),
                post = this,
                uploadUiState = uploadUiState,
                hasUnhandledConflicts = false,
                hasAutoSave = false
            )
        }
        val changedMillis = dateLocallyChangedMillis()
        val date = if (changedMillis > 0L) {
            RsDateFormatter.format(Date(changedMillis), nowLabel).let { dateFormat?.format(it) ?: it }
        } else {
            ""
        }
        return RsLocalDraftUiModel(
            localId = id,
            title = title.toPlainText(),
            excerpt = listExcerpt(),
            dateLabel = date,
            // The row's badge already says it's local, so the plain "Local draft" label would repeat it.
            statusLabels = labels.filterNot { it in LOCAL_DRAFT_LABELS },
            isUploadFailed = uploadUiState is PostUploadUiState.UploadFailed,
        )
    }

    /**
     * Built as the legacy list builds it, so gallery and VideoPress blocks are dropped and escaped text
     * such as "&lt;div&gt;" survives stripping. Collapsed to one paragraph, as the row shows two lines.
     */
    private fun PostModel.listExcerpt(): String =
        PostUtils.getPostListExcerptFromPost(this)
            ?.let { StringEscapeUtils.unescapeHtml4(it) }
            ?.let { PostUtils.collapseShortcodes(it) }
            ?.split(LARGE_SPACE_GAP)?.first()
            ?.replace(WHITESPACE, " ")?.trim()
            .orEmpty()

    private fun String.toPlainText(): String =
        HtmlUtils.fastStripHtml(StringEscapeUtils.unescapeHtml4(this)).replace(WHITESPACE, " ").trim()

    private companion object {
        const val SOURCE_RETRY = "rs list retry"
        val WHITESPACE = Regex("\\s+")
        val LARGE_SPACE_GAP = Regex("\\s+\\n+")
        val LOCAL_DRAFT_LABELS = setOf(UiStringRes(R.string.local_draft), UiStringRes(R.string.page_local_draft))
    }
}
