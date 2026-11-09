package org.wordpress.android.ui.reader.saved

import org.wordpress.android.models.ReaderPost
import org.wordpress.android.util.DateTimeUtils
import uniffi.wp_api.ReaderPostSaveIdentity
import uniffi.wp_mobile.ReaderLocalSave

/**
 * The fields wordpress-rs needs to pick the key a post is saved under. [ReaderPost] keeps them as
 * the Reader API sent them (`ID`, `site_ID`, `feed_ID`, `feed_item_ID`, `is_external`, `URL`), which
 * is what [ReaderPostSaveIdentity] reads, so they map one to one.
 */
fun ReaderPost.toSaveIdentity() = ReaderPostSaveIdentity(
    id = postId.toULong(),
    siteId = blogId.takeIf { it > 0 }?.toULong(),
    feedId = feedId.takeIf { it > 0 }?.toULong(),
    feedItemId = feedItemId.takeIf { it > 0 }?.toULong(),
    isExternal = isExternal,
    url = url.takeIf { it.isNotEmpty() }
)

/** The blog ID a [ReaderPost] with this identity has in the local database. */
val ReaderPostSaveIdentity.localBlogId: Long
    get() = siteId?.toLong() ?: 0L

/** The post ID a [ReaderPost] with this identity has in the local database. */
val ReaderPostSaveIdentity.localPostId: Long
    get() = id.toLong()

fun ReaderPost.toLocalSave() = ReaderLocalSave(
    identity = toSaveIdentity(),
    dateSaved = dateTagged.takeIf { it.isNotEmpty() }?.let { DateTimeUtils.dateFromIso8601(it) },
    title = title.takeIf { it.isNotEmpty() },
    excerpt = excerpt.takeIf { it.isNotEmpty() },
    featuredImage = featuredImage.takeIf { it.isNotEmpty() },
    siteName = blogName.takeIf { it.isNotEmpty() },
    siteUrl = blogUrl.takeIf { it.isNotEmpty() }
)
