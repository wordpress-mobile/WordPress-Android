package org.wordpress.android.datasets

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.SiteSettingsModel

/**
 * Provides site-level settings for a given [SiteModel].
 */
interface SiteSettingsProvider {
    fun getSettings(site: SiteModel): SiteSettingsModel?
    fun isBlockEditorDefault(site: SiteModel): Boolean
}
