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
import uniffi.wp_mobile_local_content.WorkingCopyId

@AndroidEntryPoint
class WorkingCopyDetailActivity : BaseAppCompatActivity() {
    private val viewModel: WorkingCopyDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            AppThemeM3 {
                WorkingCopyDetailScreen(
                    state = state,
                    onNavigateBack = onBackPressedDispatcher::onBackPressed
                )
            }
        }
    }

    companion object {
        fun createIntent(context: Context, kind: WorkingCopyKind, id: WorkingCopyId): Intent =
            Intent(context, WorkingCopyDetailActivity::class.java)
                .putExtra(WorkingCopyDetailViewModel.EXTRA_KIND, kind.name)
                .putExtra(WorkingCopyDetailViewModel.EXTRA_ID, id)
    }
}
