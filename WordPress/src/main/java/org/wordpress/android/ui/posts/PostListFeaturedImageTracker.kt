package org.wordpress.android.ui.posts

import android.annotation.SuppressLint
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload

/**
 * This is a temporary class to make the PostListViewModel more manageable. Please feel free to refactor it any way
 * you see fit.
 */
class PostListFeaturedImageTracker(private val dispatcher: Dispatcher, private val mediaStore: MediaStore) {
    /*
    Using `SparseArray` is results in ArrayIndexOutOfBoundsException when we are trying to put a new item. Although
    the reason for the crash is unclear, this defeats the whole purpose of using a `SparseArray`. Furthermore,
    `SparseArray` is actually not objectively better than using a `HashMap` and in this case `HashMap` should perform
    better due to higher number of items.

    https://github.com/wordpress-mobile/WordPress-Android/issues/11487
     */
    @SuppressLint("UseSparseArrays")
    private val featuredImageMap = HashMap<Long, String>()

    fun getFeaturedImageUrl(site: SiteModel, featuredImageId: Long): String? {
        if (featuredImageId == 0L) {
            return null
        }
        featuredImageMap[featuredImageId]?.let { return it }
        mediaStore.getSiteMediaWithId(site, featuredImageId)?.let { media ->
            // This should be a pretty rare case, but some media seems to be missing url
            return if (media.url != null) {
                featuredImageMap[featuredImageId] = media.url
                media.url
            } else null
        }
        // Media is not in the Store, we need to download it
        val mediaToDownload = MediaModel()
        mediaToDownload.mediaId = featuredImageId
        mediaToDownload.localSiteId = site.id
        val payload = MediaPayload(site, mediaToDownload)
        dispatcher.dispatch(MediaActionBuilder.newFetchMediaAction(payload))
        return null
    }

    fun invalidateFeaturedMedia(featuredImageIds: List<Long>) {
        featuredImageIds.forEach { featuredImageMap.remove(it) }
    }
}
