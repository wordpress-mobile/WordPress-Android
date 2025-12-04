package org.wordpress.android.ui.posttypes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.posttypes.compose.CptPostTypesScreen
import org.wordpress.android.util.extensions.setContent

@AndroidEntryPoint
class CptPostTypesActivity : BaseAppCompatActivity() {
    private val viewModel: CptPostTypesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppThemeM3 {
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.navigation.collect { action ->
                        handleNavigation(action)
                    }
                }

                CptPostTypesScreen(
                    uiState = uiState,
                    onBackClick = { onBackPressedDispatcher.onBackPressed() },
                    onPostTypeClick = viewModel::onPostTypeClick
                )
            }
        }
    }

    private fun handleNavigation(action: CptNavigationAction) {
        when (action) {
            is CptNavigationAction.OpenPostTypeList -> {
                startActivity(
                    CptFlatPostListActivity.createIntent(
                        context = this,
                        site = action.site,
                        postTypeSlug = action.postTypeSlug,
                        postTypeLabel = action.postTypeLabel
                    )
                )
            }
        }
    }

    companion object {
        fun createIntent(context: Context, site: SiteModel): Intent {
            return Intent(context, CptPostTypesActivity::class.java).apply {
                putExtra(WordPress.SITE, site)
            }
        }
    }
}
