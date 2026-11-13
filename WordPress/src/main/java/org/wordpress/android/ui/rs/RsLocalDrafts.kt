package org.wordpress.android.ui.rs

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.posts.PostModelUploadStatusTracker
import org.wordpress.android.ui.posts.PostUtils
import org.wordpress.android.ui.uploads.UploadServiceFacade
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState
import java.util.Date
import javax.inject.Inject
import javax.inject.Provider

/** A post that exists only on this device, as the rs list's local drafts sheet renders it. */
data class RsLocalDraftUiModel(
    val localId: Int,
    /** Blank when the draft has no title; the UI shows its own placeholder. */
    val title: String,
    val dateLabel: String,
    val isUploadFailed: Boolean,
)

/**
 * Reads, opens and retries the posts that never reached the server.
 *
 * The rs list renders the server's posts, which these can't be among, and wordpress-rs has no local
 * write path, so they're read from FluxC.
 */
class RsLocalDrafts @Inject constructor(
    private val appContext: Context,
    private val postStore: PostStore,
    private val uploadStatusTrackerProvider: Provider<PostModelUploadStatusTracker>,
    private val uploadUiStateUseCase: PostModelUploadUiStateUseCase,
    private val uploadServiceFacade: UploadServiceFacade,
) {
    /**
     * Local drafts for [site], most recently changed first. [dateFormat] wraps the date, e.g.
     * "Modified: %s".
     */
    suspend fun load(site: SiteModel, nowLabel: String, dateFormat: String?): List<RsLocalDraftUiModel> =
        withContext(Dispatchers.IO) {
            // The tracker caches per post in a plain HashMap and loads can overlap, so each load gets
            // its own: nothing is shared, and nothing cached by an earlier load can be stale.
            val tracker = uploadStatusTrackerProvider.get()
            postStore.getLocalDraftPosts(site)
                // A failed upload stamps the change date too, so a batch that fails together ties; newest
                // created (highest local id) first keeps those in a sensible order.
                .sortedWith(
                    compareByDescending<PostModel> { it.dateLocallyChangedMillis() }.thenByDescending { it.id }
                )
                .map { it.toUiModel(site, tracker, nowLabel, dateFormat) }
        }

    /** The draft to hand the editor, or null when it was uploaded or removed since the list loaded. */
    fun open(site: SiteModel, localId: Int): PostModel? = find(site, localId)

    /**
     * Whether the draft is being sent to the server, or is queued to be. PostUploadHandler pushes its
     * queued copy even after a cancel, so editing it then would race the push.
     */
    fun isUploadingOrQueued(site: SiteModel, localId: Int): Boolean =
        find(site, localId)?.let { uploadServiceFacade.isPostUploadingOrQueued(it) } ?: false

    /** Starts the upload again; the change listener reports the result. */
    fun retry(site: SiteModel, localId: Int) {
        val post = find(site, localId) ?: return
        // Only a first publish is tracked as one, as the legacy list's retry does.
        val trackAnalytics = PostUtils.isFirstTimePublish(post)
        uploadServiceFacade.uploadPost(appContext, post, trackAnalytics, sourceForLogging = SOURCE_RETRY)
    }

    private fun find(site: SiteModel, localId: Int): PostModel? =
        postStore.getPostByLocalPostId(localId)?.takeIf { it.isLocalDraft && it.localSiteId == site.id }

    private fun PostModel.dateLocallyChangedMillis(): Long =
        dateLocallyChanged.takeIf { it.isNotBlank() }
            ?.let { DateTimeUtils.timestampFromIso8601Millis(it) }
            ?: 0L

    private fun PostModel.toUiModel(
        site: SiteModel,
        tracker: PostModelUploadStatusTracker,
        nowLabel: String,
        dateFormat: String?
    ): RsLocalDraftUiModel {
        val uploadUiState = uploadUiStateUseCase.createUploadUiState(this, site, tracker)
        val changedMillis = dateLocallyChangedMillis()
        val date = if (changedMillis > 0L) {
            RsDateFormatter.format(Date(changedMillis), nowLabel).let { dateFormat?.format(it) ?: it }
        } else {
            ""
        }
        return RsLocalDraftUiModel(
            localId = id,
            title = title.toDisplayTitle(),
            dateLabel = date,
            isUploadFailed = uploadUiState is PostUploadUiState.UploadFailed,
        )
    }

    /**
     * Unescaped, since the editor stores "<" as "&lt;", but not stripped: a title can't hold markup, and
     * stripping would eat text that only looks like a tag, such as "x < 5 and y > 3".
     */
    private fun String.toDisplayTitle(): String =
        StringEscapeUtils.unescapeHtml4(this).replace(WHITESPACE, " ").trim()

    private companion object {
        const val SOURCE_RETRY = "rs list retry"
        val WHITESPACE = Regex("\\s+")
    }
}
