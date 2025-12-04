package org.wordpress.android.ui.posttypes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.posttypes.compose.PostTypesScreen
import org.wordpress.android.util.extensions.setContent

@AndroidEntryPoint
class PostTypesActivity : BaseAppCompatActivity() {
    private val viewModel: PostTypesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppThemeM3 {
                val uiState by viewModel.uiState.collectAsState()
                PostTypesScreen(
                    uiState = uiState,
                    onBackClick = { onBackPressedDispatcher.onBackPressed() },
                    onPostTypeClick = viewModel::onPostTypeClick
                )
            }
        }
    }

    companion object {
        fun createIntent(context: Context, site: SiteModel): Intent {
            return Intent(context, PostTypesActivity::class.java).apply {
                putExtra(WordPress.SITE, site)
            }
        }
    }
}
