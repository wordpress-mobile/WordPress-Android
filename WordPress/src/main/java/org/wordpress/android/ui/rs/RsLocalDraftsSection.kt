package org.wordpress.android.ui.rs

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ResourceProvider

/**
 * The "Not uploaded" section of an rs list: the drafts of [site] that exist only on this device.
 *
 * Built by the list's view model, like [RsAuthorNames] and [RsFeaturedImages], so the posts and pages
 * lists share loading, filtering and upload triggers. The view model keeps what differs between them:
 * analytics, editor events and confirmation dialogs.
 */
internal class RsLocalDraftsSection(
    private val scope: CoroutineScope,
    private val source: RsLocalDrafts,
    private val resourceProvider: ResourceProvider,
    private val site: SiteModel,
    private val isPage: Boolean,
    /** The search query to filter by; blank outside search. Read on every [reload]. */
    private val query: () -> String,
) {
    private val _drafts = MutableStateFlow<List<RsLocalDraftUiModel>>(emptyList())
    val drafts: StateFlow<List<RsLocalDraftUiModel>> = _drafts.asStateFlow()

    private var loadJob: Job? = null

    /**
     * Lists the drafts and keeps them current with [localChanges]. Uploads still due are left to
     * [org.wordpress.android.ui.uploads.UploadStarter], which queues them on foreground and reconnect.
     */
    fun start(localChanges: Flow<Unit>) {
        scope.launch { localChanges.collect { reload() } }
        reload()
    }

    fun reload() {
        val previous = loadJob
        previous?.cancel()
        // Read now rather than in the coroutine, so a load reflects the query it was asked for.
        val currentQuery = query()
        loadJob = scope.launch {
            // Cancelling doesn't stop a load mid-query, and loads share the upload status tracker's
            // unsynchronized cache, so the next one waits for it to finish.
            previous?.join()
            @Suppress("TooGenericExceptionCaught")
            try {
                _drafts.value = source.load(
                    site = site,
                    isPage = isPage,
                    query = currentQuery,
                    nowLabel = resourceProvider.getString(R.string.rs_date_now),
                    dateFormat = resourceProvider.getString(R.string.rs_date_modified)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(AppLog.T.POSTS, "Failed to load local drafts", e)
            }
        }
    }

    /**
     * The draft to hand the editor. It needs no network, since the editor reads it from FluxC. Null when
     * it was uploaded or deleted since it was listed, in which case the list reloads to move or drop it.
     */
    fun open(localId: Int): PostModel? = source.open(site, localId) ?: run {
        reload()
        null
    }

    fun retry(localId: Int) {
        source.retry(site, localId)
        reload()
    }

    fun delete(localId: Int) = source.delete(site, localId)

    /**
     * True while the draft is being sent to the server; see [RsLocalDrafts.isUploading]. Reloads in that
     * case, since an upload starting isn't reported and the row may not show it yet.
     */
    fun isUploading(localId: Int): Boolean = source.isUploading(site, localId).also { if (it) reload() }
}
