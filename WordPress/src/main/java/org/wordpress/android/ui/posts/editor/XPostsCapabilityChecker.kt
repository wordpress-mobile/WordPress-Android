package org.wordpress.android.ui.posts.editor

import androidx.core.util.Consumer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.XPostsStore
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject

class XPostsCapabilityChecker @Inject constructor(
    private val xPostsStore: XPostsStore,
    private val networkUtilsWrapper: NetworkUtilsWrapper
) {
    fun retrieveCapability(site: SiteModel, callback: Consumer<Boolean>) {
        GlobalScope.launch(Dispatchers.IO) {
            val capability = isCapable(site)
            callback.accept(capability)
        }
    }

    private suspend fun isCapable(site: SiteModel): Boolean {
        // Check db first in order to reduce unnecessary network calls
        val saved = xPostsStore.getXPostsFromDb(site)
        return if (saved.isNotEmpty()) {
            // If we have xposts saved in the db, then set capability to true even though
            // it is possible that the site no longer has any xpost suggestions. We're doing
            // this because it is a very edge case for a site to have removed all its xposts
            // suggestions since the last time we checked, and by not making the call here
            // we significantly reduce the number of expensive calls being made.
            true
        } else {
            // Db was empty, so let's check the endpoint to make sure there aren't any new xpost suggestions.
            // This call will be made every time the editor opens on sites that don't have any xpost
            // suggestions, but because the response will always be empty, it's not an expensive call.
            val hasXposts = xPostsStore
                    .fetchXPosts(site)
                    .xPosts
                    .isNotEmpty()
            hasXposts || !networkUtilsWrapper.isNetworkAvailable()
        }
    }
}
