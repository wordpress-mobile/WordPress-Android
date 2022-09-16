package org.wordpress.android.ui.posts.editor

import androidx.core.util.Consumer
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
                    if (saved.xPosts.isEmpty()) {
                        // Db was empty, so let's make the api call and ensure there aren't any new xpost suggestions.
                        // This call will be made every time the editor opens on sites that don't have any xpost
                        // suggestions, but because the response will almost always be empty, it's not an expensive
                        // call.
                        fetchingReturnsXposts(site)
                    } else {
                        // We have xposts saved in the db, so set capability to true even though
                        // it is possible that the site no longer has any xpost suggestions. We're doing
                        // this because it is an edge case for a site to have removed all its xpost
                        // suggestions since the last time we checked, and by not making the call here
                        // we avoid making an expensive call that probably will return many xpost suggestions
                        // every time the editor is opened.
                        true
                    }
                is XPostsResult.Unknown ->
                    // We don't know whether the site has any xposts, so make the api call to try to find out
                    fetchingReturnsXposts(site)
            }
}
