package org.wordpress.android.ui.reader.utils

import android.content.Context
import dagger.Reusable
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.ui.reader.services.update.TagUpdateClientUtilsProvider
import javax.inject.Inject

/**
 * Injectable wrapper around ReaderUtils.
 *
 * ReaderUtils interface is consisted of static methods, which make the client code difficult to test/mock. Main purpose of
 * this wrapper is to make testing easier.
 *
 */
@Reusable
class ReaderUtilsWrapper @Inject constructor(
    private val appContext: Context,
    private val tagUpdateClientUtilsProvider: TagUpdateClientUtilsProvider
) {
    fun getResizedImageUrl(imageUrl: String?, width: Int, height: Int, isPrivate: Boolean, isAtomic: Boolean): String? =
            ReaderUtils.getResizedImageUrl(imageUrl, width, height, isPrivate, isAtomic)

    fun getTagFromTagName(tagName: String, tagType: ReaderTagType): ReaderTag =
            ReaderUtils.getTagFromTagName(tagName, tagType)

    fun getDefaultTagFromDbOrCreateInMemory() =
            ReaderUtils.getDefaultTagFromDbOrCreateInMemory(appContext, tagUpdateClientUtilsProvider)

    fun getLongLikeLabelText(numLikes: Int, isLikedByCurrentUser: Boolean): String =
            ReaderUtils.getLongLikeLabelText(appContext, numLikes, isLikedByCurrentUser)
}
