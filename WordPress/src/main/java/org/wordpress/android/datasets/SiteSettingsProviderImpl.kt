package org.wordpress.android.datasets

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.SiteSettingsModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SiteSettingsProviderImpl @Inject constructor() :
    SiteSettingsProvider {
    override fun getSettings(site: SiteModel): SiteSettingsModel? {
        val cursor = SiteSettingsTable.getSettings(site.id.toLong())
            ?: return null
        return cursor.use {
            if (it.moveToFirst()) {
                SiteSettingsModel().also { model ->
                    model.deserializeOptionsDatabaseCursor(it, null)
                }
            } else {
                null
            }
        }
    }

    override fun isBlockEditorDefault(site: SiteModel): Boolean {
        val editor = site.mobileEditor
        if (editor.isNullOrEmpty()) return true
        return site.isWPComSimpleSite || editor == GUTENBERG_EDITOR_NAME
    }

    private companion object {
        const val GUTENBERG_EDITOR_NAME = "gutenberg"
    }
}
