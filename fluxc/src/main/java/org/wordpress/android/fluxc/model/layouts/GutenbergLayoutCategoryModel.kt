package org.wordpress.android.fluxc.model.layouts

import com.wellsql.generated.GutenbergLayoutCategoryModelTable
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.site.GutenbergLayoutCategory

@Table
class GutenbergLayoutCategoryModel(
    @PrimaryKey @Column private var id: Int = 0,
    @Column var slug: String = "",
    @Column var siteId: Int = 0, // Foreign key
    @Column var title: String = "",
    @Column var description: String = "",
    @Column var emoji: String = ""
) : Identifiable {
    override fun getId() = id

    override fun setId(id: Int) {
        this.id = id
    }
}

fun GutenbergLayoutCategory.transform(site: SiteModel) = GutenbergLayoutCategoryModel(
        slug = slug,
        siteId = site.id,
        title = title,
        description = description,
        emoji = emoji ?: ""
)

fun GutenbergLayoutCategoryModel.transform() = GutenbergLayoutCategory(
        slug = slug,
        title = title,
        description = description,
        emoji = emoji
)

fun List<GutenbergLayoutCategoryModel>.transform() = map { it.transform() }

fun List<GutenbergLayoutCategory>.transform(site: SiteModel) = map { it.transform(site) }

fun getCategoryId(siteId: Int, categorySlug: String): Int? = WellSql.select(GutenbergLayoutCategoryModel::class.java)
        .where()
        .equals(GutenbergLayoutCategoryModelTable.SITE_ID, siteId)
        .equals(GutenbergLayoutCategoryModelTable.SLUG, categorySlug)
        .endWhere().asModel.firstOrNull()?.id
