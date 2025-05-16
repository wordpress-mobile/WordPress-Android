package org.wordpress.android.ui.reader.utils

import android.content.Context
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.R
import org.wordpress.android.datasets.ReaderCommentTable
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.datasets.ReaderTagTable
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.ui.FilteredRecyclerView
import org.wordpress.android.ui.reader.ReaderConstants
import org.wordpress.android.ui.reader.services.update.TagUpdateClientUtilsProvider
import org.wordpress.android.util.FormatUtils
import org.wordpress.android.util.PhotonUtils
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.UrlUtils
import java.net.URI
import androidx.core.net.toUri

object ReaderUtils {
    @JvmStatic
    fun getResizedImageUrl(
        imageUrl: String,
        width: Int,
        height: Int,
        isPrivate: Boolean,
        isPrivateAtomic: Boolean
    ): String {
        return getResizedImageUrl(
            imageUrl = imageUrl,
            width = width,
            height = height,
            isPrivate = isPrivate,
            isPrivateAtomic = isPrivateAtomic,
            quality = PhotonUtils.Quality.MEDIUM
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
        val unescapedUrl = StringEscapeUtils.unescapeHtml4(imageUrl)
        return if (isPrivate && !isPrivateAtomic) {
            getImageForDisplayWithoutPhoton(
                imageUrl = unescapedUrl,
                width = width,
                height = height,
                forceHttps = true
            )
        } else {
            PhotonUtils.getPhotonImageUrl(
                unescapedUrl,
                width,
                height,
                quality,
                isPrivateAtomic
            )
        }
    }

    fun getResizedImageUrl(
        imageUrl: String,
        width: Int,
        height: Int,
        siteAccessibilityInfo: SiteAccessibilityInfo
    ): String {
        return getResizedImageUrl(
            imageUrl = imageUrl,
            width = width,
            height = height,
            siteAccessibilityInfo = siteAccessibilityInfo,
            quality = PhotonUtils.Quality.MEDIUM
        )
    }

    fun getResizedImageUrl(
        imageUrl: String,
        width: Int,
        height: Int,
        siteAccessibilityInfo: SiteAccessibilityInfo,
        quality: PhotonUtils.Quality
    ): String {
        val unescapedUrl = StringEscapeUtils.unescapeHtml4(imageUrl)

        return if (siteAccessibilityInfo.isPhotonCapable) {
            PhotonUtils.getPhotonImageUrl(
                unescapedUrl,
                width,
                height,
                quality,
                siteAccessibilityInfo.siteVisibility == SiteVisibility.PRIVATE_ATOMIC
            )
        } else {
            getImageForDisplayWithoutPhoton(
                imageUrl = unescapedUrl,
                width = width,
                height = height,
                forceHttps = siteAccessibilityInfo.siteVisibility == SiteVisibility.PRIVATE
            )
        }
    }

    /*
     * use this to request a reduced size image from not photon capable sites
     * (i.e. a private post - images in private posts can't use photon
     * but these are usually wp images so they support the h= and w= query params)
     */
    private fun getImageForDisplayWithoutPhoton(
        imageUrl: String,
        width: Int,
        height: Int,
        forceHttps: Boolean
    ): String {
        if (imageUrl.isEmpty()) {
            return ""
        }
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
            // remove the existing query string, add the new one, and make sure the url is https:
            UrlUtils.removeQuery(UrlUtils.makeHttps(imageUrl)) + query
        } else {
            // remove the existing query string, add the new one
            UrlUtils.removeQuery(imageUrl) + query
        }
    }

