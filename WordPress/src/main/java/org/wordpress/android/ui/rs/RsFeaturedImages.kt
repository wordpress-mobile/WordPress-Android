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
 * Featured image URLs for the rows of an rs list, sized for both row shapes. The caller applies the
 * result, because pages wrap their models in tree items and posts don't.
 */
internal class RsFeaturedImages<TAB>(
    private val scope: CoroutineScope,
    private val restClient: RsSiteRestClient,
    /** Called on [scope] with whatever resolved, for the caller to map through [withImage]. */
    private val onImagesResolved: (TAB, Map<Long, FeaturedImageUrls>) -> Unit,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val jobs = mutableMapOf<TAB, Job>()

    /** Media ids the lookup couldn't resolve, so their rows stop waiting. Cleared by a refresh. */
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
            // An id that failed once and later resolved is no longer written off.
            unresolvable.removeAll(images.keys)
            unresolvable.addAll(unresolvedIds.filterNot(images::containsKey))
            onImagesResolved(tab, images)
        }
    }

    /** Keeps [existing]'s resolved image on a reloaded [model], so the row doesn't re-resolve. */
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
