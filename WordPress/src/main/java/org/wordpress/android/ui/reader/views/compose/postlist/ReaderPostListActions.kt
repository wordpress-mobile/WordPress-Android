package org.wordpress.android.ui.reader.views.compose.postlist

import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListEmptyButton

/**
 * The Reader post list's callbacks, implemented by ReaderPostListViewModel and ReaderPostListFragment. Those with
 * defaults are for rows only the post list has, so other lists shown by the gallery can leave them out.
 */
@Suppress("LongParameterList")
class ReaderPostListActions(
    val onRefresh: () -> Unit,
    val onLoadMore: () -> Unit,
    val onEmptyStateButtonClicked: (ReaderPostListEmptyButton) -> Unit,
    val onScrollHandled: () -> Unit = {},
    val onFirstItemVisibilityChanged: (Boolean) -> Unit = {},
    val onNewPostsBarClicked: () -> Unit = {},
    val onNewPostsBarScrolledAway: () -> Unit = {},
    val onGapMarkerClicked: () -> Unit = {},
    val onTagHeaderFollowClicked: () -> Unit = {},
    val onSiteHeaderFollowClicked: () -> Unit = {},
    val onSiteHeaderSettingsClicked: () -> Unit = {},
    val onXPostClicked: (ReaderPost) -> Unit = {},
    val onRemovedPostClicked: (ReaderPost) -> Unit = {}
)
