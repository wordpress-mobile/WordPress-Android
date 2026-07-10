package org.wordpress.android.ui.commentsrs

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.wordpress.android.ui.comments.unified.UnifiedCommentsDetailsActivity
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.commentsrs.screens.CommentsRsListScreen
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.extensions.setContent

@AndroidEntryPoint
class CommentsRsListActivity : BaseAppCompatActivity() {
    private val viewModel: CommentsRsListViewModel by viewModels()

    // The detail reports RESULT_OK when it changed the comment (moderation, reply, edit,
    // delete); only then refresh, so a view-only visit keeps the paged lists and scroll
    // position intact.
    private val detailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.refreshAllTabs()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        observeEvents()

        setContent {
            val tabStates by viewModel.tabStates.collectAsState()
            val selectedIds by viewModel.selectedIds.collectAsState()
            val confirmation by viewModel.pendingConfirmation.collectAsState()
            val isSearchActive by viewModel.isSearchActive.collectAsState()
            val searchQuery by viewModel.searchQuery.collectAsState()
            val isQuerySearchable by viewModel.isQuerySearchable.collectAsState()
            AppThemeM3 {
                CommentsRsListScreen(
                    tabStates = tabStates,
                    selectedIds = selectedIds,
                    pendingConfirmation = confirmation,
                    isSearchActive = isSearchActive,
                    searchQuery = searchQuery,
                    isQuerySearchable = isQuerySearchable,
                    onDismissConfirmation = viewModel::onDismissPendingAction,
                    snackbarMessages = viewModel.snackbarMessages,
                    onSearchOpen = viewModel::onSearchOpen,
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    onSearchClose = viewModel::onSearchClose,
                    onInitTab = viewModel::initTab,
                    onTabChanged = viewModel::onTabChanged,
                    onRefreshTab = { tab -> viewModel.refreshTab(tab, isUserRefresh = true) },
                    onLoadMore = viewModel::loadMore,
                    onNavigateBack = { onBackPressedDispatcher.onBackPressed() },
                    onCommentClick = viewModel::onCommentClick,
                    onCommentLongClick = viewModel::onCommentLongClick,
                    onClearSelection = viewModel::onClearSelection,
                    onBatchAction = viewModel::onBatchAction,
                    onConfirmPendingAction = viewModel::onConfirmPendingAction
                )
            }
        }
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event -> handleEvent(event) }
            }
        }
    }

    private fun handleEvent(event: CommentsRsListEvent) {
        when (event) {
            is CommentsRsListEvent.OpenCommentDetail -> detailLauncher.launch(
                UnifiedCommentsDetailsActivity.createIntent(this, event.site, event.remoteCommentId)
            )
            is CommentsRsListEvent.ShowToast -> ToastUtils.showToast(this, event.messageResId)
            is CommentsRsListEvent.Finish -> finish()
        }
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, CommentsRsListActivity::class.java)
    }
}
