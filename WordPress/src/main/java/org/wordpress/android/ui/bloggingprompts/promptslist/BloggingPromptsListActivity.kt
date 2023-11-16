package org.wordpress.android.ui.bloggingprompts.promptslist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.bloggingprompts.promptslist.compose.BloggingPromptsListScreen
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.posts.PostUtils
import org.wordpress.android.util.extensions.setContent

@AndroidEntryPoint
class BloggingPromptsListActivity : LocaleAwareActivity() {
    private val viewModel: BloggingPromptsListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                val uiState by viewModel.uiStateFlow.collectAsState()
                BloggingPromptsListScreen(
                    uiState,
                    { onBackPressedDispatcher.onBackPressed() },
                    viewModel::onPromptListItemClicked
                )
            }
        }
        observeActions()
    }

    /**
     * Since we're declaring that this Activity handles orientation changes by itself in the
     * AndroidManifest.xml (android:configChanges="orientation|screenSize") Activity#onResume will not be called
     * when the screen orientation changes. This was done on purpose to avoid loading the list of prompts again
     * unnecessarily and also to avoid tracking of the screen shown analytics event again while in the same screen.
     */
    override fun onResume() {
        super.onResume()
        viewModel.onScreenShown()
    }

    private fun observeActions() {
        viewModel.actionEvents.onEach(this::handleActionEvents).launchIn(lifecycleScope)
    }

    private fun handleActionEvents(actionEvent: BloggingPromptsListViewModel.ActionEvent) {
        when (actionEvent) {
            is BloggingPromptsListViewModel.ActionEvent.OpenEditor -> {
                startActivity(
                    ActivityLauncher.createOpenEditorWithBloggingPromptIntent(
                        this, actionEvent.bloggingPromptId, PostUtils.EntryPoint.BLOGGING_PROMPTS_LIST
                    )
                )
            }
        }
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context) = Intent(context, BloggingPromptsListActivity::class.java)
    }
}
