package org.wordpress.android.ui.commentsrs.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.ui.commentsrs.CommentsRsBatchAction
import org.wordpress.android.ui.commentsrs.CommentsRsListTab
import org.wordpress.android.ui.commentsrs.CommentsTabUiState
import org.wordpress.android.ui.commentsrs.PendingConfirmation
import org.wordpress.android.ui.commentsrs.batchActions
import org.wordpress.android.ui.commentsrs.isEnabledFor
import org.wordpress.android.ui.postsrs.SnackbarMessage

// Material's disabled-content alpha, used to dim batch-action icons that can't apply to the
// current selection while keeping them visible.
private const val DISABLED_ICON_ALPHA = 0.38f

/** Which of the mutually exclusive top bars is showing: selection wins over search. */
private enum class TopBarMode { SELECTION, SEARCH, NORMAL }

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsRsListScreen(
    tabStates: Map<CommentsRsListTab, CommentsTabUiState>,
    selectedIds: Set<Long>,
    canModerate: Boolean,
    pendingConfirmation: PendingConfirmation?,
    isSearchActive: Boolean,
    searchQuery: String,
    isQuerySearchable: Boolean,
    onDismissConfirmation: () -> Unit,
    snackbarMessages: Flow<SnackbarMessage>,
    onSearchOpen: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSearchClose: (CommentsRsListTab) -> Unit,
    onInitTab: (CommentsRsListTab) -> Unit,
    onTabChanged: (CommentsRsListTab) -> Unit,
    onRefreshTab: (CommentsRsListTab) -> Unit,
    onLoadMore: (CommentsRsListTab) -> Unit,
    onNavigateBack: () -> Unit,
    onCommentClick: (Long) -> Unit,
    onCommentLongClick: (Long) -> Unit,
    onClearSelection: () -> Unit,
    onBatchAction: (CommentsRsBatchAction, CommentsRsListTab) -> Unit,
    onConfirmPendingAction: (CommentsRsListTab) -> Unit
) {
    val tabs = CommentsRsListTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    // Hoisted so a re-tap on the active tab can scroll its list back to the top. Listed
    // explicitly (rememberLazyListState can't be called in an associateWith lambda) so each
    // state is saveable and scroll positions survive rotation; keep in sync with the enum.
    val listStates = mapOf(
        CommentsRsListTab.ALL to rememberLazyListState(),
        CommentsRsListTab.PENDING to rememberLazyListState(),
        CommentsRsListTab.UNREPLIED to rememberLazyListState(),
        CommentsRsListTab.APPROVED to rememberLazyListState(),
        CommentsRsListTab.SPAM to rememberLazyListState(),
        CommentsRsListTab.TRASHED to rememberLazyListState()
    )
    val activeTab = tabs[pagerState.settledPage]
    val snackbarHostState = remember { SnackbarHostState() }
    // Statuses of the selected comments that live on the active tab. This is empty during a tab
    // swipe (the selection still belongs to the previous tab and is about to be cleared), so gating
    // the contextual bar on it keeps it from flashing the next tab's actions mid-transition.
    val selectedStatuses = tabStates[activeTab]?.comments
        .orEmpty()
        .filter { it.remoteCommentId in selectedIds }
        .map { it.status }
        .toSet()
    val isSelectionActive = selectedStatuses.isNotEmpty()
    val focusRequester = remember { FocusRequester() }
    // Focus (and the keyboard) is requested once per search open, not every time SearchTopBar
    // re-enters composition — a selection round-trip over active search must not pop the keyboard.
    var searchFocusPending by remember { mutableStateOf(false) }
    val topBarMode = when {
        isSelectionActive -> TopBarMode.SELECTION
        isSearchActive -> TopBarMode.SEARCH
        else -> TopBarMode.NORMAL
    }

    // Search opening, closing, or changing the trimmed query replaces each tab's content
    // wholesale; reset the scroll so page 1 renders at the top. The hoisted list states otherwise
    // keep their old deep offsets, clamping the short new list to its end and tripping the
    // load-more trigger. drop(1) skips the initial emission: an Activity recreation must not
    // discard the scroll positions the saveable list states just restored. The at-top check
    // avoids scrollToItem's forced remeasure (and fling cancellation) when there's nothing to do.
    val currentSearchActive by rememberUpdatedState(isSearchActive)
    val currentQuery by rememberUpdatedState(searchQuery)
    LaunchedEffect(Unit) {
        snapshotFlow { currentSearchActive to currentQuery.trim() }
            .drop(1)
            .collect {
                listStates.values.forEach { state ->
                    if (state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset > 0) {
                        state.scrollToItem(0)
                    }
                }
            }
    }

    // Like the legacy action mode: the first back press dismisses the selection, the next one
    // leaves the screen. Enabled on selectedIds (not selectedStatuses) so any live selection is
    // cleared, including during the brief tab-swipe window before it clears itself.
    BackHandler(enabled = selectedIds.isNotEmpty()) {
        onClearSelection()
    }
    // Similarly, a back press while searching closes the search before leaving the screen.
    BackHandler(enabled = isSearchActive && selectedIds.isEmpty()) {
        onSearchClose(activeTab)
    }

    LaunchedEffect(snackbarMessages) {
        snackbarMessages.collect { msg ->
            val result = snackbarHostState.showSnackbar(
                message = msg.message,
                actionLabel = msg.actionLabel
            )
            if (result == SnackbarResult.ActionPerformed) {
                msg.onAction?.invoke()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AnimatedContent(targetState = topBarMode, label = "topBar") { mode ->
                when (mode) {
                    TopBarMode.SELECTION -> SelectionTopBar(
                        selectedCount = selectedIds.size,
                        actions = activeTab.batchActions(),
                        selectedStatuses = selectedStatuses,
                        canModerate = canModerate,
                        onClearSelection = onClearSelection,
                        onBatchAction = { action -> onBatchAction(action, activeTab) }
                    )
                    TopBarMode.SEARCH -> {
                        SearchTopBar(
                            searchQuery = searchQuery,
                            focusRequester = focusRequester,
                            onQueryChanged = onSearchQueryChanged,
                            onClose = { onSearchClose(activeTab) }
                        )
                        // Runs after SearchTopBar is composed, so the requester is attached.
                        LaunchedEffect(searchFocusPending) {
                            if (searchFocusPending) {
                                focusRequester.requestFocus()
                                searchFocusPending = false
                            }
                        }
                    }
                    TopBarMode.NORMAL -> TopAppBar(
                        title = { Text(text = stringResource(R.string.comments)) },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back)
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                searchFocusPending = true
                                onSearchOpen()
                            }) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = stringResource(R.string.comments_rs_search_prompt)
                                )
                            }
                        }
                    )
                }
            }
        }
    ) { contentPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            // The tab row stays visible while searching: the comments endpoint only accepts a
            // single status per request (unlike posts, which search across all statuses), so a
            // search is always scoped to one tab — keeping the tabs on screen makes that scope
            // visible and lets the user re-run the query against another status.
            PrimaryScrollableTabRow(
                selectedTabIndex = pagerState.settledPage,
                edgePadding = 0.dp
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.settledPage == index,
                        onClick = {
                            coroutineScope.launch {
                                if (pagerState.settledPage == index) {
                                    // Re-tapping the active tab scrolls its list back to the top.
                                    listStates.getValue(tab).animateScrollToItem(0)
                                } else {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        },
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        text = { Text(text = stringResource(tab.labelResId)) }
                    )
                }
            }

            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.settledPage }.collect { page ->
                    onInitTab(tabs[page])
                    onTabChanged(tabs[page])
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val tab = tabs[page]

                CommentsRsTabListScreen(
                    state = tabStates[tab],
                    emptyMessageResId = tab.emptyMessageResId,
                    selectedIds = selectedIds,
                    listState = listStates.getValue(tab),
                    isSearchActive = isSearchActive,
                    isQuerySearchable = isQuerySearchable,
                    onRefresh = { onRefreshTab(tab) },
                    onLoadMore = { onLoadMore(tab) },
                    onCommentClick = onCommentClick,
                    onCommentLongClick = onCommentLongClick
                )
            }
        }
    }

    BatchConfirmationDialogs(
        pending = pendingConfirmation,
        activeTab = activeTab,
        onConfirm = onConfirmPendingAction,
        onDismiss = onDismissConfirmation
    )
}

