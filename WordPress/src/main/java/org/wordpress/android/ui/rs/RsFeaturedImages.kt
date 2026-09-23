package org.wordpress.android.ui.rs

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.rs.contentlist.ContentItemUiModel
import org.wordpress.android.ui.rs.contentlist.HERO_IMAGE_HEIGHT_DP
import org.wordpress.android.ui.rs.contentlist.RsMenuAction
import org.wordpress.android.ui.rs.contentlist.THUMBNAIL_SIZE_DP
import org.wordpress.android.ui.rs.data.FeaturedImageUrls
import org.wordpress.android.ui.rs.data.RsSiteRestClient

/**
 * Featured image URLs for the rows of an rs list, sized for both row shapes.
 *
 * Owns the per-tab lookup jobs and the ids the lookup could not resolve, so a row stops showing a
 * placeholder once there is nothing to wait for. Pushing the result onto the rows is the caller's,
 * because the pages list wraps its models in tree items and the posts list does not.
 */
internal class RsFeaturedImages<TAB>(
    private val scope: CoroutineScope,
    private val restClient: RsSiteRestClient,
    /** Called on [scope] with whatever resolved, for the caller to map through [withImage]. */
    private val onImagesResolved: (TAB, Map<Long, FeaturedImageUrls>) -> Unit,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val jobs = mutableMapOf<TAB, Job>()

    /**
     * Featured media ids whose lookup came back without a URL. Rows use this to stop waiting: the
     * fetch is not retried on its own, so without it they shimmer indefinitely. Cleared by a
     * refresh, which is what gives a failed lookup another go.
     */
    private val unresolvable = mutableSetOf<Long>()

    /** Looks up the images [items] still lack, replacing any lookup already running for [tab]. */
    fun resolve(tab: TAB, site: SiteModel, items: List<ContentItemUiModel<*>>) {
        val unresolvedIds = items
            .filter { it.featuredImageId != 0L && it.featuredImage == null }
            .map { it.featuredImageId }
            .distinct()
        if (unresolvedIds.isEmpty()) return

        jobs[tab]?.cancel()
        jobs[tab] = scope.launch {
            val images = withContext(ioDispatcher) {
                restClient.fetchFeaturedImageUrls(
                    site, unresolvedIds, THUMBNAIL_SIZE_DP, HERO_IMAGE_HEIGHT_DP
                )
            }
            // Ids the lookup could not resolve stop their row waiting; ones that did resolve are
            // no longer reported as unresolvable, so an id that failed once and later came back
            // is not still written off.
            unresolvable.removeAll(images.keys)
            unresolvable.addAll(unresolvedIds.filterNot(images::containsKey))
            onImagesResolved(tab, images)
        }
    }

    /**
     * Carries an image already resolved for [existing] onto a freshly loaded [model], so the row
     * doesn't visibly re-resolve every time its collection reports a change.
     */
    fun <A : RsMenuAction> carryOver(
        model: ContentItemUiModel<A>,
        existing: ContentItemUiModel<*>?
    ): ContentItemUiModel<A> = model.copy(
        featuredImage = existing?.featuredImage?.takeIf {
            model.featuredImageId != 0L && model.featuredImageId == existing.featuredImageId
        },
        isFeaturedImageUnresolvable = model.featuredImageId in unresolvable
    )

    /** [model] with its image from [images], or [model] itself when there is nothing to change. */
    fun <A : RsMenuAction> withImage(
        model: ContentItemUiModel<A>,
        images: Map<Long, FeaturedImageUrls>
    ): ContentItemUiModel<A> {
        val image = images[model.featuredImageId]
        return when {
            image != null -> model.copy(featuredImage = image, isFeaturedImageUnresolvable = false)
            model.featuredImageId in unresolvable -> model.copy(isFeaturedImageUnresolvable = true)
            else -> model
        }
    }

    /** Forgets the failures, so a refresh asks for those images again. */
    fun invalidateUnresolved() {
        unresolvable.clear()
    }

    fun clear() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        unresolvable.clear()
    }
}
