package org.wordpress.android.ui.stats.refresh.lists.detail

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.StatsDetailFragmentBinding
import org.wordpress.android.ui.stats.refresh.lists.StatsListFragment
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.helpers.SwipeToRefreshHelper

@AndroidEntryPoint
class InsightsDetailFragment : Fragment(R.layout.stats_detail_fragment) {
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper

    private val viewModel: InsightsDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = requireActivity()
        val listType = nonNullActivity.intent.extras?.get(StatsListFragment.LIST_TYPE) as StatsSection
        with(StatsDetailFragmentBinding.bind(view)) {
            with(nonNullActivity as AppCompatActivity) {
                setSupportActionBar(toolbar)
                supportActionBar?.let {
                    it.title = getString(listType.titleRes)
                    it.setHomeButtonEnabled(true)
                    it.setDisplayHomeAsUpEnabled(true)
                }
            }
            initializeViewModels(nonNullActivity)
            initializeViews()
        }
    }

    private fun StatsDetailFragmentBinding.initializeViews() {
        swipeToRefreshHelper = WPSwipeToRefreshHelper.buildSwipeToRefreshHelper(pullToRefresh) {
            viewModel.onPullToRefresh()
        }
    }

    private fun initializeViewModels(activity: FragmentActivity) {
        val siteId = activity.intent?.getIntExtra(WordPress.LOCAL_SITE_ID, 0) ?: 0
        viewModel.init(siteId)
        setupObservers(viewModel)
    }

    private fun setupObservers(viewModel: InsightsDetailViewModel) {
        viewModel.isRefreshing.observe(viewLifecycleOwner) {
            it?.let { isRefreshing ->
                swipeToRefreshHelper.isRefreshing = isRefreshing
            }
        }
    }
}
