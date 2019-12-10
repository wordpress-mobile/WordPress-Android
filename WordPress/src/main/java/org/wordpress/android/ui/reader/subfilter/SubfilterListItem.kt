package org.wordpress.android.ui.reader.subfilter

import org.json.JSONObject
import org.wordpress.android.R
import org.wordpress.android.datasets.ReaderBlogTable
import org.wordpress.android.models.ReaderBlog
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType.DIVIDER
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType.SECTION_TITLE
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType.SITE
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType.SITE_ALL
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType.TAG
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T

const val JSON_KEY_ITEM_TYPE = "item-type"
const val JSON_KEY_SITE_FEED_ID = "feed-id"
const val JSON_KEY_SITE_BLOG_ID = "blog-id"
const val JSON_KEY_TAG_NAME = "tag-name"
const val JSON_KEY_TAG_TYPE = "tag-type"

sealed class SubfilterListItem(val type: ItemType) {
    open var isSelected: Boolean = false
    open var onClickAction: ((filter: SubfilterListItem) -> Unit)? = null
    open val label: UiString? = null

    companion object {
        @JvmStatic
        fun fromJson(json: String): SubfilterListItem {
            val jsonObj = try {
                JSONObject(json)
            } catch (e: Exception) {
                AppLog.d(T.READER, "fromJson > could not parse json string: ${e.message}")
                JSONObject()
            }

            val type = jsonObj.optInt(JSON_KEY_ITEM_TYPE, SITE_ALL.value).let {
                ItemType.fromInt(it)
            }

            return when (type) {
                SITE -> {
                    val blogId = jsonObj.optLong(JSON_KEY_SITE_BLOG_ID)
                    val feedId = jsonObj.optLong(JSON_KEY_SITE_FEED_ID)

                    var blogInfo: ReaderBlog?

                    blogInfo = if (blogId != 0L) {
                        ReaderBlogTable.getBlogInfo(blogId)
                    } else if (feedId != 0L) {
                        ReaderBlogTable.getFeedInfo(feedId)
                    } else null

                    blogInfo?.let {
                        Site(
                            blog = blogInfo,
                            onClickAction = null
                        )
                    } ?: SiteAll(onClickAction = null)
                }
                TAG -> {
                    val tagName = jsonObj.optString(JSON_KEY_TAG_NAME)
                    val tagType = jsonObj.optInt(JSON_KEY_TAG_TYPE)

                    if (tagName.isNotEmpty()) {
                        val tag = ReaderUtils.getTagFromTagName(tagName, ReaderTagType.fromInt(tagType))
                        Tag(
                            tag = tag,
                            onClickAction = null
                        )
                    } else {
                        SiteAll(onClickAction = null)
                    }
                }
                SITE_ALL -> SiteAll(onClickAction = null)
                else -> throw IllegalArgumentException("fromJson > Unexpected Subfilter type $type")
            }
        }
    }

    fun toJson(): String {
        val jsonObject = JSONObject()

        jsonObject.put(JSON_KEY_ITEM_TYPE, type.value)

        when (this) {
            is Site -> {
                jsonObject.put(JSON_KEY_SITE_FEED_ID, blog.feedId)
                jsonObject.put(JSON_KEY_SITE_BLOG_ID, blog.blogId)
            }
            is Tag -> {
                jsonObject.put(JSON_KEY_TAG_NAME, tag.tagSlug)
                jsonObject.put(JSON_KEY_TAG_TYPE, tag.tagType.toInt())
            }
        }

        return jsonObject.toString()
    }

    fun isSameItem(otherItem: SubfilterListItem?): Boolean {
        if (otherItem == null) return false

        return if (type == otherItem.type) {
            when (type) {
                SECTION_TITLE -> label == otherItem.label
                SITE -> (this as Site).blog.isSameAs((otherItem as Site).blog)
                TAG -> (this as Tag).tag == (otherItem as Tag).tag
                SITE_ALL,
                DIVIDER -> true
            }
        } else {
            false
        }
    }

    enum class ItemType constructor(val value: Int) {
        SECTION_TITLE(0),
        SITE_ALL(1),
        SITE(2),
        DIVIDER(3),
        TAG(4);

        companion object {
            fun fromInt(value: Int): ItemType? = values().firstOrNull { it.value == value }
        }
    }

    data class SectionTitle(override val label: UiString) : SubfilterListItem(SECTION_TITLE)

    object Divider : SubfilterListItem(DIVIDER)

    data class SiteAll(
        override var isSelected: Boolean = false,
        override var onClickAction: ((filter: SubfilterListItem) -> Unit)?
    ) : SubfilterListItem(SITE_ALL) {
        override val label: UiString = UiStringRes(R.string.reader_filter_all_sites)
    }

    data class Site(
        override var isSelected: Boolean = false,
        override var onClickAction: ((filter: SubfilterListItem) -> Unit)?,
        val blog: ReaderBlog
    ) : SubfilterListItem(SITE) {
        override val label: UiString = if (blog.name.isNotEmpty()) {
            UiStringText(blog.name)
        } else {
            UiStringRes(R.string.reader_untitled_post)
        }
    }

    data class Tag(
        override var isSelected: Boolean = false,
        override var onClickAction: ((filter: SubfilterListItem) -> Unit)?,
        val tag: ReaderTag
    ) : SubfilterListItem(TAG) {
        override val label: UiString = UiStringText(tag.tagTitle)
    }
}
