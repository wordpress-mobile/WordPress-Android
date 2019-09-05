package org.wordpress.android.ui.reader.utils

import dagger.Reusable
import javax.inject.Inject

/**
 * Injectable wrapper around ReaderUtils.
 *
 * ReaderUtils interface is consisted of static methods, which make the client code difficult to test/mock. Main purpose of
 * this wrapper is to make testing easier.
 *
 */
@Reusable
class ReaderUtilsWrapper @Inject constructor() {
    fun getResizedImageUrl(imageUrl: String?, width: Int, height: Int, isPrivate: Boolean): String? =
            ReaderUtils.getResizedImageUrl(imageUrl, width, height, isPrivate)
}
