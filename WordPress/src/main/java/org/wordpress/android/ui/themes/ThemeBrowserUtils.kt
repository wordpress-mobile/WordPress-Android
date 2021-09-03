package org.wordpress.android.ui.themes

import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

class ThemeBrowserUtils
@Inject constructor() {
    fun isAccessible(site: SiteModel?): Boolean {
        // themes are only accessible to admin wordpress.com users
        return site != null &&
                site.isUsingWpComRestApi &&
                site.hasCapabilityEditThemeOptions &&
                !site.isWpForTeamsSite
    }
}
