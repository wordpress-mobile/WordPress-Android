package org.wordpress.android.ui.sitecreation.theme

import org.wordpress.android.R
import org.wordpress.android.fluxc.network.rest.wpcom.theme.StarterDesign
import org.wordpress.android.fluxc.network.rest.wpcom.theme.StarterDesignCategory
import org.wordpress.android.ui.layoutpicker.LayoutCategoryModel
import org.wordpress.android.ui.layoutpicker.LayoutModel
import org.wordpress.android.ui.layoutpicker.toLayoutCategories
import org.wordpress.android.ui.layoutpicker.toLayoutModels
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class SiteDesignRecommendationProvider @Inject constructor(private val resourceProvider: ResourceProvider) {
    fun handleResponse(
        vertical: String,
        designs: List<StarterDesign>,
        categories: List<StarterDesignCategory>,
        responseHandler: (layouts: List<LayoutModel>, categories: List<LayoutCategoryModel>) -> Unit
    ) {
        val verticalSlug: String? = if (vertical.isNullOrEmpty()) null else getVerticalSlug(vertical)
        val hasRecommendations = !verticalSlug.isNullOrEmpty() &&
                designs.any { it.group.contains(verticalSlug) }

        if (hasRecommendations) {
            val recommendedTitle = resourceProvider.getString(R.string.hpp_recommended_title, vertical)
            // Create a new category for the recommendations
            val recommendedCategory = StarterDesignCategory(
                    slug = "recommended_$verticalSlug", // The slug is not used but should not already exist
                    title = recommendedTitle,
                    description = recommendedTitle,
                    emoji = ""
            )
            val designsWithRecommendations = designs.map {
                // Add the new category to the recommended designs so that they are filtered correctly
                // in the `LayoutPickerViewModel.loadLayouts()` method
                if (it.group.contains(verticalSlug)) {
                    it.copy(categories = it.categories + recommendedCategory)
                } else {
                    it
                }
            }.toLayoutModels()
            val categoriesWithRecommendations =
                    listOf(recommendedCategory).toLayoutCategories(recommended = true) +
                            categories.toLayoutCategories(randomizeOrder = true)
            responseHandler(designsWithRecommendations, categoriesWithRecommendations)
        } else {
            // If no designs are recommended for the selected vertical recommend the blog category
            val recommendedTitle = resourceProvider.getString(
                    R.string.hpp_recommended_title,
                    resourceProvider.getString(R.string.hpp_recommended_default_vertical)
            )
            val recommendedCategory = categories.firstOrNull { it.slug == "blog" }?.copy(
                    title = recommendedTitle,
                    description = recommendedTitle
            )
            if (recommendedCategory == null) {
                // If there is no blog category do not show a recommendation
                responseHandler(designs.toLayoutModels(), categories.toLayoutCategories(randomizeOrder = true))
            } else {
                val categoriesWithRecommendations =
                        listOf(recommendedCategory).toLayoutCategories(recommended = true, randomizeOrder = true) +
                                categories.toLayoutCategories(randomizeOrder = true)
                responseHandler(designs.toLayoutModels(), categoriesWithRecommendations)
            }
        }
    }

    @Suppress("UseCheckOrError")
    private fun getVerticalSlug(vertical: String): String? {
        val slugsArray = resourceProvider.getStringArray(R.array.site_creation_intents_slugs)
        val verticalArray = resourceProvider.getStringArray(R.array.site_creation_intents_strings)
        if (slugsArray.size != verticalArray.size) {
            throw IllegalStateException("Intents arrays size mismatch")
        }
        return slugsArray.getOrNull(verticalArray.indexOf(vertical))
    }
}
