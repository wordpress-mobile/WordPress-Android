package org.wordpress.android.ui.layoutpicker

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.network.rest.wpcom.site.GutenbergLayoutCategory
import org.wordpress.android.fluxc.network.rest.wpcom.theme.StarterDesignCategory

@Parcelize
@SuppressLint("ParcelCreator")
class LayoutCategoryModel(
    private val starterDesignCategory: StarterDesignCategory? = null,
    private val blockLayoutCategory: GutenbergLayoutCategory? = null,
    val isRecommended: Boolean = false,
    val randomizeOrder: Boolean = false
) : Parcelable {
    val slug: String
        get() = starterDesignCategory?.slug ?: blockLayoutCategory?.slug ?: ""
    val title: String
        get() = starterDesignCategory?.title ?: blockLayoutCategory?.title ?: ""
    val description: String
        get() = starterDesignCategory?.description ?: blockLayoutCategory?.description ?: ""
    val emoji: String
        get() = starterDesignCategory?.emoji ?: blockLayoutCategory?.emoji ?: ""
}

@JvmName("starterDesignToLayoutCategories")
fun List<StarterDesignCategory>.toLayoutCategories(recommended: Boolean = false, randomizeOrder: Boolean = false) =
        map {
            LayoutCategoryModel(
                    starterDesignCategory = it,
                    isRecommended = recommended,
                    randomizeOrder = randomizeOrder
            )
        }

@JvmName("gutenbergLayoutToLayoutCategories")
fun List<GutenbergLayoutCategory>.toLayoutCategories() = map { LayoutCategoryModel(blockLayoutCategory = it) }
