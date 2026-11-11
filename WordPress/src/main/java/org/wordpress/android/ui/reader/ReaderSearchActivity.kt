package org.wordpress.android.ui.reader

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.isVisible
import androidx.fragment.compose.AndroidFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.wordpress.android.networking.NetworkConnectionMonitor
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.tracker.ReaderTrackerType.MAIN_READER
import org.wordpress.android.ui.reader.viewmodels.ReaderSearchEvent
import org.wordpress.android.ui.reader.viewmodels.ReaderSearchViewModel
import org.wordpress.android.ui.reader.views.compose.search.ReaderSearchActions
import org.wordpress.android.ui.reader.views.compose.search.ReaderSearchScreen
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.extensions.setContent
import javax.inject.Inject

/**
 * Reader search: the query field, recent-query suggestions and the Posts/Sites tabs. The Posts tab is
 * [ReaderPostListFragment] in search mode, hosted with AndroidFragment.
 */
@AndroidEntryPoint
class ReaderSearchActivity : BaseAppCompatActivity() {
    @Inject
    lateinit var readerTracker: ReaderTracker

    @Inject
    lateinit var networkConnectionMonitor: NetworkConnectionMonitor

    private val viewModel: ReaderSearchViewModel by viewModels()

    // state, so the screen re-applies the list's visibility once AndroidFragment provides it
    private var postListFragment by mutableStateOf<ReaderPostListFragment?>(null)

    // starts as connected so the replay of "connected" doesn't re-run the search - only a real
    // offline->online transition should
    private var lastConnected = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this) { finishWithRefreshSubscriptionsResult() }
        observeEvents()
        networkConnectionMonitor.isConnected.observe(this) { connected ->
            if (connected && !lastConnected) {
                viewModel.onConnectionRestored()
            }
            lastConnected = connected
        }

        val actions = ReaderSearchActions(
            onBack = ::finishWithRefreshSubscriptionsResult,
            onQueryChanged = viewModel::onQueryChanged,
            onSearch = viewModel::onSearchSubmitted,
            onSuggestionClicked = viewModel::onSuggestionClicked,
            onSuggestionDeleteClicked = viewModel::onSuggestionDeleteClicked,
            onClearHistoryConfirmed = viewModel::onClearHistoryConfirmed,
            onTabSelected = viewModel::onTabSelected,
            onPostsTabReselected = { postListFragment?.onScrollToTop() },
            onSiteClicked = viewModel::onSiteClicked,
            onSiteFollowClicked = viewModel::onSiteFollowClicked,
            onLoadMoreSites = viewModel::onLoadMoreSites
        )
        val postListArguments = ReaderPostListFragment.searchArguments()

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            AppThemeM3 {
                ReaderSearchScreen(uiState = uiState, actions = actions) { isPostListVisible ->
                    AndroidFragment<ReaderPostListFragment>(arguments = postListArguments) { postListFragment = it }
                    // hide the list rather than removing it, so its results survive tab switches, and so
                    // TalkBack doesn't reach it under the suggestions or sites
                    SideEffect { postListFragment?.view?.isVisible = isPostListVisible }
                }
            }
        }
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is ReaderSearchEvent.SubmitPostSearch -> postListFragment?.onSearchSubmitted(event.query)
                        ReaderSearchEvent.ClearPostSearch -> postListFragment?.onSearchCleared()
                        is ReaderSearchEvent.ShowSitePreview -> ReaderActivityLauncher.showReaderBlogOrFeedPreview(
                            this@ReaderSearchActivity,
                            event.site.siteId,
                            event.site.feedId,
                            event.site.isFollowing,
                            ReaderTracker.SOURCE_SEARCH,
                            readerTracker
                        )
                        is ReaderSearchEvent.ShowToast ->
                            ToastUtils.showToast(this@ReaderSearchActivity, event.messageRes)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reader search used to be part of the MAIN_READER - it's still being tracked as MAIN_READER so we don't
        // introduce inconsistencies into the existing tracking data
        readerTracker.start(MAIN_READER)
        viewModel.onResume()
    }

    override fun onPause() {
        super.onPause()
        readerTracker.stop(MAIN_READER)
    }

    /*
     * set here rather than up front, since the post list fragment sets its own result when it attaches
     */
    private fun finishWithRefreshSubscriptionsResult() {
        val data = Intent()
        data.putExtra(RESULT_SHOULD_REFRESH_SUBSCRIPTIONS, true)
        setResult(RESULT_OK, data)
        finish()
    }

    companion object {
        const val RESULT_SHOULD_REFRESH_SUBSCRIPTIONS = "should_refresh_subscriptions"
    }
}
