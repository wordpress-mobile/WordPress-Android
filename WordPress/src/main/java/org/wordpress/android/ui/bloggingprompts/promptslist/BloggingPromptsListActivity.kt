package org.wordpress.android.ui.bloggingprompts.promptslist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.ui.compose.theme.AppTheme

@AndroidEntryPoint
class BloggingPromptsListActivity : AppCompatActivity() {
    private val viewModel: BloggingPromptsListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                val uiState by viewModel.uiStateFlow.collectAsState()
                BloggingPromptsListScreen(uiState, ::onBackPressed)
            }
        }
        viewModel.start()
    }

    // TODO it might be safer bringing in the androidx.activity:activity-compose lib
    private fun setContent(content: @Composable () -> Unit) {
        val composeView = ComposeView(this).apply { setContent(content) }
        setContentView(composeView)
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context) = Intent(context, BloggingPromptsListActivity::class.java)
    }
}
