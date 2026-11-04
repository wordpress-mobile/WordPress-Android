package org.wordpress.android.ui.reader.views.compose.postlist

import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListEmptyButton

/** The Reader post list's callbacks, implemented by ReaderPostListViewModel and ReaderPostListFragment. */
@Suppress("LongParameterList")
class ReaderPostListActions(
    val onRefresh: () -> Unit,
    val onLoadMore: () -> Unit,
    val onScrollHandled: () -> Unit,
    val onFirstItemVisibilityChanged: (Boolean) -> Unit,
    val onNewPostsBarClicked: () -> Unit,
    val onNewPostsBarScrolledAway: () -> Unit,
    val onEmptyStateButtonClicked: (ReaderPostListEmptyButton) -> Unit,
    val onGapMarkerClicked: () -> Unit,
    val onTagHeaderFollowClicked: () -> Unit,
    val onSiteHeaderFollowClicked: () -> Unit,
    val onSiteHeaderSettingsClicked: () -> Unit,
    val onXPostClicked: (ReaderPost) -> Unit,
    val onRemovedPostClicked: (ReaderPost) -> Unit
)
