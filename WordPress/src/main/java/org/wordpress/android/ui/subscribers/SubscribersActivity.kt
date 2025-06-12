package org.wordpress.android.ui.subscribers

import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.dataview.DataViewScreen
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.extensions.getSerializableCompat
import org.wordpress.android.util.extensions.getSerializableExtraCompat

@AndroidEntryPoint
class SubscribersActivity : BaseAppCompatActivity() {
    private val viewModel by viewModels<SubscribersViewModel>()
    private var site: SiteModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        site = if (savedInstanceState == null) {
            intent.getSerializableExtraCompat(WordPress.SITE)
        } else {
            savedInstanceState.getSerializableCompat(WordPress.SITE)
        }
        if (site == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT)
            finish()
            return
        }

        setContentView(
            ComposeView(this).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    this.isForceDarkAllowed = false
                }
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    DataViewScreen(
                        title = getString(R.string.subscribers),
                        uiState = viewModel.uiState.collectAsState(),
                        items = viewModel.subscribers.collectAsState(),
                        supportedFilters = viewModel.getSupportedFilters(),
                        currentFilter = viewModel.itemFilter.collectAsState().value,
                        onRefresh = {
                            viewModel.refreshData()
                                    },
                        onSearchQueryChange = { query ->
                            viewModel.onSearchQueryChange(query)
                        },
                        onItemClick = { item ->
                            viewModel.onItemClick(item)
                        },
                        onFilterClick = { filter ->
                            viewModel.onFilterClick(filter)
                        },
                        onBackClick = {
                            finish()
                        }
                    )
                }
            }
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(WordPress.SITE, site)
    }
}
