package org.wordpress.android.fluxc.model

import com.google.gson.JsonObject
import org.wordpress.android.fluxc.persistence.EditorSettingsSqlUtils.EditorSettingsBuilder

class EditorSettings(val rawSettings: JsonObject) {
    fun toJsonString(): String {
        return rawSettings.toString()
    }

    fun toBuilder(site: SiteModel): EditorSettingsBuilder {
        return EditorSettingsBuilder().apply {
            localSiteId = site.id
            rawSettings = this@EditorSettings.rawSettings.toString()
        }
    }
}
