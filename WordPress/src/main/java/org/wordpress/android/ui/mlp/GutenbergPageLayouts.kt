package org.wordpress.android.ui.mlp

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.network.rest.wpcom.site.GutenbergLayout
import org.wordpress.android.fluxc.network.rest.wpcom.site.GutenbergLayoutCategory

@Parcelize
data class GutenbergPageLayouts(
    val layouts: List<GutenbergLayout> = listOf(),
    val categories: List<GutenbergLayoutCategory> = listOf()
) : Parcelable {
    val isNotEmpty: Boolean
        get() = layouts.isNotEmpty() || categories.isNotEmpty()
    fun getFilteredLayouts(categorySlug: String) =
            layouts.filter { l -> l.categories.any { c -> c.slug == categorySlug } }
}
