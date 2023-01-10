package org.wordpress.android.ui.layoutpicker

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.network.rest.wpcom.site.GutenbergLayout
import org.wordpress.android.fluxc.network.rest.wpcom.theme.StarterDesign

@Parcelize
@SuppressLint("ParcelCreator")
data class LayoutModel(
    val slug: String,
    val title: String,
    val preview: String,
    val previewTablet: String,
    val previewMobile: String,
    val demoUrl: String,
    val categories: List<LayoutCategoryModel>
) : Parcelable {
    constructor(starterDesign: StarterDesign) : this(
        starterDesign.slug,
        starterDesign.title,
        starterDesign.preview,
        starterDesign.previewTablet,
        starterDesign.previewMobile,
        starterDesign.demoUrl,
        starterDesign.categories.toLayoutCategories()
    )

    constructor(blockLayout: GutenbergLayout) : this(
        blockLayout.slug,
        blockLayout.title,
        blockLayout.preview,
        blockLayout.previewTablet,
        blockLayout.previewMobile,
        blockLayout.demoUrl,
        blockLayout.categories.toLayoutCategories()
    )
}

@JvmName("designToLayoutModel")
fun List<StarterDesign>.toLayoutModels() = map { LayoutModel(starterDesign = it) }

@JvmName("blockLayoutToLayoutModel")
fun List<GutenbergLayout>.toLayoutModels() = map { LayoutModel(blockLayout = it) }

fun List<LayoutModel>.getFilteredLayouts(categorySlug: String) =
    filter { l -> l.categories.any { c -> c.slug == categorySlug } }
