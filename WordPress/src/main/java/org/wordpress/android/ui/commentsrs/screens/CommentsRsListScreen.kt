package org.wordpress.android.ui.commentsrs.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.commentsrs.CommentsRsBatchAction
import org.wordpress.android.ui.commentsrs.CommentsRsListTab
import org.wordpress.android.ui.commentsrs.CommentsTabUiState
import org.wordpress.android.ui.commentsrs.PendingConfirmation
import org.wordpress.android.ui.commentsrs.batchActions
import org.wordpress.android.ui.postsrs.SnackbarMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsRsListScreen(
    tabStates: Map<CommentsRsListTab, CommentsTabUiState>,
    selectedIds: Set<Long>,
    pendingConfirmation: PendingConfirmation?,
    onDismissConfirmation: () -> Unit,
    snackbarMessages: Flow<SnackbarMessage>,
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
    val activeTab = tabs[pagerState.settledPage]
    val snackbarHostState = remember { SnackbarHostState() }
    val isSelectionActive = selectedIds.isNotEmpty()

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
            if (isSelectionActive) {
                SelectionTopBar(
                    selectedCount = selectedIds.size,
                    actions = activeTab.batchActions(),
                    onClearSelection = onClearSelection,
                    onBatchAction = { action -> onBatchAction(action, activeTab) }
                )
            } else {
                TopAppBar(
                    title = { Text(text = stringResource(R.string.comments)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                )
            }
        }
    ) { contentPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            PrimaryScrollableTabRow(
                selectedTabIndex = pagerState.settledPage,
                edgePadding = 0.dp
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.settledPage == index,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
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
                val tabState = tabStates[tab] ?: CommentsTabUiState(isLoading = true)

                CommentsRsTabListScreen(
                    state = tabState,
                    emptyMessageResId = tab.emptyMessageResId,
                    selectedIds = selectedIds,
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
    when (pending) {
        is PendingConfirmation.Trash -> ConfirmationDialog(
            titleResId = R.string.trash,
            message = stringResource(R.string.dlg_confirm_trash_comments),
            confirmTextResId = R.string.dlg_confirm_action_trash,
            isDestructive = true,
            onConfirm = { onConfirm(activeTab) },
            onDismiss = onDismiss
        )
        is PendingConfirmation.Delete -> ConfirmationDialog(
            titleResId = R.string.delete,
            message = stringResource(
                if (pending.commentIds.size > 1) {
                    R.string.dlg_sure_to_delete_comments
                } else {
                    R.string.dlg_sure_to_delete_comment
                }
            ),
            confirmTextResId = R.string.delete,
            isDestructive = true,
            onConfirm = { onConfirm(activeTab) },
            onDismiss = onDismiss
        )
        null -> {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(
    selectedCount: Int,
    actions: List<CommentsRsBatchAction>,
    onClearSelection: () -> Unit,
    onBatchAction: (CommentsRsBatchAction) -> Unit
) {
    TopAppBar(
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
            actions.forEach { action ->
                IconButton(onClick = { onBatchAction(action) }) {
                    Icon(
                        painter = painterResource(action.iconResId),
                        contentDescription = stringResource(action.labelResId),
                        tint = if (action.isDestructive) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    )
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
