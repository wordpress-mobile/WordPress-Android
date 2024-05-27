package org.wordpress.android.ui.reader.utils

import dagger.Reusable
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.ui.reader.services.update.TagUpdateClientUtilsProvider
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

/**
 * Injectable wrapper around ReaderUtils.
 *
 * ReaderUtils interface is consisted of static methods, which make the client code difficult to test/mock.
 * Main purpose of this wrapper is to make testing easier.
 */
@Reusable
class ReaderUtilsWrapper @Inject constructor(
    private val contextProvider: ContextProvider,
    private val tagUpdateClientUtilsProvider: TagUpdateClientUtilsProvider
) {
    fun getResizedImageUrl(imageUrl: String?, width: Int, height: Int, isPrivate: Boolean, isAtomic: Boolean): String? =
        ReaderUtils.getResizedImageUrl(imageUrl, width, height, isPrivate, isAtomic)

    fun getResizedImageUrl(
        imageUrl: String?,
        width: Int,
        height: Int,
        siteAccessibilityInfo: SiteAccessibilityInfo
    ): String? = ReaderUtils.getResizedImageUrl(imageUrl, width, height, siteAccessibilityInfo)

    fun getTagFromTagName(tagName: String, tagType: ReaderTagType): ReaderTag =
        ReaderUtils.getTagFromTagName(tagName, tagType)

    fun getDefaultTagFromDbOrCreateInMemory() =
        ReaderUtils.getDefaultTagFromDbOrCreateInMemory(contextProvider.getContext(), tagUpdateClientUtilsProvider)

    fun getLongLikeLabelText(numLikes: Int, isLikedByCurrentUser: Boolean): String =
        ReaderUtils.getLongLikeLabelText(contextProvider.getContext(), numLikes, isLikedByCurrentUser)

    fun isExternalFeed(blogId: Long, feedId: Long): Boolean = ReaderUtils.isExternalFeed(blogId, feedId)

    fun getReportPostUrl(blogUrl: String): String = ReaderUtils.getReportPostUrl(blogUrl)

    fun getReportUserUrl(blogUrl: String, userId: Long): String = ReaderUtils.getReportUserUrl(blogUrl, userId)

    fun postAndCommentExists(blogId: Long, postId: Long, commentId: Long): Boolean {
        return ReaderUtils.postAndCommentExists(blogId, postId, commentId)
    }

    fun postExists(blogId: Long, postId: Long) = ReaderUtils.postExists(blogId, postId)

    fun commentExists(
        blogId: Long,
        postId: Long,
        commentId: Long
    ) = ReaderUtils.commentExists(blogId, postId, commentId)

    fun getTextForCommentSnippet(numComments: Int): String? = ReaderUtils.getTextForCommentSnippet(
        contextProvider.getContext(),
        numComments
    )

    fun isSelfHosted(authorBlogId: Long) = ReaderUtils.isSelfHosted(authorBlogId)

    fun getTagFromTagUrl(url: String): String = ReaderUtils.getTagFromTagUrl(url)

    fun getShortLikeLabelText(numLikes: Int): String =
        ReaderUtils.getShortLikeLabelText(contextProvider.getContext(), numLikes)

    fun getShortCommentLabelText(numComments: Int): String =
        ReaderUtils.getShortCommentLabelText(contextProvider.getContext(), numComments)
}
