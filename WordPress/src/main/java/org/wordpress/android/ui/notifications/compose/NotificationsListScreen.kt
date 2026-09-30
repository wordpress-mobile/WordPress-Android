package org.wordpress.android.ui.notifications.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.commentsrs.screens.CommentsRsPlaceholderRow
import org.wordpress.android.ui.notifications.NotificationsListFragment.Companion.TabPosition
import org.wordpress.android.ui.notifications.adapters.Filter
import org.wordpress.android.ui.rs.contentlist.ContentDateGroup
import org.wordpress.android.ui.rs.contentlist.ContentDateGrouper
import org.wordpress.android.ui.rs.contentlist.ContentListDefaults
import org.wordpress.android.ui.rs.contentlist.ContentListEmptyState
import org.wordpress.android.ui.rs.contentlist.ContentListGroupHeader
import org.wordpress.android.ui.rs.contentlist.ContentListMenuAction
import org.wordpress.android.ui.rs.contentlist.ContentListOverflowMenu
import org.wordpress.android.ui.rs.contentlist.ContentListPullToRefreshBox
import org.wordpress.android.ui.rs.contentlist.ContentListShimmer
import org.wordpress.android.ui.rs.contentlist.ContentListTabRow
import org.wordpress.android.ui.rs.contentlist.ReportSettledTab

