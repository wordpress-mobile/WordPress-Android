package org.wordpress.android.ui.layoutpicker

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.network.rest.wpcom.site.GutenbergLayout
import org.wordpress.android.fluxc.network.rest.wpcom.theme.StarterDesign

@Parcelize
class LayoutModel(private val starterDesign: StarterDesign? = null, private val blockLayout: GutenbergLayout? = null) :
        Parcelable {
    val slug: String
        get() = starterDesign?.slug ?: blockLayout?.slug ?: ""

    val title: String
        get() = starterDesign?.title ?: blockLayout?.title ?: ""

    val preview: String
        get() = starterDesign?.preview ?: blockLayout?.preview ?: ""

    val previewTablet: String
        get() = starterDesign?.previewTablet ?: blockLayout?.previewTablet ?: ""

    val previewMobile: String
        get() = starterDesign?.previewMobile ?: blockLayout?.previewMobile ?: ""

    val demoUrl: String
        get() = starterDesign?.demoUrl ?: blockLayout?.demoUrl ?: ""

    val content: String
        get() = blockLayout?.content ?: ""

    val categories: List<LayoutCategoryModel>
        get() = starterDesign?.categories?.toLayoutCategories()
                ?: blockLayout?.categories?.toLayoutCategories()
                ?: listOf()
}

@JvmName("designToLayoutModel") fun List<StarterDesign>.toLayoutModels() = map { LayoutModel(starterDesign = it) }

@JvmName("blockLayoutToLayoutModel") fun List<GutenbergLayout>.toLayoutModels() = map { LayoutModel(blockLayout = it) }

fun List<LayoutModel>.getFilteredLayouts(categorySlug: String) =
        filter { l -> l.categories.any { c -> c.slug == categorySlug } }