    /*
     * returns the passed string formatted for use with our API - see sanitize_title_with_dashes
     * https://git.io/JqUEP
     * http://stackoverflow.com/a/1612015/1673548
     */
    @JvmStatic
    fun sanitizeWithDashes(title: String): String {
        val trimmedTitle = title.trim()
        return if (isValidUrlEncodedString(trimmedTitle)
        ) {
            trimmedTitle
        } else {
            trimmedTitle
                .replace("&[^\\s]*;".toRegex(), "") // remove html entities
                .replace("[\\.\\s]+".toRegex(), "-") // replace periods and whitespace with a dash
                .replace(
                    "[^\\p{L}\\p{Nd}\\-]+".toRegex(),
                    ""
                ) // remove remaining non-alphanumeric/non-dash chars (Unicode aware)
                .replace("--".toRegex(), "-") // reduce double dashes potentially added above
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

    /*
     * returns the long text to use for a like label ("Liked by 3 people", etc.)
     */
    @JvmStatic
    fun getLongLikeLabelText(
        context: Context,
        numLikes: Int,
        isLikedByCurrentUser: Boolean
    ): String {
        return if (isLikedByCurrentUser) {
            when (numLikes) {
                1 -> context.getString(R.string.reader_likes_only_you)
                2 -> context.getString(R.string.reader_likes_you_and_one)
                else -> {
                    val youAndMultiLikes = context.getString(R.string.reader_likes_you_and_multi)
                    String.format(youAndMultiLikes, numLikes - 1)
                }
            }
        } else if (numLikes == 1) {
            context.getString(R.string.reader_likes_one)
        } else {
            val likes = context.getString(R.string.reader_likes_multi)
            String.format(likes, numLikes)
        }
    }

    /*
     * short like text ("1 like," "5 likes," etc.)
     */
    @JvmStatic
    fun getShortLikeLabelText(context: Context, numLikes: Int): String {
        return when (numLikes) {
            0 -> context.getString(R.string.reader_short_like_count_none)
            1 -> context.getString(R.string.reader_short_like_count_one)
            else -> {
                val count = FormatUtils.formatInt(numLikes)
                String.format(
                    context.getString(R.string.reader_short_like_count_multi),
                    count
                )
            }
        }
    }

    fun getShortCommentLabelText(context: Context, numComments: Int): String {
        return if (numComments == 1) {
            context.getString(R.string.reader_short_comment_count_one)
        } else {
            val count = FormatUtils.formatInt(numComments)
            String.format(context.getString(R.string.reader_short_comment_count_multi), count)
        }
    }

    fun getTextForCommentSnippet(context: Context, numComments: Int): String {
        return when (numComments) {
            0 -> context.getString(R.string.comments)
            1 -> context.getString(R.string.reader_short_comment_count_one)
            else -> {
                val count = FormatUtils.formatInt(numComments)
                String.format(
                    context.getString(R.string.reader_short_comment_count_multi),
                    count
                )
            }
        }
    }

    /*
     * returns true if a ReaderPost and ReaderComment exist for the passed Ids
     */
    fun postAndCommentExists(blogId: Long, postId: Long, commentId: Long): Boolean {
        return ReaderPostTable.postExists(blogId, postId)
                && ReaderCommentTable.commentExists(blogId, postId, commentId)
    }

    /*
     * used by Discover site picks to add a "Visit [BlogName]" link which shows the
     * native blog preview for that blog
     */
    fun makeBlogPreviewUrl(blogId: Long): String {
        return "wordpress://blogpreview?blogId=$blogId"
    }

    fun isBlogPreviewUrl(url: String): Boolean {
        return url.startsWith("wordpress://blogpreview")
    }

    fun getBlogIdFromBlogPreviewUrl(url: String): Long {
        if (isBlogPreviewUrl(url)) {
            val strBlogId = url.toUri().getQueryParameter("blogId")
            return StringUtils.stringToLong(strBlogId)
        } else {
            return 0
        }
    }

    fun isTagUrl(url: String): Boolean {
        return url.matches("^https?://wordpress\\.com/tag/[^/]+$".toRegex())
    }

    fun getTagFromTagUrl(url: String): String {
        return if (isTagUrl(url)) {
            url.substring(url.lastIndexOf("/") + 1)
        } else {
            ""
        }
    }

    /*
     * returns a tag object from the passed endpoint if tag is in database, otherwise null
     */
    fun getTagFromEndpoint(endpoint: String): ReaderTag? {
        return ReaderTagTable.getTagFromEndpoint(endpoint)
    }

    /*
     * returns a tag object from the passed tag name - first checks for it in the tag db
     * (so we can also get its title & endpoint), returns a new tag if that fails
     */
    @JvmStatic
    fun getTagFromTagName(
        tagName: String,
        tagType: ReaderTagType
    ): ReaderTag {
        return getTagFromTagName(
            tagName = tagName,
            tagType = tagType,
            markDefaultIfInMemory = false
        )
    }

    @JvmStatic
    fun getTagFromTagName(
        tagName: String,
        tagType: ReaderTagType,
        markDefaultIfInMemory: Boolean
    ): ReaderTag {
        val tag = ReaderTagTable.getTag(tagName, tagType)
        return tag
            ?: createTagFromTagName(
                tagName = tagName,
                tagType = tagType,
                isDefaultInMemoryTag = markDefaultIfInMemory
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
        val tagDisplayName = if (tagType == ReaderTagType.DEFAULT) tagName else tagSlug
        return ReaderTag(
            tagSlug,
            tagDisplayName,
            tagName,
            null,
            tagType,
            isDefaultInMemoryTag
        )
    }

    /*
     * returns the default tag, which is the one selected by default in the reader when
     * the user hasn't already chosen one
     */
    fun getDefaultTag(): ReaderTag {
        var defaultTag =
            getTagFromEndpoint(ReaderTag.TAG_ENDPOINT_DEFAULT)
        if (defaultTag == null) {
            defaultTag = getTagFromTagName(
                ReaderTag.TAG_TITLE_DEFAULT,
                ReaderTagType.DEFAULT,
                true
            )
        }
        return defaultTag
    }

    fun getDefaultTagFromDbOrCreateInMemory(
        context: Context,
        clientUtilsProvider: TagUpdateClientUtilsProvider
    ): ReaderTag {
        // getDefaultTag() tries to get the default tag from reader db by tag endpoint or tag name.
        // In case it cannot get the default tag from db, it creates it in memory with createTagFromTagName
        val tag = getDefaultTag()

        if (tag.isDefaultInMemoryTag) {
            // if the tag was created in memory from createTagFromTagName
            // we need to set some fields as below before to use it
            tag.tagTitle =
                context.getString(R.string.reader_subscribed_display_name)
            tag.tagDisplayName =
                context.getString(R.string.reader_subscribed_display_name)

            var baseUrl = clientUtilsProvider.getTagUpdateEndpointURL()

            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length - 1)
            }

            tag.endpoint = baseUrl + ReaderTag.FOLLOWING_PATH
        }

        return tag
    }

    /*
     * used when storing search results in the reader post table
     */
    @JvmStatic
    fun getTagForSearchQuery(query: String): ReaderTag {
        val trimQuery = query.trim()
        val slug = sanitizeWithDashes(trimQuery)
        return ReaderTag(slug, trimQuery, trimQuery, null, ReaderTagType.SEARCH)
    }

    fun getDefaultTagInfo(): Map<String, TagInfo> {
        // Note that the following is the desired order in the tabs
        // (see usage in prependDefaults)
        val defaultTagInfo: MutableMap<String, TagInfo> =
            LinkedHashMap()

        defaultTagInfo[ReaderConstants.KEY_FOLLOWING] =
            TagInfo(ReaderTagType.DEFAULT, ReaderTag.FOLLOWING_PATH)
        defaultTagInfo[ReaderConstants.KEY_DISCOVER] =
            TagInfo(ReaderTagType.DEFAULT, ReaderTag.DISCOVER_PATH)
        defaultTagInfo[ReaderConstants.KEY_LIKES] =
            TagInfo(ReaderTagType.DEFAULT, ReaderTag.LIKED_PATH)
        defaultTagInfo[ReaderConstants.KEY_SAVED] = TagInfo(ReaderTagType.BOOKMARKED, "")

        return defaultTagInfo
    }

    private fun putIfAbsentDone(
        defaultTags: MutableMap<String, ReaderTag>,
        key: String,
        tag: ReaderTag
    ): Boolean {
        var insertionDone = false

        if (defaultTags[key] == null) {
            defaultTags[key] = tag
            insertionDone = true
        }

        return insertionDone
    }

    private fun prependDefaults(
        defaultTags: Map<String, ReaderTag>,
        orderedTagList: ReaderTagList,
        defaultTagInfo: Map<String, TagInfo>
    ) {
        if (defaultTags.isEmpty()) return

        val reverseOrderedKeys = ArrayList(defaultTagInfo.keys)
        reverseOrderedKeys.reverse()

        for (key in reverseOrderedKeys) {
            if (defaultTags.containsKey(key)) {
                val tag = defaultTags[key]
                orderedTagList.add(0, tag)
            }
        }
    }

    private fun defaultTagFoundAndAdded(
        defaultTagInfos: Map<String, TagInfo>,
        tag: ReaderTag,
        defaultTags: MutableMap<String, ReaderTag>
    ): Boolean {
        var foundAndAdded = false

        for (key in defaultTagInfos.keys) {
            if (defaultTagInfos[key]!!.isDesiredTag(tag)) {
                if (putIfAbsentDone(defaultTags, key, tag)) {
                    foundAndAdded = true
                }
                break
            }
        }

        return foundAndAdded
    }

    fun getOrderedTagsList(
        tagList: ReaderTagList,
        defaultTagInfos: Map<String, TagInfo>
    ): ReaderTagList {
        val orderedTagList = ReaderTagList()
        val defaultTags: MutableMap<String, ReaderTag> = HashMap()

        for (tag in tagList) {
            if (defaultTagFoundAndAdded(defaultTagInfos, tag, defaultTags)) {
                continue
            }
            orderedTagList.add(tag)
        }
        prependDefaults(defaultTags, orderedTagList, defaultTagInfos)

        return orderedTagList
    }

    @Suppress("ReturnCount")
    fun isTagManagedInFollowingTab(
        tag: ReaderTag,
        isTopLevelReader: Boolean,
        recyclerView: FilteredRecyclerView
    ): Boolean {
        if (isTopLevelReader) {
            if (tag.isDefaultInMemoryTag) {
                return true
            }

            val isSpecialTag = tag.isDiscover || tag.isPostsILike || tag.isBookmarked
            val tabsInitializingNow = recyclerView.currentFilter == null
            val tagIsFollowedSitesOrAFollowedTag =
                tag.isFollowedSites || tag.tagType == ReaderTagType.FOLLOWED

            return if (isSpecialTag) {
                false
            } else if (tabsInitializingNow) {
                tagIsFollowedSitesOrAFollowedTag
            } else if (recyclerView.currentFilter is ReaderTag) {
                if (recyclerView.isValidFilter(tag)) {
                    tag.isFollowedSites
                } else {
                    // If we reach here it means we are setting a followed tag or site in the Following tab
                    true
                }
            } else {
                false
            }
        } else {
            return tag.isFollowedSites
        }
    }

    @Suppress("ReturnCount")
    fun getValidTagForSharedPrefs(
        tag: ReaderTag,
        isTopLevelReader: Boolean,
        recyclerView: FilteredRecyclerView,
        defaultTag: ReaderTag
    ): ReaderTag {
        if (!isTopLevelReader) {
            return tag
        }

        val isValidFilter = recyclerView.isValidFilter(tag)
        val isSpecialTag = tag.isDiscover || tag.isPostsILike || tag.isBookmarked
        if (!isSpecialTag && !isValidFilter && isTagManagedInFollowingTab(
                tag = tag,
                isTopLevelReader = isTopLevelReader,
                recyclerView = recyclerView
            )
        ) {
            return defaultTag
        }

        return tag
    }

    @JvmStatic
    fun getCommaSeparatedTagSlugs(tags: ReaderTagList): String {
        val slugs = StringBuilder()
        tags.forEach { tag ->
            if (slugs.isNotEmpty()) {
                slugs.append(",")
            }
            val tagNameForApi = sanitizeWithDashes(tag.tagSlug)
            slugs.append(tagNameForApi)
        }
        return slugs.toString()
    }

    @JvmStatic
    fun getTagsFromCommaSeparatedSlugs(commaSeparatedTagSlugs: String): ReaderTagList {
        val tags = ReaderTagList()
        if (commaSeparatedTagSlugs.trim().isNotEmpty()) {
            val slugs = commaSeparatedTagSlugs.split(",".toRegex())
            slugs.forEach { slug ->
                val tag = getTagFromTagName(slug, ReaderTagType.DEFAULT)
                tags.add(tag)
            }
        }
        return tags
    }

    /**
     * isExternalFeed identifies an external RSS feed
     * blogId will be empty for feeds and in some instances, it is explicitly
     * setting blogId equal to the feedId
     */
    @JvmStatic
    fun isExternalFeed(blogId: Long, feedId: Long): Boolean {
        return (blogId == 0L && feedId != 0L) || blogId == feedId
    }

    fun getReportPostUrl(blogUrl: String): String {
        return "https://wordpress.com/abuse/?report_url=$blogUrl"
    }

    fun getReportUserUrl(blogUrl: String, userId: Long): String {
        return getReportPostUrl(blogUrl) + "&report_user_id=" + userId
    }

    fun postExists(blogId: Long, postId: Long): Boolean {
        return ReaderPostTable.postExists(blogId, postId)
    }

    fun commentExists(blogId: Long, postId: Long, commentId: Long): Boolean {
        return ReaderCommentTable.commentExists(blogId, postId, commentId)
    }

    /**
     * Self-hosted sites have a site id of 0, but we use -1 to indicate a self-hosted site
     *
     * @param authorBlogId site id of the post's author
     */
    fun isSelfHosted(authorBlogId: Long): Boolean {
        return authorBlogId < 1
    }
}
