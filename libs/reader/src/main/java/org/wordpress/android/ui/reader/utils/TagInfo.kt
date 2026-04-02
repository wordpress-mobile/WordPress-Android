package org.wordpress.android.ui.reader.utils

import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType

class TagInfo(
    val tagType: ReaderTagType,
    private val endPoint: String
) {
    fun isDesiredTag(tag: ReaderTag): Boolean {
        return tag.tagType == tagType && (endPoint.isEmpty() || tag.endpoint.endsWith(endPoint))
    }
}
