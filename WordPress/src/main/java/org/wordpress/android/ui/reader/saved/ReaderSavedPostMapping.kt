package org.wordpress.android.ui.reader.saved

import androidx.core.net.toUri
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.util.DateTimeUtils
import uniffi.wp_api.ReaderPostAuthor
import uniffi.wp_api.ReaderPostSaveIdentity
import uniffi.wp_api.ReaderPostTag
import uniffi.wp_api.ReaderSavedKey
import uniffi.wp_mobile.ReaderLocalSave
import uniffi.wp_mobile.ReaderSavedListItem

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

/**
 * The saved post as the Reader's post cards read it, built from what wordpress-rs keeps with the save, since a
 * post saved elsewhere often isn't in the local database. Only what a card shows is filled in. A page saved by
 * URL has nothing but its address, so its host stands in for the site and the address for the title.
 */
fun ReaderSavedListItem.toCardPost(): ReaderPost = ReaderPost().also { card ->
    val record = post
    val identity = record?.identity
    val url = identity?.url ?: (key as? ReaderSavedKey.Url)?.url
    val host = url?.let { it.toUri().host ?: it }
    card.postId = identity?.localPostId ?: 0L
    card.blogId = identity?.localBlogId ?: 0L
    card.feedId = identity?.feedId?.toLong() ?: 0L
    card.feedItemId = identity?.feedItemId?.toLong() ?: 0L
    card.isExternal = identity?.isExternal == true
    card.url = url
    card.title = if (record == null) url else record.title
    card.excerpt = record?.excerpt
    card.featuredImage = record?.displayImageUrl.nonBlank() ?: record?.featuredImage
    card.blogName = record?.siteName.nonBlank() ?: host
    card.blogUrl = record?.siteUrl
    card.blogImageUrl = record?.siteIconUrl
    card.authorName = record?.authorName
    card.postAvatar = record?.authorAvatarUrl
    record?.date?.let { card.setDatePublished(DateTimeUtils.iso8601UTCFromDate(it)) }
    card.numLikes = record?.likeCount?.toInt() ?: 0
    card.numReplies = record?.commentCount?.toInt() ?: 0
    card.isLikedByCurrentUser = record?.iLike == true
    card.isFollowedByCurrentUser = record?.isFollowing == true
    // the saved post doesn't say, and the card only offers comments for a WordPress.com post that has them open
    card.isCommentsOpen = !card.isExternal && card.blogId != 0L
    card.isBookmarked = true
}

private fun String?.nonEmpty(): String? = this?.takeIf { it.isNotEmpty() }

private fun String?.nonBlank(): String? = this?.takeIf { it.isNotBlank() }
