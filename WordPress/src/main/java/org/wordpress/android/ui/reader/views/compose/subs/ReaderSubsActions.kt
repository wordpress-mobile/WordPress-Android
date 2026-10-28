package org.wordpress.android.ui.reader.views.compose.subs

import org.wordpress.android.ui.reader.viewmodels.ReaderSubsSite
import org.wordpress.android.ui.reader.viewmodels.ReaderSubsTab
import org.wordpress.android.ui.reader.viewmodels.ReaderSubsTag

/** The Manage subscriptions screen's callbacks, implemented by ReaderSubsViewModel and ReaderSubsActivity. */
@Suppress("LongParameterList")
class ReaderSubsActions(
    val onBack: () -> Unit,
    val onTabSelected: (ReaderSubsTab) -> Unit,
    val onSiteSearchOpened: () -> Unit,
    val onSiteSearchQueryChanged: (String) -> Unit,
    val onSiteSearchClosed: () -> Unit,
    val onTagFollowClicked: (ReaderSubsTag) -> Unit,
    val onSiteClicked: (ReaderSubsSite) -> Unit,
    val onSiteFollowClicked: (ReaderSubsSite) -> Unit,
    val onEntryChanged: (String) -> Unit,
    val onAddClicked: () -> Unit,
    val onShowInterests: () -> Unit
)
