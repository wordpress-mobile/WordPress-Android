package org.wordpress.android.ui.reader

import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType

object ReaderTestUtils {
    fun createTag(
        slug: String,
        type: ReaderTagType = ReaderTagType.FOLLOWED,
    ): ReaderTag = ReaderTag(
        slug,
        slug,
        slug,
        "endpoint/$slug",
        type,
    )
}
