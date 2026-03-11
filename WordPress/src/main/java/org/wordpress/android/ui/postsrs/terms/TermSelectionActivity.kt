package org.wordpress.android.ui.postsrs.terms

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.wordpress.android.R
import uniffi.wp_api.TermEndpointType
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.extensions.setContent

@AndroidEntryPoint
class TermSelectionActivity : BaseAppCompatActivity() {
    private val viewModel: TermSelectionViewModel
        by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        observeEvents()

        val title = if (
            viewModel.endpointType
                is TermEndpointType.Categories
        ) {
            getString(R.string.categories)
        } else {
            getString(R.string.post_settings_tags)
        }

        setContent {
            val uiState by viewModel.uiState
                .collectAsState()
            AppThemeM3 {
                TermSelectionScreen(
                    title = title,
                    uiState = uiState,
                    onBackClicked =
                        viewModel::onBackClicked,
                    onSaveClicked =
                        viewModel::onSaveClicked,
                    onTermToggled =
                        viewModel::onTermToggled,
                    onSearchQueryChanged =
                        viewModel::onSearchQueryChanged,
                    onLoadMore =
                        viewModel::onLoadMore,
                    onAddTermClicked =
                        viewModel::onAddTermClicked,
                    onAddDialogDismissed =
                        viewModel::onAddDialogDismissed,
                    onAddTermConfirmed =
                        viewModel::onAddTermConfirmed,
                    onRetry = viewModel::retry,
                )
            }
        }
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is TermSelectionEvent.Finish ->
                            finish()
                        is TermSelectionEvent
                            .FinishWithSelection -> {
                            setResult(
                                RESULT_OK,
                                Intent().putExtra(
                                    TermSelectionViewModel
                                        .RESULT_SELECTED_IDS,
                                    event.selectedIds
                                        .toLongArray()
                                )
                            )
                            finish()
                        }
                        is TermSelectionEvent
                            .ShowSnackbar ->
                            ToastUtils.showToast(
                                this@TermSelectionActivity,
                                event.message
                            )
                    }
                }
            }
        }
    }

    companion object {
        fun createIntent(
            context: Context,
            isCategories: Boolean,
            selectedIds: LongArray,
        ): Intent {
            return Intent(
                context,
                TermSelectionActivity::class.java
            )
                .putExtra(
                    TermSelectionViewModel
                        .EXTRA_IS_CATEGORIES,
                    isCategories
                )
                .putExtra(
                    TermSelectionViewModel
                        .EXTRA_SELECTED_IDS,
                    selectedIds
                )
        }
    }
}