@Suppress("LongParameterList", "LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsListScreen(
    uiState: NotificationsListUiState,
    showPermissionWarning: Boolean,
    isReaderEnabled: Boolean,
    hasSelectedSite: Boolean,
    scrollToTopRequests: Flow<Unit>,
    onPermissionWarningClick: () -> Unit,
    onPermissionWarningDismiss: () -> Unit,
    onMenuOpened: () -> Unit,
    onMarkAllRead: (Filter) -> Unit,
    onSettingsClick: () -> Unit,
    onTabChanged: (Filter) -> Unit,
    onRefresh: () -> Unit,
    onNoteClick: (String, Filter) -> Unit,
    onInlineAction: (String, NotificationInlineAction) -> Unit,
    onEmptyAction: (Filter) -> Unit,
    onListScrolled: () -> Unit,
    onNewNotificationsTapped: () -> Unit
) {
    val tabs = TabPosition.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    // Listed explicitly so each is saveable; keep in sync with TabPosition.
    val listStates = listOf(
        rememberLazyListState(),
        rememberLazyListState(),
        rememberLazyListState(),
        rememberLazyListState(),
        rememberLazyListState()
    )
    val activeListState by rememberUpdatedState(listStates[pagerState.settledPage])

    ReportSettledTab(
        pagerState = pagerState,
        onTabSettled = {},
        onTabChanged = { onTabChanged(tabs[it].filter) }
    )

    LaunchedEffect(scrollToTopRequests) {
        scrollToTopRequests.collect { activeListState.animateScrollToItem(0) }
    }

    Scaffold(
        containerColor = ContentListDefaults.containerColor(isRedesignEnabled = true),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.notifications_screen_title)) },
                actions = {
                    ContentListOverflowMenu(
                        actions = listOf(
                            ContentListMenuAction(
                                labelResId = R.string.notifications_action_mark_all_as_read,
                                iconResId = R.drawable.ic_checkmark_white_24dp,
                                isDestructive = false,
                                onClick = { onMarkAllRead(tabs[pagerState.settledPage].filter) }
                            ),
                            ContentListMenuAction(
                                labelResId = R.string.notification_settings,
                                iconResId = R.drawable.ic_cog_white_24dp,
                                isDestructive = false,
                                onClick = onSettingsClick
                            )
                        ),
                        onExpand = onMenuOpened
                    )
                }
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            ContentListTabRow(
                labels = tabs.map { stringResource(it.titleRes) },
                selectedIndex = pagerState.settledPage,
                isRedesignEnabled = true,
                onSelect = { index ->
                    coroutineScope.launch {
                        if (pagerState.settledPage == index) {
                            listStates[index].animateScrollToItem(0)
                        } else {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                }
            )
            if (showPermissionWarning) {
                PermissionWarning(onClick = onPermissionWarningClick, onDismiss = onPermissionWarningDismiss)
            }
            Box(modifier = Modifier.weight(1f)) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    val filter = tabs[page].filter
                    NotificationsTab(
                        rows = uiState.rowsByFilter[filter],
                        isLoading = uiState.isLoading,
                        isRefreshing = uiState.isRefreshing,
                        emptyCopy = filter.emptyCopy(isReaderEnabled, hasSelectedSite),
                        listState = listStates[page],
                        onRefresh = onRefresh,
                        onNoteClick = { noteId -> onNoteClick(noteId, filter) },
                        onInlineAction = onInlineAction,
                        onEmptyAction = { onEmptyAction(filter) },
                        onListScrolled = onListScrolled
                    )
                }
                val isScrolledDown by remember {
                    derivedStateOf { activeListState.firstVisibleItemIndex > 0 }
                }
                if (uiState.hasNewNotifications && isScrolledDown) {
                    NewNotificationsBar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = NEW_BAR_BOTTOM_PADDING),
                        onClick = {
                            onNewNotificationsTapped()
                            coroutineScope.launch { activeListState.animateScrollToItem(0) }
                        }
                    )
                }
            }
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun NotificationsTab(
    rows: List<NotificationRowUiModel>?,
    isLoading: Boolean,
    isRefreshing: Boolean,
    emptyCopy: EmptyCopy,
    listState: LazyListState,
    onRefresh: () -> Unit,
    onNoteClick: (String) -> Unit,
    onInlineAction: (String, NotificationInlineAction) -> Unit,
    onEmptyAction: () -> Unit,
    onListScrolled: () -> Unit
) {
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { it }
            .collect { onListScrolled() }
    }

    ContentListPullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh) {
        when {
            isLoading || (rows.isNullOrEmpty() && isRefreshing) -> ContentListShimmer { CommentsRsPlaceholderRow() }
            rows.isNullOrEmpty() -> ContentListEmptyState(
                messageResId = emptyCopy.titleResId,
                detailResId = emptyCopy.detailResId,
                actionLabelResId = emptyCopy.actionResId,
                onAction = emptyCopy.actionResId?.let { onEmptyAction }
            )
            else -> {
                val items = remember(rows) { withDateGroups(rows) }
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(items = items, key = { it.key }, contentType = { it::class }) { item ->
                        when (item) {
                            is NotificationListItem.Header -> ContentListGroupHeader(
                                group = item.group,
                                modifier = Modifier.animateItem()
                            )
                            is NotificationListItem.Row -> NotificationRow(
                                row = item.row,
                                onClick = { onNoteClick(item.row.noteId) },
                                onInlineAction = { action -> onInlineAction(item.row.noteId, action) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewNotificationsBar(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(NEW_BAR_RADIUS),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = NEW_BAR_ELEVATION
    ) {
        Column(
            modifier = Modifier.padding(horizontal = NEW_BAR_H_PADDING, vertical = NEW_BAR_V_PADDING),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.notifications_label_new_notifications),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(R.string.notifications_label_new_notifications_subtitle),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun PermissionWarning(onClick: () -> Unit, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.warning_0))
            .clickable(onClick = onClick)
            .padding(start = WARNING_START_PADDING),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.notifications_permission_off_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Black,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onClick) {
            Text(
                text = stringResource(R.string.notifications_permission_fix),
                color = colorResource(R.color.warning_60)
            )
        }
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.notifications_permission_dismiss_content_description),
                tint = Color.Black
            )
        }
    }
}

private sealed interface NotificationListItem {
    val key: String

    data class Header(val group: ContentDateGroup, override val key: String) : NotificationListItem
    data class Row(val row: NotificationRowUiModel, override val key: String) : NotificationListItem
}

/** Keys are deduped: a note can lack an id, and a duplicate LazyColumn key crashes. */
private fun withDateGroups(rows: List<NotificationRowUiModel>): List<NotificationListItem> {
    val items = ArrayList<NotificationListItem>(rows.size + 1)
    val usedKeys = HashSet<String>()
    var currentGroupKey: String? = null
    rows.forEachIndexed { index, row ->
        val group = ContentDateGrouper.groupOf(row.timestampMillis)
        if (group.key != currentGroupKey) {
            items.add(NotificationListItem.Header(group, "header_${index}_${group.key}"))
            currentGroupKey = group.key
        }
        val key = "note_${row.noteId}".takeIf { row.noteId.isNotEmpty() && usedKeys.add(it) } ?: "note_index_$index"
        items.add(NotificationListItem.Row(row, key))
    }
    return items
}

private data class EmptyCopy(
    @StringRes val titleResId: Int,
    @StringRes val detailResId: Int? = null,
    @StringRes val actionResId: Int? = null
)

private fun Filter.emptyCopy(isReaderEnabled: Boolean, hasSelectedSite: Boolean): EmptyCopy {
    val copy = when (this) {
        Filter.ALL -> EmptyCopy(
            R.string.notifications_empty_all,
            R.string.notifications_empty_action_all,
            R.string.notifications_empty_view_reader
        )
        Filter.COMMENT -> EmptyCopy(
            R.string.notifications_empty_comments,
            R.string.notifications_empty_action_comments,
            R.string.notifications_empty_view_reader
        )
        Filter.FOLLOW -> EmptyCopy(
            R.string.notifications_empty_subscribers,
            R.string.notifications_empty_action_followers_likes,
            R.string.notifications_empty_view_reader
        )
        Filter.LIKE -> EmptyCopy(
            R.string.notifications_empty_likes,
            R.string.notifications_empty_action_followers_likes,
            R.string.notifications_empty_view_reader
        )
        Filter.UNREAD -> if (hasSelectedSite) {
            EmptyCopy(
                R.string.notifications_empty_unread,
                R.string.notifications_empty_action_unread,
                R.string.posts_empty_list_button
            )
        } else {
            EmptyCopy(R.string.notifications_empty_unread)
        }
    }
    return if (isReaderEnabled) copy else EmptyCopy(copy.titleResId)
}

private val NEW_BAR_BOTTOM_PADDING = 16.dp
private val NEW_BAR_RADIUS = 24.dp
private val NEW_BAR_ELEVATION = 4.dp
private val NEW_BAR_H_PADDING = 20.dp
private val NEW_BAR_V_PADDING = 10.dp
private val WARNING_START_PADDING = 16.dp
