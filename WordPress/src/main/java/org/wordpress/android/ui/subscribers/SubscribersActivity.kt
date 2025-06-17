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
import org.wordpress.android.ui.dataview.DataViewScreen
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.util.ToastUtils

@AndroidEntryPoint
class SubscribersActivity : BaseAppCompatActivity() {
    private val viewModel by viewModels<SubscribersViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            val siteId = intent.getLongExtra(WordPress.REMOTE_SITE_ID, 0L)
            if (siteId == 0L) {
                ToastUtils.showToast(this, R.string.blog_not_found)
                finish()
                return
            }
            viewModel.siteId = siteId
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
                        items = viewModel.items.collectAsState(),
                        supportedFilters = viewModel.getSupportedFilters(),
                        currentFilter = viewModel.itemFilter.collectAsState().value,
                        onRefresh = {
                            viewModel.onRefreshData()
                        },
                        onFetchMore = {
                            viewModel.onFetchMoreData()
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
}
