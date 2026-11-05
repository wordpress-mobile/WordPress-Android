package org.wordpress.android.ui.localcontent

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.util.extensions.setContent

@AndroidEntryPoint
class WorkingCopyListActivity : BaseAppCompatActivity() {
    private val viewModel: WorkingCopyListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            AppThemeM3 {
                WorkingCopyListScreen(
                    kind = viewModel.kind,
                    state = state,
                    onWorkingCopyClick = { id ->
                        startActivity(WorkingCopyDetailActivity.createIntent(this, viewModel.kind, id))
                    },
                    onRetry = viewModel::load,
                    onNavigateBack = onBackPressedDispatcher::onBackPressed
                )
            }
        }
    }

    // Working copies can change while this screen is in the background, such as when the editor saves one.
    override fun onResume() {
        super.onResume()
        viewModel.load()
    }

    companion object {
        fun createIntent(context: Context, kind: WorkingCopyKind): Intent =
            Intent(context, WorkingCopyListActivity::class.java)
                .putExtra(WorkingCopyListViewModel.EXTRA_KIND, kind.name)
    }
}
