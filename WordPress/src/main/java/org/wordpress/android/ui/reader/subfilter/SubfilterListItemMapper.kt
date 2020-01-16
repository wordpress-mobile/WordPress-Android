package org.wordpress.android.ui.reader.subfilter

import com.google.gson.Gson
import org.wordpress.android.datasets.ReaderBlogTable
import org.wordpress.android.models.ReaderBlog
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType.SITE
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType.SITE_ALL
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType.TAG
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Site
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.SiteAll
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Tag
import org.wordpress.android.ui.reader.utils.ReaderUtils
import javax.inject.Inject

private data class MappedItem(
    val type: Int,
    var feedId: Long = 0,
    var blogId: Long = 0,
    var tagSlug: String = "",
    var tagType: Int = 0
)

class SubfilterListItemMapper @Inject constructor() {
    fun fromJson(
        json: String,
        onClickAction: ((filter: SubfilterListItem) -> Unit),
        isSelected: Boolean
    ): SubfilterListItem {

        val mappedItem = if (json.isEmpty()) {
            MappedItem(type = SITE_ALL.value)
        } else {
            val gson = Gson()
            gson.fromJson(json, MappedItem::class.java)
        }

        return when (mappedItem.type) {
            SITE.value -> {
                val blogInfo: ReaderBlog? = if (mappedItem.blogId != 0L) {
                    ReaderBlogTable.getBlogInfo(mappedItem.blogId)
                } else if (mappedItem.feedId != 0L) {
                    ReaderBlogTable.getFeedInfo(mappedItem.feedId)
                } else null

                blogInfo?.let {
                    Site(
                        blog = blogInfo,
                        onClickAction = onClickAction,
                        isSelected = isSelected
                    )
                } ?: SiteAll(onClickAction = onClickAction, isSelected = isSelected)
            }
            TAG.value -> {
                if (mappedItem.tagSlug.isNotEmpty()) {
                    val tag = ReaderUtils.getTagFromTagName(
                            mappedItem.tagSlug,
                            ReaderTagType.fromInt(mappedItem.tagType)
                    )
                    Tag(
                        tag = tag,
                        onClickAction = onClickAction,
                        isSelected = isSelected
                    )
                } else {
                    SiteAll(onClickAction = onClickAction, isSelected = isSelected)
                }
            }
            SITE_ALL.value -> SiteAll(onClickAction = onClickAction, isSelected = isSelected)
            else -> throw IllegalArgumentException("fromJson > Unexpected Subfilter type $mappedItem.type")
        }
    }

    fun toJson(item: SubfilterListItem): String {
        val gson = Gson()

        val mappedItem = MappedItem(type = item.type.value)

        when (item) {
            is Site -> {
                mappedItem.feedId = item.blog.feedId
                mappedItem.blogId = item.blog.blogId
            }
            is Tag -> {
                mappedItem.tagSlug = item.tag.tagSlug
                mappedItem.tagType = item.tag.tagType.toInt()
            }
        }

        return gson.toJson(mappedItem)
    }
}
