package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.EditorThemeElementTable
import com.wellsql.generated.EditorThemeTable
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.EditorTheme
import org.wordpress.android.fluxc.model.EditorThemeElement
import org.wordpress.android.fluxc.model.EditorThemeSupport
import org.wordpress.android.fluxc.model.SiteModel

enum class EditorThemeElementType(val value: String) {
    COLOR("color"),
    GRADIENT("gradient");
}

class EditorThemeSqlUtils {
    fun replaceEditorThemeForSite(site: SiteModel, editorTheme: EditorTheme?) {
        deleteEditorThemeForSite(site)
        if (editorTheme == null) return
        makeEditorTheme(site, editorTheme)
    }

    fun getEditorThemeForSite(site: SiteModel): EditorTheme? {
        val editorTheme: EditorThemeBuilder? = WellSql.select(EditorThemeBuilder::class.java)
                .limit(1)
                .where()
                .equals(EditorThemeTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .asModel
                .firstOrNull()

        if (editorTheme == null) return null

        val colors = WellSql.select(EditorThemeElementBuilder::class.java)
                .where()
                .equals(EditorThemeElementTable.THEME_ID, editorTheme.id)
                .equals(EditorThemeElementTable.TYPE, EditorThemeElementType.COLOR.value)
                .endWhere()
                .asModel

        val gradients = WellSql.select(EditorThemeElementBuilder::class.java)
                .where()
                .equals(EditorThemeElementTable.THEME_ID, editorTheme.id)
                .equals(EditorThemeElementTable.TYPE, EditorThemeElementType.GRADIENT.value)
                .endWhere()
                .asModel

        return editorTheme.toEditorTheme(colors, gradients)
    }

    fun deleteEditorThemeForSite(site: SiteModel) {
        // Deleting the row for the EditorThemeTable table row here will cascade to delete the associated rows in the EditorThemeElementTable as well.
        WellSql.delete(EditorThemeBuilder::class.java)
                .where()
                .equals(EditorThemeTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .execute()
    }

    private fun makeEditorTheme(site: SiteModel, editorTheme: EditorTheme) {
        val editorThemeBuilder = editorTheme.toBuilder(site.id)
        val items = (editorTheme.themeSupport.colors ?: emptyList()) +
                (editorTheme.themeSupport.gradients ?: emptyList())

        WellSql.insert(editorThemeBuilder).execute()

        val elements = items.map {
            it.toBuilder(editorThemeBuilder.id)
        }

        WellSql.insert(elements).asSingleTransaction(true).execute()
    }

    @Table(name = "EditorTheme")
    data class EditorThemeBuilder(@PrimaryKey @Column private var mId: Int = -1) : Identifiable {
        @Column var localSiteId: Int = -1
            @JvmName("getLocalSiteId")
            get
            @JvmName("setLocalSiteId")
            set
        @Column var stylesheet: String? = null
        @Column var version: String? = null

        override fun setId(id: Int) {
            this.mId = id
        }

        override fun getId() = mId

        fun toEditorTheme(
            storedColors: List<EditorThemeElementBuilder>,
            storedGradients: List<EditorThemeElementBuilder>
        ): EditorTheme {
            val colors = if (storedColors.count() > 0) storedColors.mapNotNull { it.toEditorThemeElement() } else null
            val gradients = if (storedColors.count() > 0) storedGradients.mapNotNull { it.toEditorThemeElement() } else null
            val editorThemeSupport = EditorThemeSupport(colors, gradients)

            return EditorTheme(editorThemeSupport, stylesheet, version)
        }
    }

    @Table(name = "EditorThemeElement")
    data class EditorThemeElementBuilder(@PrimaryKey @Column private var mId: Int = -1) : Identifiable {
        @Column var themeId: Int = -1
        @Column var type: String = EditorThemeElementType.COLOR.value
        @Column var name: String? = null
        @Column var slug: String? = null
        @Column var value: String? = null

        override fun setId(id: Int) {
            this.mId = id
        }

        override fun getId() = mId

        fun toEditorThemeElement(): EditorThemeElement? {
            when (type) {
                EditorThemeElementType.COLOR.value -> return EditorThemeElement(name, slug, value, null)
                EditorThemeElementType.GRADIENT.value -> return EditorThemeElement(name, slug, null, value)
                else -> {
                    // This shouldn't really happen as the "type" is defined in this library and isn't really driven
                    // off of the network call. However adding it for completeness.
                    return null
                }
            }
        }
    }
}
