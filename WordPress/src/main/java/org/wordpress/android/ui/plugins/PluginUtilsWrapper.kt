package org.wordpress.android.ui.plugins

import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

class PluginUtilsWrapper
@Inject constructor() {
    fun isPluginFeatureAvailable(site: SiteModel): Boolean {
        return PluginUtils.isPluginFeatureAvailable(site)
    }
}
