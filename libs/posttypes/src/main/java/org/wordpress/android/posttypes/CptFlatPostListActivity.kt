package org.wordpress.android.posttypes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.posttypes.bridge.BridgeConstants
import org.wordpress.android.posttypes.bridge.CptActivity
import org.wordpress.android.posttypes.bridge.CptTheme
import org.wordpress.android.posttypes.bridge.SiteReference
import org.wordpress.android.posttypes.bridge.applyBaseSetup
import org.wordpress.android.posttypes.compose.CptFlatPostListScreen

@AndroidEntryPoint
class CptFlatPostListActivity : AppCompatActivity(), CptActivity {
    private val viewModel: CptFlatPostListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        applyBaseSetup()
        super.onCreate(savedInstanceState)
        setContent {
            CptTheme {
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
            site: SiteReference,
            postTypeSlug: String,
            postTypeLabel: String
        ): Intent {
            return Intent(context, CptFlatPostListActivity::class.java).apply {
                putExtra(BridgeConstants.EXTRA_SITE, site)
                putExtra(EXTRA_POST_TYPE_SLUG, postTypeSlug)
                putExtra(EXTRA_POST_TYPE_LABEL, postTypeLabel)
            }
        }
    }
}
