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
import org.wordpress.android.util.extensions.getSerializableCompat
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import java.io.Serializable

@AndroidEntryPoint
class InsightsDetailFragment : Fragment(R.layout.stats_detail_fragment) {
    private val viewsVisitorsDetailViewModel: ViewsVisitorsDetailViewModel by viewModels()
    private val totalLikesDetailViewModel: TotalLikesDetailViewModel by viewModels()
    private val totalCommentsDetailViewModel: TotalCommentsDetailViewModel by viewModels()
    private val totalFollowersDetailViewModel: TotalFollowersDetailViewModel by viewModels()

    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper
    private lateinit var viewModel: InsightsDetailViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = requireActivity()
        val listType = nonNullActivity.intent.extras?.getSerializableCompat<StatsSection>(StatsListFragment.LIST_TYPE)
        with(StatsDetailFragmentBinding.bind(view)) {
            with(nonNullActivity as AppCompatActivity) {
                setSupportActionBar(toolbar)
                supportActionBar?.let { actionBar ->
                    listType?.let { statsSection -> actionBar.title = getString(statsSection.titleRes) }
                    actionBar.setHomeButtonEnabled(true)
                    actionBar.setDisplayHomeAsUpEnabled(true)
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
        val listType = activity.intent.extras?.getSerializableCompat<Serializable>(StatsListFragment.LIST_TYPE)

        viewModel = when (listType) {
            StatsSection.INSIGHT_DETAIL -> viewsVisitorsDetailViewModel
            StatsSection.TOTAL_LIKES_DETAIL -> totalLikesDetailViewModel
            StatsSection.TOTAL_COMMENTS_DETAIL -> totalCommentsDetailViewModel
            StatsSection.TOTAL_FOLLOWERS_DETAIL -> totalFollowersDetailViewModel
            else -> viewsVisitorsDetailViewModel
        }

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
