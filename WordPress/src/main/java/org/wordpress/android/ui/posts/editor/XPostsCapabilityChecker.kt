package org.wordpress.android.ui.posts.editor

import android.util.Log
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
        val saved = xPostsStore.getXPostsFromDb(site)
        return if (saved.isNotEmpty()) {
            Log.e("TEST123", "XPostsCapability saved is not empty so returning true")
            true
        } else {
            val result = xPostsStore
                    .fetchXPosts(site)
                    .xPosts
                    .isNotEmpty()
            Log.e("TEST123", "XPostsCapability fetching results and returning $result")
            result
        }
    }
}
