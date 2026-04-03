package org.wordpress.android.ui.reader.utils

import org.wordpress.android.datasets.ReaderTagTable
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.util.PhotonUtils
import org.wordpress.android.util.UrlUtils
import java.net.URI

/**
 * Pure utility methods extracted from ReaderUtils for use by
 * reader-models (models and datasets). ReaderUtils in the app module
 * delegates to these where possible.
 */
object ReaderSlugUtils {
    @JvmStatic
    fun sanitizeWithDashes(title: String): String {
        val trimmedTitle = title.trim()
        return if (isValidUrlEncodedString(trimmedTitle)) {
            trimmedTitle
        } else {
            trimmedTitle
                .replace("&[^\\s]*;".toRegex(), "")
                .replace("[\\.\\s]+".toRegex(), "-")
                .replace(
                    "[^\\p{L}\\p{Nd}\\-]+".toRegex(),
                    ""
                )
                .replace("--".toRegex(), "-")
        }
    }

    @JvmStatic
    fun getTagFromTagName(
        tagName: String,
        tagType: ReaderTagType
    ): ReaderTag {
        return getTagFromTagName(tagName, tagType, false)
    }

    @JvmStatic
    fun getTagFromTagName(
        tagName: String,
        tagType: ReaderTagType,
        markDefaultIfInMemory: Boolean
    ): ReaderTag {
        val tag = ReaderTagTable.getTag(tagName, tagType)
        return tag ?: createTagFromTagName(
            tagName, tagType, markDefaultIfInMemory
        )
    }

    @JvmOverloads
    @JvmStatic
    fun createTagFromTagName(
        tagName: String,
        tagType: ReaderTagType,
        isDefaultInMemoryTag: Boolean = false
    ): ReaderTag {
        val tagSlug = sanitizeWithDashes(tagName).lowercase()
        val tagDisplayName =
            if (tagType == ReaderTagType.DEFAULT) tagName else tagSlug
        return ReaderTag(
            tagSlug, tagDisplayName, tagName,
            null, tagType, isDefaultInMemoryTag
        )
    }

    @JvmStatic
    fun getCommaSeparatedTagSlugs(tags: ReaderTagList): String {
        val slugs = StringBuilder()
        tags.forEach { tag ->
            if (slugs.isNotEmpty()) {
                slugs.append(",")
            }
            slugs.append(sanitizeWithDashes(tag.tagSlug))
        }
        return slugs.toString()
    }

    @JvmStatic
    fun getTagsFromCommaSeparatedSlugs(
        commaSeparatedTagSlugs: String
    ): ReaderTagList {
        val tags = ReaderTagList()
        if (commaSeparatedTagSlugs.trim().isNotEmpty()) {
            val slugs = commaSeparatedTagSlugs.split(",".toRegex())
            slugs.forEach { slug ->
                tags.add(getTagFromTagName(slug, ReaderTagType.DEFAULT))
            }
        }
        return tags
    }

    @JvmStatic
    fun getResizedImageUrl(
        imageUrl: String,
        width: Int,
        height: Int,
        isPrivate: Boolean,
        isPrivateAtomic: Boolean
    ): String {
        return getResizedImageUrl(
            imageUrl, width, height,
            isPrivate, isPrivateAtomic,
            PhotonUtils.Quality.MEDIUM
        )
    }

    @JvmStatic
    @Suppress("LongParameterList")
    fun getResizedImageUrl(
        imageUrl: String,
        width: Int,
        height: Int,
        isPrivate: Boolean,
        isPrivateAtomic: Boolean,
        quality: PhotonUtils.Quality
    ): String {
        val unescapedUrl = org.apache.commons.text
            .StringEscapeUtils.unescapeHtml4(imageUrl)
        return if (isPrivate && !isPrivateAtomic) {
            getImageForDisplayWithoutPhoton(
                unescapedUrl, width, height, true
            )
        } else {
            PhotonUtils.getPhotonImageUrl(
                unescapedUrl, width, height,
                quality, isPrivateAtomic
            )
        }
    }

    private fun getImageForDisplayWithoutPhoton(
        imageUrl: String,
        width: Int,
        height: Int,
        forceHttps: Boolean
    ): String {
        if (imageUrl.isEmpty()) return ""
        val query = if (width > 0 && height > 0) {
            "?w=$width&h=$height"
        } else if (width > 0) {
            "?w=$width"
        } else if (height > 0) {
            "?h=$height"
        } else {
            ""
        }
        return if (forceHttps) {
            UrlUtils.removeQuery(UrlUtils.makeHttps(imageUrl)) + query
        } else {
            UrlUtils.removeQuery(imageUrl) + query
        }
    }

    @Suppress("SwallowedException")
    private fun isValidUrlEncodedString(title: String): Boolean {
        try {
            URI.create(title)
            return true
        } catch (e: IllegalArgumentException) {
            return false
        }
    }
}
