package org.wordpress.android.ui.rs

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.config.WpComWpRsFeatureConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decides whether the wordpress-rs screens (posts, pages, comments) are used for a site instead of
 * the legacy (FluxC) ones. Keep this as the single definition so those screens can't drift apart
 * for the same site.
 *
 * Note this does not govern every route into those screens: the drafts/scheduled tabs, upload
 * notifications and blogging reminders go straight to the legacy screens regardless of this
 * decision.
 */
@Singleton
class WpRsRouting @Inject constructor(
    private val wpComWpRsFeatureConfig: WpComWpRsFeatureConfig
) {
    /**
     * An application password is enough on its own. Sites reached over the WP.com REST API don't
     * need one — wordpress-rs authenticates them with an OAuth bearer token — but they're behind a
     * rollout flag because that moves the whole WP.com audience off the mature legacy screens.
     */
    fun canUseWpRs(site: SiteModel?): Boolean = site != null &&
            (site.hasApplicationPassword() ||
                    (site.isUsingWpComRestApi && wpComWpRsFeatureConfig.isEnabled()))
}
