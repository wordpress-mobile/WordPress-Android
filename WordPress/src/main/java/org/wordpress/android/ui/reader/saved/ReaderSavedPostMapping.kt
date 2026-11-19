package org.wordpress.android.ui.reader.saved

import androidx.core.net.toUri
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.HtmlUtils
import uniffi.wp_api.ReaderPostAuthor
import uniffi.wp_api.ReaderPostSaveIdentity
import uniffi.wp_api.ReaderPostTag
import uniffi.wp_api.ReaderSavedKey
import uniffi.wp_mobile.ReaderLocalSave
import uniffi.wp_mobile.ReaderSavedListItem
import uniffi.wp_mobile_cache.ReaderPostRecord

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
    val identity = post?.identity
    val url = identity?.url ?: (key as? ReaderSavedKey.Url)?.url
    card.postId = identity?.localPostId ?: 0L
    card.blogId = identity?.localBlogId ?: 0L
    card.feedId = identity?.feedId?.toLong() ?: 0L
    card.feedItemId = identity?.feedItemId?.toLong() ?: 0L
    card.isExternal = identity?.isExternal == true
    card.url = url
    // what a page saved by URL shows, and what the post's own details replace
    card.title = url
    card.blogName = url?.let { it.toUri().host ?: it }
    post?.let { card.fillFrom(it) }
    // Always set: the card reads it, and a post that never had it set crashes the card. A save without the
    // post's date, such as one wordpress-rs hasn't fetched yet, shows when it was saved instead.
    card.setDatePublished((post?.date ?: dateSaved)?.let { DateTimeUtils.iso8601UTCFromDate(it) })
    // the saved post doesn't say whether comments are open, so the card only offers them when there are some
    card.isCommentsOpen = false
    card.isBookmarked = true
}

/** The record's text as the Reader API's posts have it in [ReaderPost]: without HTML, and entities decoded. */
private fun ReaderPost.fillFrom(record: ReaderPostRecord) {
    // a post without a title keeps the address it was saved under
    record.title?.plainText()?.ifBlank { null }?.let { title = it }
    excerpt = record.excerpt?.plainText()
    featuredImage = record.displayImageUrl?.ifBlank { null } ?: record.featuredImage
    record.siteName?.plainText()?.ifBlank { null }?.let { blogName = it }
    blogUrl = record.siteUrl
    blogImageUrl = record.siteIconUrl
    authorName = record.authorName?.plainText()
    postAvatar = record.authorAvatarUrl
    numLikes = record.likeCount?.toInt() ?: 0
    numReplies = record.commentCount?.toInt() ?: 0
    isLikedByCurrentUser = record.iLike == true
    isFollowedByCurrentUser = record.isFollowing == true
}

private fun String?.nonEmpty(): String? = this?.takeIf { it.isNotEmpty() }

private fun String.plainText(): String = HtmlUtils.fastStripHtml(this).trim()
