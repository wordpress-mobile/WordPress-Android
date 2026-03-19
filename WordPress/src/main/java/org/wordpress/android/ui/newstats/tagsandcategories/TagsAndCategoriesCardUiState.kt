package org.wordpress.android.ui.newstats.tagsandcategories

/**
 * UI State for the Tags & Categories insights card.
 */
sealed class TagsAndCategoriesCardUiState {
    data object Loading : TagsAndCategoriesCardUiState()

    data object NoData : TagsAndCategoriesCardUiState()

    data class Loaded(
        val items: List<TagGroupUiItem>,
        val maxViewsForBar: Long
    ) : TagsAndCategoriesCardUiState()

    data class Error(
        val message: String
    ) : TagsAndCategoriesCardUiState()
}

/**
 * A tag group displayed in the card list.
 */
data class TagGroupUiItem(
    val name: String,
    val tags: List<TagUiItem>,
    val views: Long,
    val displayType: TagGroupDisplayType
) {
    val isExpandable: Boolean get() = tags.size > 1
}

/**
 * A single tag within a tag group.
 */
data class TagUiItem(
    val name: String,
    val tagType: String
)

/**
 * Display type for a tag group, determines which icon to show.
 */
enum class TagGroupDisplayType {
    CATEGORY,
    TAG,
    MIXED;

    companion object {
        private const val CATEGORY_TYPE = "category"

        fun fromTagType(
            tagType: String
        ): TagGroupDisplayType =
            if (tagType == CATEGORY_TYPE) {
                CATEGORY
            } else {
                TAG
            }

        fun fromTags(
            tags: List<TagUiItem>
        ): TagGroupDisplayType {
            val allCategories = tags.all {
                it.tagType == CATEGORY_TYPE
            }
            val allTags = tags.none {
                it.tagType == CATEGORY_TYPE
            }
            return when {
                allCategories -> CATEGORY
                allTags -> TAG
                else -> MIXED
            }
        }
    }
}
