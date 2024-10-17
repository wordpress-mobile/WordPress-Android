package org.wordpress.android.fluxc.model.layouts

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.site.GutenbergLayout

@Table
class GutenbergLayoutCategoriesModel(
    @PrimaryKey @Column private var id: Int = 0,
    @Column var layoutId: Int = 0, // Foreign key
    @Column var categoryId: Int = 0, // Foreign key
    @Column var siteId: Int = 0 // Foreign key
) : Identifiable {
    override fun getId() = id

    override fun setId(id: Int) {
        this.id = id
    }
}

@Suppress("NestedBlockDepth")
fun List<GutenbergLayout>.connections(site: SiteModel): List<GutenbergLayoutCategoriesModel> {
    val connections = arrayListOf<GutenbergLayoutCategoriesModel>()
    forEach { layout ->
        getLayoutId(site.id, layout.slug)?.let { layoutId ->
            layout.categories.forEach { category ->
                getCategoryId(site.id, category.slug)?.let { categoryId ->
                    connections.add(
                            GutenbergLayoutCategoriesModel(
                                    layoutId = layoutId,
                                    categoryId = categoryId,
                                    siteId = site.id
                            )
                    )
                }
            }
        }
    }
    return connections
}
