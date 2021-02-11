package org.wordpress.android.fluxc.model.layouts

import com.wellsql.generated.GutenbergLayoutModelTable
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.site.GutenbergLayout

@Table
class GutenbergLayoutModel(
    @PrimaryKey @Column private var id: Int = 0,
    @Column var slug: String = "",
    @Column var siteId: Int = 0, // Foreign key
    @Column var title: String = "",
    @Column var preview: String = "",
    @Column(name = "PREVIEW_TABLET") var previewTablet: String = "",
    @Column(name = "PREVIEW_MOBILE") var previewMobile: String = "",
    @Column var content: String = "",
    @Column(name = "DEMO_URL") var demoUrl: String = ""
) : Identifiable {
    override fun getId() = id

    override fun setId(id: Int) {
        this.id = id
    }
}

fun GutenbergLayout.transform(site: SiteModel) = GutenbergLayoutModel(
        slug = slug,
        siteId = site.id,
        title = title,
        preview = preview,
        previewMobile = previewMobile,
        previewTablet = previewTablet,
        content = content,
        demoUrl = demoUrl
)

fun GutenbergLayoutModel.transform(categories: List<GutenbergLayoutCategoryModel>) = GutenbergLayout(
        slug = slug,
        title = title,
        preview = preview,
        previewTablet = previewTablet,
        previewMobile = previewMobile,
        content = content,
        demoUrl = demoUrl,
        categories = categories.transform()
)

fun List<GutenbergLayout>.transform(site: SiteModel) = map { it.transform(site) }

fun getLayoutId(siteId: Int, layoutSlug: String): Int? = WellSql.select(GutenbergLayoutModel::class.java)
        .where()
        .equals(GutenbergLayoutModelTable.SITE_ID, siteId)
        .equals(GutenbergLayoutModelTable.SLUG, layoutSlug)
        .endWhere().asModel.firstOrNull()?.id
