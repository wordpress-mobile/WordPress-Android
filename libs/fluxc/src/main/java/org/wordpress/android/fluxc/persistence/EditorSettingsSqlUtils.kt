package org.wordpress.android.fluxc.persistence

import com.google.gson.JsonParser
import com.wellsql.generated.EditorSettingsTable
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.EditorSettings
import org.wordpress.android.fluxc.model.SiteModel

class EditorSettingsSqlUtils {
    fun replaceEditorSettingsForSite(site: SiteModel, editorSettings: EditorSettings?) {
        deleteEditorSettingsForSite(site)
        if (editorSettings == null) return
        makeEditorSettings(site, editorSettings)
    }

    fun getEditorSettingsForSite(site: SiteModel): EditorSettings? {
        return WellSql.select(EditorSettingsBuilder::class.java)
            .limit(1)
            .where()
            .equals(EditorSettingsTable.LOCAL_SITE_ID, site.id)
            .endWhere()
            .asModel
            .firstOrNull()
            ?.toEditorSettings()
    }

    fun deleteEditorSettingsForSite(site: SiteModel) {
        WellSql.delete(EditorSettingsBuilder::class.java)
            .where()
            .equals(EditorSettingsTable.LOCAL_SITE_ID, site.id)
            .endWhere()
            .execute()
    }

    private fun makeEditorSettings(site: SiteModel, editorSettings: EditorSettings) {
        val builder = editorSettings.toBuilder(site)
        WellSql.insert(builder).execute()
    }

    @Table(name = "EditorSettings")
    data class EditorSettingsBuilder(@PrimaryKey @Column private var mId: Int = -1) : Identifiable {
        @Column var localSiteId: Int = -1
            @JvmName("getLocalSiteId")
            get
            @JvmName("setLocalSiteId")
            set
        @Column var rawSettings: String? = null

        override fun setId(id: Int) {
            this.mId = id
        }

        override fun getId() = mId

        fun toEditorSettings(): EditorSettings? {
            return rawSettings?.let {
                val jsonObject = JsonParser.parseString(it).asJsonObject
                EditorSettings(jsonObject)
            }
        }
    }
}
