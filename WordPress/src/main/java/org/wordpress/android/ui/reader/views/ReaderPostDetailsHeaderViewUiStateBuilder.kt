package org.wordpress.android.ui.reader.views

import dagger.Reusable
import org.wordpress.android.R
import org.wordpress.android.datasets.ReaderBlogTableWrapper
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.discover.ReaderPostTagsUiStateBuilder
import org.wordpress.android.ui.reader.discover.ReaderPostUiStateBuilder
import org.wordpress.android.ui.reader.utils.FeaturedImageUtils
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.reader.views.uistates.FollowButtonUiState
import org.wordpress.android.ui.reader.views.uistates.InteractionSectionUiState
import org.wordpress.android.ui.reader.views.uistates.ReaderPostDetailsHeaderAction
import org.wordpress.android.ui.reader.views.uistates.ReaderFeaturedImageUiState
import org.wordpress.android.ui.reader.views.uistates.ReaderPostDetailsHeaderUiState
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.DisplayUtilsWrapper
import org.wordpress.android.util.HtmlUtils
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import kotlin.math.ceil

private const val WORDS_PER_MINUTE = 200

@Reusable
class ReaderPostDetailsHeaderViewUiStateBuilder @Inject constructor(
    private val postUiStateBuilder: ReaderPostUiStateBuilder,
    private val readerPostTagsUiStateBuilder: ReaderPostTagsUiStateBuilder,
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper,
    private val featuredImageUtils: FeaturedImageUtils,
    private val readerUtilsWrapper: ReaderUtilsWrapper,
    private val displayUtilsWrapper: DisplayUtilsWrapper,
    private val readerBlogTableWrapper: ReaderBlogTableWrapper,
) {
    fun mapPostToUiState(
        post: ReaderPost,
        onHeaderAction: (ReaderPostDetailsHeaderAction) -> Unit,
    ): ReaderPostDetailsHeaderUiState {
        val textTitle = post
            .takeIf { post.hasTitle() }
            ?.title?.let { UiStringText(it) }

        return ReaderPostDetailsHeaderUiState(
            title = textTitle,
            authorName = post.authorName?.takeIf {
                it.isNotBlank() &&
                    !it.equals(post.blogName, ignoreCase = true)
            },
            tagItems = readerPostTagsUiStateBuilder
                .mapPostTagsToTagUiStates(post) {
                    onHeaderAction(
                        ReaderPostDetailsHeaderAction.TagItemClicked(it)
                    )
                },
            tagItemsVisibility = post.tags.isNotEmpty(),
            blogSectionUiState = postUiStateBuilder
                .mapPostToBlogSectionUiState(post) {
                    onHeaderAction(
                        ReaderPostDetailsHeaderAction.BlogSectionClicked
                    )
                },
            followButtonUiState = FollowButtonUiState(
                onFollowButtonClicked = {
                    onHeaderAction(
                        ReaderPostDetailsHeaderAction.FollowClicked
                    )
                },
                isFollowed = post.isFollowedByCurrentUser,
                isVisible = true
            ),
            dateLine = buildDateLine(post),
            readingTime = buildReadingTime(post),
            excerpt = buildExcerpt(post),
            featuredImageUiState = buildFeaturedImageUiState(
                post,
                onFeaturedImageClicked = { blogId, url ->
                    onHeaderAction(
                        ReaderPostDetailsHeaderAction.FeaturedImageClicked(
                            blogId, url
                        )
                    )
                }
            ),
            interactionSectionUiState = InteractionSectionUiState(
                likeCount = post.numLikes,
                commentCount = post.numReplies,
                onLikesClicked = {
                    onHeaderAction(
                        ReaderPostDetailsHeaderAction.LikesClicked
                    )
                },
                onCommentsClicked = {
                    onHeaderAction(
                        ReaderPostDetailsHeaderAction.CommentsClicked
                    )
                }
            )
        )
    }

    private fun buildDateLine(post: ReaderPost): String {
        val date = post.getDisplayDate(dateTimeUtilsWrapper) ?: return ""
        return SimpleDateFormat(
            DATE_FORMAT_PATTERN, Locale.getDefault()
        ).format(date)
    }

    /**
     * Estimates reading time by stripping HTML tags and img elements
     * from the post content, counting words, and dividing by
     * [WORDS_PER_MINUTE] (rounded up, minimum 1 minute).
     * Returns null for excerpt-only posts or empty content.
     */
    private fun buildReadingTime(post: ReaderPost): UiString? {
        val text = post.takeUnless { it.shouldShowExcerpt() }
            ?.text
            ?.takeIf { it.isNotBlank() }
            ?.let {
                HtmlUtils.fastStripHtml(
                    it.replace(IMG_TAG_REGEX, "")
                ).trim()
            }
            ?.takeIf { it.isNotBlank() }
            ?: return null
        val wordCount = text.split(WHITESPACE_REGEX).size
        val minutes = ceil(wordCount.toDouble() / WORDS_PER_MINUTE)
            .toInt()
            .coerceAtLeast(1)
        return UiStringResWithParams(
            R.string.reader_reading_time,
            listOf(UiStringText(minutes.toString()))
        )
    }

    private fun buildExcerpt(post: ReaderPost): UiString? {
        val description = readerBlogTableWrapper
            .getBlogInfo(post.blogId)
            ?.description
            ?.takeIf { it.isNotBlank() }
            ?: return null
        return UiStringText(description)
    }

    private fun buildFeaturedImageUiState(
        post: ReaderPost,
        onFeaturedImageClicked: (Long, String) -> Unit,
    ): ReaderFeaturedImageUiState? {
        if (!featuredImageUtils.shouldAddFeaturedImage(post)) return null
        val url = readerUtilsWrapper.getResizedImageUrl(
            post.featuredImage,
            displayUtilsWrapper.getDisplayPixelWidth(),
            0,
            post.isPrivate,
            post.isPrivateAtomic
        )
        return ReaderFeaturedImageUiState(
            blogId = post.blogId,
            url = url,
            onFeaturedImageClicked = onFeaturedImageClicked,
        )
    }

    companion object {
        private val IMG_TAG_REGEX = Regex("<img[^>]*>")
        private val WHITESPACE_REGEX = "\\s+".toRegex()
        private const val DATE_FORMAT_PATTERN =
            "MMM d, yyyy 'at' h:mm a"
    }
}
