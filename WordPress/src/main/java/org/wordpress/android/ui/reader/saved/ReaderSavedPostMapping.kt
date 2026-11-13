package org.wordpress.android.ui.reader.saved

import org.wordpress.android.models.ReaderPost
import org.wordpress.android.util.DateTimeUtils
import uniffi.wp_api.ReaderPostAuthor
import uniffi.wp_api.ReaderPostSaveIdentity
import uniffi.wp_api.ReaderPostTag
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
    url = url.nonEmpty()
)

/** The blog ID a [ReaderPost] with this identity has in the local database. */
val ReaderPostSaveIdentity.localBlogId: Long
    get() = siteId?.toLong() ?: 0L

/** The post ID a [ReaderPost] with this identity has in the local database. */
val ReaderPostSaveIdentity.localPostId: Long
    get() = id.toLong()

fun ReaderPost.toLocalSave() = ReaderLocalSave(
    identity = toSaveIdentity(),
    dateSaved = dateTagged.nonEmpty()?.let { DateTimeUtils.dateFromIso8601(it) },
    title = title.nonEmpty(),
    excerpt = excerpt.nonEmpty(),
    featuredImage = featuredImage.nonEmpty(),
    siteName = blogName.nonEmpty(),
    siteUrl = blogUrl.nonEmpty()
)

/**
 * The post's details as wordpress-rs stores them with a save, so the Saved list can show the post
 * straight away rather than an empty row until the next refresh. They only fill in what the cache
 * doesn't already have, and the saved list's own copy replaces them on the next refresh.
 */
fun ReaderPost.toSavedPostDetails() = uniffi.wp_api.ReaderPost(
    identity = toSaveIdentity(),
    pseudoId = pseudoId.nonEmpty(),
    shortUrl = shortUrl.nonEmpty(),
    title = title.nonEmpty(),
    excerpt = excerpt.nonEmpty(),
    date = datePublished.nonEmpty()?.let { DateTimeUtils.dateFromIso8601(it) },
    author = ReaderPostAuthor(
        id = authorId.takeIf { it > 0 }?.toULong(),
        name = authorName.nonEmpty(),
        firstName = authorFirstName.nonEmpty(),
        avatarUrl = postAvatar.nonEmpty()
    ),
    siteName = blogName.nonEmpty(),
    siteUrl = blogUrl.nonEmpty(),
    siteIconUrl = blogImageUrl.nonEmpty(),
    featuredImage = featuredImage.nonEmpty(),
    // the image the Reader card shows for the post
    displayImageUrl = featuredImage.nonEmpty(),
    tags = tags.orEmpty().map { ReaderPostTag(slug = it.tagSlug, name = it.tagDisplayName.ifEmpty { it.tagSlug }) },
    likeCount = numLikes.toULong(),
    iLike = isLikedByCurrentUser,
    isFollowing = isFollowedByCurrentUser,
    commentCount = numReplies.toULong()
)

private fun String?.nonEmpty(): String? = this?.takeIf { it.isNotEmpty() }
