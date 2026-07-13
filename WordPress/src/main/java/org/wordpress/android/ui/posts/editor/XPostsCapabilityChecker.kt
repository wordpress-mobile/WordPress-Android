package org.wordpress.android.ui.posts.editor

import androidx.core.util.Consumer
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.XPostsResult
import org.wordpress.android.fluxc.store.XPostsStore
import javax.inject.Inject

class XPostsCapabilityChecker @Inject constructor(
    private val xPostsStore: XPostsStore
) {
    @Suppress("GlobalCoroutineUsage")
    @OptIn(DelicateCoroutinesApi::class)
    fun retrieveCapability(site: SiteModel, callback: Consumer<Boolean>) {
        GlobalScope.launch(Dispatchers.IO) {
            val capability = isCapable(site)
            callback.accept(capability)
        }
    }

    private suspend fun fetchingReturnsXposts(site: SiteModel): Boolean =
        when (val fetched = xPostsStore.fetchXPosts(site)) {
            is XPostsResult.Result -> fetched.xPosts.isNotEmpty()
            is XPostsResult.Unknown ->
                // We don't know whether the site has any xposts, so default to true
                true
        }

    suspend fun isCapable(site: SiteModel): Boolean =
        // Check db first in order to reduce unnecessary network calls
        when (val saved = xPostsStore.getXPostsFromDb(site)) {
            is XPostsResult.Result ->
                // A persisted result is definitive: a non-empty list means the site has xposts, and an
                // empty list is the stored "no xposts" marker from a previous backend response. Trust the
                // cache either way rather than re-fetching on every editor open. Re-fetching on the empty
                // case fired a request each time the editor opened on sites without xposts, which for
                // non-P2 sites is rejected with a recurring `xposts_require_o2_enabled` (HTTP 400). We
                // accept that a site which gains or loses all its xposts may be briefly stale, matching
                // the staleness we already tolerate for the non-empty case.
                saved.xPosts.isNotEmpty()
            is XPostsResult.Unknown ->
                // We have never gotten a response for this site, so make the api call to try to find out
                fetchingReturnsXposts(site)
        }
}
