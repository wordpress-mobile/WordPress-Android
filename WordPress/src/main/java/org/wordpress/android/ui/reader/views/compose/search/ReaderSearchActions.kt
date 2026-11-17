package org.wordpress.android.ui.reader.views.compose.search

import org.wordpress.android.ui.reader.viewmodels.ReaderSearchSite
import org.wordpress.android.ui.reader.viewmodels.ReaderSearchTab

/** The Reader search screen's callbacks, implemented by ReaderSearchViewModel and ReaderSearchActivity. */
@Suppress("LongParameterList")
class ReaderSearchActions(
    val onBack: () -> Unit,
    val onQueryChanged: (String) -> Unit,
    val onSearch: () -> Unit,
    val onSuggestionClicked: (String) -> Unit,
    val onSuggestionDeleteClicked: (String) -> Unit,
    val onClearHistoryConfirmed: () -> Unit,
    val onTabSelected: (ReaderSearchTab) -> Unit,
    val onPostsTabReselected: () -> Unit,
    val onSiteClicked: (ReaderSearchSite) -> Unit,
    val onSiteFollowClicked: (ReaderSearchSite) -> Unit,
    val onLoadMoreSites: () -> Unit
)
