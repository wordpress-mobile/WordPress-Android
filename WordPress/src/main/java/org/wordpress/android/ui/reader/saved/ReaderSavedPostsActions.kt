package org.wordpress.android.ui.reader.saved

import org.wordpress.android.ui.reader.viewmodels.ReaderPostListEmptyButton

/** What [ReaderSavedPostsScreen] asks of its host. */
class ReaderSavedPostsActions(
    val onRefresh: () -> Unit,
    val onLoadMore: () -> Unit,
    val onEmptyStateButtonClicked: (ReaderPostListEmptyButton) -> Unit,
)
