package org.wordpress.android.ui.reader.saved

import org.wordpress.android.R
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.PrimaryAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType
import org.wordpress.android.ui.reader.discover.ReaderPostUiStateBuilder
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.utils.UiString.UiStringRes
import uniffi.wp_mobile.ReaderSavedListItem
import javax.inject.Inject

/**
 * Builds a saved post's card with the same builder as the Reader's other post lists, so it looks the same. A
 * saved post often isn't in the local database, which the card's other actions need, so it offers only save,
 * which removes it, and comments.
 */
class ReaderSavedPostCardBuilder @Inject constructor(
    private val postUiStateBuilder: ReaderPostUiStateBuilder,
) {
    class Callbacks(
        val onClicked: (ReaderSavedListItem) -> Unit,
        val onUnsaveClicked: (ReaderSavedListItem) -> Unit,
        val onCommentsClicked: (ReaderSavedListItem) -> Unit,
        val onSiteClicked: (ReaderSavedListItem) -> Unit,
    )

    /** Reads and formats the post, so call it off the main thread. */
    fun build(
        item: ReaderSavedListItem,
        photonWidth: Int,
        photonHeight: Int,
        callbacks: Callbacks,
    ): ReaderPostUiState {
        val post = item.toCardPost()
        val card = postUiStateBuilder.mapPostToUiStateBlocking(
            source = ReaderTracker.SOURCE_SAVED,
            post = post,
            photonWidth = photonWidth,
            photonHeight = photonHeight,
            postListType = ReaderPostListType.TAG_FOLLOWED,
            onButtonClicked = { _, _, type ->
                if (type == ReaderPostCardActionType.COMMENTS) callbacks.onCommentsClicked(item)
            },
            onItemClicked = { _, _ -> callbacks.onClicked(item) },
            onItemRendered = {},
            onMoreButtonClicked = {},
            onMoreDismissed = {},
            onPostHeaderViewClicked = { _, _ -> callbacks.onSiteClicked(item) },
        )
        val blogSection = card.blogSection.copy(
            // a page saved by URL has no site to open
            onClicked = card.blogSection.onClicked.takeIf { post.blogId != 0L || post.feedId != 0L },
        )
        return card.copy(
            blogSection = blogSection,
            moreMenuVisibility = false,
            likeAction = card.likeAction.copy(isEnabled = false),
            reblogAction = card.reblogAction.copy(isEnabled = false),
            // rebuilt rather than routed like comments: the shared builder disables it for a post without IDs,
            // such as a page saved by URL, which still needs a way to be removed
            bookmarkAction = PrimaryAction(
                isEnabled = true,
                isSelected = true,
                contentDescription = UiStringRes(R.string.reader_remove_bookmark),
                type = ReaderPostCardActionType.BOOKMARK,
                onClicked = { _, _, _ -> callbacks.onUnsaveClicked(item) },
            ),
        )
    }
}
