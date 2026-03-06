package org.wordpress.android.ui.postsrs

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
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.postsrs.screens.PostRsSettingsScreen
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.extensions.setContent

@AndroidEntryPoint
class PostRsSettingsActivity : BaseAppCompatActivity() {
    private val viewModel: PostRsSettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        observeEvents()

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            AppThemeM3 {
                PostRsSettingsScreen(
                    uiState = uiState,
                    onNavigateBack = {
                        onBackPressedDispatcher.onBackPressed()
                    },
                    onRetry = viewModel::retry,
                    onRetryField = viewModel::retryField,
                )
            }
        }
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is PostRsSettingsEvent.Finish -> finish()
                        is PostRsSettingsEvent.ShowSnackbar ->
                            ToastUtils.showToast(
                                this@PostRsSettingsActivity,
                                event.message
                            )
                    }
                }
            }
        }
    }

    companion object {
        fun createIntent(context: Context, postId: Long): Intent {
            return Intent(context, PostRsSettingsActivity::class.java)
                .putExtra(PostRsSettingsViewModel.EXTRA_POST_ID, postId)
        }
    }
}
