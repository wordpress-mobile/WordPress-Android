package org.wordpress.android.ui.mlp

import org.wordpress.android.fluxc.network.rest.wpcom.site.GutenbergLayout
import org.wordpress.android.fluxc.network.rest.wpcom.site.GutenbergLayoutCategory

data class GutenbergPageLayouts(
    val layouts: List<GutenbergLayout> = listOf(),
    val categories: List<GutenbergLayoutCategory> = listOf()
) {
    fun getFilteredLayouts(categorySlug: String) =
            layouts.filter { l -> l.categories.any { c -> c.slug == categorySlug } }
}
