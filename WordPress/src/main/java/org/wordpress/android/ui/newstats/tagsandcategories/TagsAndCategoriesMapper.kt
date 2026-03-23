package org.wordpress.android.ui.newstats.tagsandcategories

import org.wordpress.android.ui.newstats.datasource.TagGroupData
import javax.inject.Inject

class TagsAndCategoriesMapper @Inject constructor() {
    fun mapToUiItems(
        tagGroups: List<TagGroupData>
    ): List<TagGroupUiItem> = tagGroups.map { group ->
        val tagUiItems = group.tags.map { tag ->
            TagUiItem(
                name = tag.name,
                tagType = tag.tagType
            )
        }
        TagGroupUiItem(
            name = tagUiItems.joinToString(
                TAGS_SEPARATOR
            ) { it.name },
            tags = tagUiItems,
            views = group.views,
            displayType =
                TagGroupDisplayType.fromTags(tagUiItems)
        )
    }

    companion object {
        private const val TAGS_SEPARATOR = " / "
    }
}
