package org.wordpress.android.ui.pagesrs

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.pagesrs.screens.PagesRsListScreen
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.extensions.setContent

@AndroidEntryPoint
class PagesRsListActivity : BaseAppCompatActivity() {
    private val viewModel: PagesRsListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        observeEvents()

        setContent {
            val tabStates by viewModel.tabStates.collectAsState()
            val isOpeningPage by viewModel.isOpeningPage.collectAsState()
            AppThemeM3 {
                PagesRsListScreen(
                    tabStates = tabStates,
                    isOpeningPage = isOpeningPage,
                    snackbarMessages = viewModel.snackbarMessages,
                    onInitTab = viewModel::initTab,
                    onTabChanged = viewModel::onTabChanged,
                    onRefreshTab = { tab -> viewModel.refreshTab(tab, isUserRefresh = true) },
                    onLoadMore = viewModel::loadMorePages,
                    onNavigateBack = { onBackPressedDispatcher.onBackPressed() },
                    onPageClick = viewModel::openPage
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

    private fun handleEvent(event: PageRsListEvent) {
        when (event) {
            is PageRsListEvent.EditPage ->
                ActivityLauncher.editPostOrPageForResult(this, event.site, event.page)
            is PageRsListEvent.ShowToast -> ToastUtils.showToast(this, event.messageResId)
            is PageRsListEvent.Finish -> finish()
        }
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, PagesRsListActivity::class.java)
    }
}
