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
import org.wordpress.android.ui.posttypes.compose.CptFlatPostListScreen
import org.wordpress.android.util.extensions.setContent

@AndroidEntryPoint
class CptFlatPostListActivity : BaseAppCompatActivity() {
    private val viewModel: CptFlatPostListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppThemeM3 {
                val uiState by viewModel.uiState.collectAsState()
                CptFlatPostListScreen(
                    uiState = uiState,
                    onBackClick = { onBackPressedDispatcher.onBackPressed() },
                    onPostClick = viewModel::onPostClick
                )
            }
        }
    }

    companion object {
        const val EXTRA_POST_TYPE_SLUG = "post_type_slug"
        const val EXTRA_POST_TYPE_LABEL = "post_type_label"

        fun createIntent(
            context: Context,
            site: SiteModel,
            postTypeSlug: String,
            postTypeLabel: String
        ): Intent {
            return Intent(context, CptFlatPostListActivity::class.java).apply {
                putExtra(WordPress.SITE, site)
                putExtra(EXTRA_POST_TYPE_SLUG, postTypeSlug)
                putExtra(EXTRA_POST_TYPE_LABEL, postTypeLabel)
            }
        }
    }
}
