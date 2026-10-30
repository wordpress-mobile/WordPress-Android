package org.wordpress.android.ui.reader

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.viewmodels.ReaderSubsUiEvent
import org.wordpress.android.ui.reader.viewmodels.ReaderSubsViewModel
import org.wordpress.android.ui.reader.views.compose.subs.ReaderSubsActions
import org.wordpress.android.ui.reader.views.compose.subs.ReaderSubsScreen
import org.wordpress.android.util.extensions.setContent
import javax.inject.Inject

/**
 * Manage subscriptions: the user's followed tags and sites
 */
@AndroidEntryPoint
class ReaderSubsActivity : BaseAppCompatActivity() {
    @Inject
    lateinit var readerTracker: ReaderTracker

    private val viewModel: ReaderSubsViewModel by viewModels()

    private val snackbarHostState = SnackbarHostState()

    private val interestsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.onInterestsClosed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // so the bars draw behind the system bars, and the keyboard's insets reach Compose on every API level
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this) { finishWithResult() }
        observeEvents()

        val actions = ReaderSubsActions(
            onBack = ::finishWithResult,
            onTabSelected = viewModel::onTabSelected,
            onSiteSearchOpened = viewModel::onSiteSearchOpened,
            onSiteSearchQueryChanged = viewModel::onSiteSearchQueryChanged,
            onSiteSearchClosed = viewModel::onSiteSearchClosed,
            onTagFollowClicked = viewModel::onTagFollowClicked,
            onSiteClicked = viewModel::onSiteClicked,
            onSiteFollowClicked = viewModel::onSiteFollowClicked,
            onEntryChanged = viewModel::onEntryChanged,
            onAddClicked = viewModel::onAddClicked,
            onShowInterests = { interestsLauncher.launch(ReaderActivityLauncher.createReaderInterestsIntent(this)) }
        )

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            AppThemeM3 {
                ReaderSubsScreen(uiState = uiState, actions = actions, snackbarHostState = snackbarHostState)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is ReaderSubsUiEvent.ShowMessage -> launch {
                            // a new message replaces the current one rather than queueing behind it
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Long)
                        }
                        is ReaderSubsUiEvent.ShowSitePreview -> ReaderActivityLauncher.showReaderBlogOrFeedPreview(
                            this@ReaderSubsActivity,
                            event.blogId,
                            event.feedId,
                            event.isFollowing,
                            ReaderTracker.SOURCE_SETTINGS,
                            readerTracker
                        )
                        ReaderSubsUiEvent.HideKeyboard -> WindowCompat.getInsetsController(window, window.decorView)
                            .hide(WindowInsetsCompat.Type.ime())
                    }
                }
            }
        }
    }

    /*
     * tells the Reader whether to reload its filters, which list the followed tags
     */
    private fun finishWithResult() {
        viewModel.onBack()
        val data = Intent().putExtra(RESULT_SHOULD_REFRESH_SUBSCRIPTIONS, viewModel.hasChangedTags())
        setResult(RESULT_OK, data)
        finish()
    }

    companion object {
        const val TAB_IDX_FOLLOWED_TAGS = 0
        const val TAB_IDX_FOLLOWED_BLOGS = 1
        const val RESULT_SHOULD_REFRESH_SUBSCRIPTIONS = "should_refresh_subscriptions"
    }
}
