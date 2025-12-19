package org.wordpress.android.posttypes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.posttypes.bridge.BridgeConstants
import org.wordpress.android.posttypes.bridge.CptActivity
import org.wordpress.android.posttypes.bridge.CptTheme
import org.wordpress.android.posttypes.bridge.SiteReference
import org.wordpress.android.posttypes.bridge.applyBaseSetup
import org.wordpress.android.posttypes.compose.CptPostTypesScreen

@AndroidEntryPoint
class CptPostTypesActivity : AppCompatActivity(), CptActivity {
    private val viewModel: CptPostTypesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        applyBaseSetup()
        super.onCreate(savedInstanceState)
        setContent {
            CptTheme {
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
                val intent = if (action.hierarchical) {
                    CptHierarchicalPostListActivity.createIntent(
                        context = this,
                        site = action.site,
                        postTypeSlug = action.postTypeSlug,
                        postTypeLabel = action.postTypeLabel
                    )
                } else {
                    CptFlatPostListActivity.createIntent(
                        context = this,
                        site = action.site,
                        postTypeSlug = action.postTypeSlug,
                        postTypeLabel = action.postTypeLabel
                    )
                }
                startActivity(intent)
            }
        }
    }

    companion object {
        fun createIntent(context: Context, site: SiteReference): Intent {
            return Intent(context, CptPostTypesActivity::class.java).apply {
                putExtra(BridgeConstants.EXTRA_SITE, site)
            }
        }
    }
}
