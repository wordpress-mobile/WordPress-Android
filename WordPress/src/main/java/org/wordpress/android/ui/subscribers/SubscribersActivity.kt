package org.wordpress.android.ui.subscribers

import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.dataview.DataViewFilterItem
import org.wordpress.android.ui.dataview.DataViewItem
import org.wordpress.android.ui.dataview.DataViewScreen
import org.wordpress.android.ui.dataview.DummyDataViewItems.getDummyData
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.selfhostedusers.SelfHostedUsersViewModel
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.extensions.getSerializableCompat
import org.wordpress.android.util.extensions.getSerializableExtraCompat

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
                        titleRes = R.string.subscribers,
                        items = getDummyData(),
                        filters = listOf(
                            DataViewFilterItem(
                                id = ID_FILTER_EMAIL,
                                title = getString(R.string.subscribers_filter_email_subscription)
                            ),
                            DataViewFilterItem(
                                id = ID_FILER__TYPE,
                                title = getString(R.string.subscribers_filter_subscription_type)
                            )
                        ),
                        onSearchQueryChange = {},
                        onItemClick = { item ->
                            onItemClick(item)
                        },
                        onFilterClick = { filter ->
                            onFilterClick(filter)
                        },
                        onBackClick = { finish() }
                    )
                }
            }
        )
    }

    private fun onItemClick(item: DataViewItem) {
        // TODO
    }

    private fun onFilterClick(filter: DataViewFilterItem) {
        // TODO
    }

    companion object {
        private const val ID_FILTER_EMAIL = 1L
        private const val ID_FILER__TYPE = 2L
    }
}