@Composable
private fun BatchConfirmationDialogs(
    pending: PendingConfirmation?,
    activeTab: CommentsRsListTab,
    onConfirm: (CommentsRsListTab) -> Unit,
    onDismiss: () -> Unit
) {
    // Every action that reaches confirmation carries its copy (see onBatchAction), so any
    // destructive action renders a dialog rather than silently stranding the selection.
    val copy = pending?.action?.confirmation ?: return
    val messageResId = if (pending.commentIds.size > 1) copy.messagePluralResId else copy.messageResId
    ConfirmationDialog(
        titleResId = copy.titleResId,
        message = stringResource(messageResId),
        confirmTextResId = copy.confirmButtonResId,
        isDestructive = true,
        onConfirm = { onConfirm(activeTab) },
        onDismiss = onDismiss
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(
    selectedCount: Int,
    actions: List<CommentsRsBatchAction>,
    selectedStatuses: Set<CommentStatus>,
    canModerate: Boolean,
    onClearSelection: () -> Unit,
    onBatchAction: (CommentsRsBatchAction) -> Unit
) {
    // Like the legacy action mode, approve/unapprove sit in the bar as icons and everything else
    // falls into the overflow menu, keeping the selection count on a single line.
    val (iconActions, menuActions) = actions.partition { it.showAsIcon }
    TopAppBar(
        // A distinct container color so selection mode visibly reads as a mode change.
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        title = { Text(text = stringResource(R.string.cab_selected, selectedCount)) },
        navigationIcon = {
            IconButton(onClick = onClearSelection) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.clear)
                )
            }
        },
        actions = {
            iconActions.forEach { action ->
                // An action that would be a no-op for the selection (e.g. approve when nothing is
                // unapproved), or that the user lacks the capability to perform, stays visible but
                // disabled.
                val enabled = action.isEnabledFor(selectedStatuses, canModerate)
                IconButton(onClick = { onBatchAction(action) }, enabled = enabled) {
                    Icon(
                        painter = painterResource(action.iconResId),
                        contentDescription = stringResource(action.labelResId),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (enabled) 1f else DISABLED_ICON_ALPHA
                        )
                    )
                }
            }
            if (menuActions.isNotEmpty()) {
                BatchActionsOverflowMenu(
                    actions = menuActions,
                    selectedStatuses = selectedStatuses,
                    canModerate = canModerate,
                    onBatchAction = onBatchAction
                )
            }
        }
    )
}

