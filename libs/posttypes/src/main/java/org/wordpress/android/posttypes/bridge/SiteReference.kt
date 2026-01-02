package org.wordpress.android.posttypes.bridge

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Minimal site reference for the Post Types module.
 *
 * ## Purpose
 * Provides a lightweight, module-internal representation of a site that doesn't depend on
 * FluxC's SiteModel. This enforces isolation and prepares for wordpress-rs integration.
 *
 * ## Migration Notes
 * When integrating wordpress-rs:
 * - This class should be replaced with the wordpress-rs site model
 * - Or converted to a domain model that maps from wordpress-rs types
 *
 * When merging back into the main app (if not using wordpress-rs):
 * - Replace with FluxC SiteModel
 * - Update [fromParcelable] callers to pass SiteModel directly
 *
 * @see org.wordpress.android.posttypes.bridge package documentation
 */
@Parcelize
data class SiteReference(
    val id: Int,
    val name: String,
    val url: String,
    val userId: Long
) : Parcelable {
    companion object {
        /**
         * Creates a [SiteReference] from a Parcelable site object.
         *
         * This is the bridge point where the main app's SiteModel is converted to our
         * module-internal representation. The main app module should call this when
         * launching Post Types activities.
         *
         * @param localSiteId The site's local database ID
         * @param siteName The site's display name
         * @param siteUrl The site's URL
         * @param userId The current user's ID for author filtering
         */
        fun create(
            localSiteId: Int,
            siteName: String,
            siteUrl: String,
            userId: Long
        ): SiteReference {
            return SiteReference(
                id = localSiteId,
                name = siteName,
                url = siteUrl,
                userId = userId
            )
        }
    }
}
