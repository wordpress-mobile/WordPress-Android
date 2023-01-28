package org.wordpress.android.ui.stats.refresh.lists.detail

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import dagger.android.support.DaggerFragment
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.StatsDetailFragmentBinding
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import javax.inject.Inject

class StatsDetailFragment : DaggerFragment(R.layout.stats_detail_fragment) {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var statsSiteProvider: StatsSiteProvider
    private lateinit var viewModel: StatsDetailViewModel
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = requireActivity()
        with(StatsDetailFragmentBinding.bind(view)) {
            with(nonNullActivity as AppCompatActivity) {
                setSupportActionBar(toolbar)
                supportActionBar?.let {
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
        statsSiteProvider.start(siteId)

        val postId = activity.intent?.getLongExtra(POST_ID, 0L)
        val postType = activity.intent?.getStringExtra(POST_TYPE)
        val postTitle = activity.intent?.getStringExtra(POST_TITLE)
        val postUrl = activity.intent?.getStringExtra(POST_URL)

        viewModel = ViewModelProvider(this, viewModelFactory)
            .get(StatsSection.DETAIL.name, StatsDetailViewModel::class.java)
        viewModel.init(
            checkNotNull(postId),
            checkNotNull(postType),
            checkNotNull(postTitle),
            postUrl
        )

        setupObservers(viewModel)
    }

    private fun setupObservers(viewModel: StatsDetailViewModel) {
        viewModel.isRefreshing.observe(viewLifecycleOwner) {
            it?.let { isRefreshing ->
                swipeToRefreshHelper.isRefreshing = isRefreshing
            }
        }
    }
}