/**
 * Search mode: an inline query field replaces the title, the navigation icon closes the search,
 * and a clear button empties a non-blank query (like the rs posts list search). The caller owns
 * the once-per-open focus request so re-entering composition — e.g. returning from the selection
 * top bar — doesn't pop the keyboard uninvited.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    searchQuery: String,
    focusRequester: FocusRequester,
    onQueryChanged: (String) -> Unit,
    onClose: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    TopAppBar(
        title = {
            TextField(
                value = searchQuery,
                onValueChange = onQueryChanged,
                placeholder = { Text(stringResource(R.string.comments_rs_search_prompt)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
        },
        actions = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onQueryChanged("") }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.clear)
                    )
                }
            }
        }
    )
}

@Composable
private fun BatchActionsOverflowMenu(
    actions: List<CommentsRsBatchAction>,
    selectedStatuses: Set<CommentStatus>,
    canModerate: Boolean,
    onBatchAction: (CommentsRsBatchAction) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        actions.forEach { action ->
            // Without moderation rights the items stay visible but disabled, matching the icon
            // actions in the bar and the detail screen's overflow.
            val enabled = action.isEnabledFor(selectedStatuses, canModerate)
            val baseColor = if (action.confirmation != null) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            val color = if (enabled) baseColor else baseColor.copy(alpha = DISABLED_ICON_ALPHA)
            DropdownMenuItem(
                text = { Text(text = stringResource(action.labelResId), color = color) },
                enabled = enabled,
                leadingIcon = {
                    Icon(
                        painter = painterResource(action.iconResId),
                        contentDescription = null,
                        tint = color
                    )
                },
                onClick = {
                    expanded = false
                    onBatchAction(action)
                }
            )
        }
    }
}

@Composable
private fun ConfirmationDialog(
    @StringRes titleResId: Int,
    message: String,
    @StringRes confirmTextResId: Int,
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleResId)) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(confirmTextResId),
                    color = if (isDestructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        Color.Unspecified
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
