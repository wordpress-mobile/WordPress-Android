package org.wordpress.android.ui.stats.refresh.lists.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.stats_detail_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.drawDateSelector
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import javax.inject.Inject

class StatsDetailFragment : DaggerFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var statsSiteProvider: StatsSiteProvider
    private lateinit var viewModel: StatsDetailViewModel
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.stats_detail_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = checkNotNull(activity)

        initializeViewModels(nonNullActivity)
        initializeViews()
    }

    private fun initializeViews() {
        swipeToRefreshHelper = WPSwipeToRefreshHelper.buildSwipeToRefreshHelper(pullToRefresh) {
            viewModel.onPullToRefresh()
        }
    }

    private fun initializeViewModels(activity: FragmentActivity) {
        val siteId = activity.intent?.getIntExtra(WordPress.LOCAL_SITE_ID, 0) ?: 0
        statsSiteProvider.start(siteId)

        val postId = activity.intent?.getLongExtra(POST_ID, 0L)
        val postType = activity.intent?.getSerializableExtra(POST_TYPE) as String?
        val postTitle = activity.intent?.getSerializableExtra(POST_TITLE) as String?
        val postUrl = activity.intent?.getSerializableExtra(POST_URL) as String?

        viewModel = ViewModelProviders.of(this, viewModelFactory)
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
        viewModel.isRefreshing.observe(viewLifecycleOwner, Observer {
            it?.let { isRefreshing ->
                swipeToRefreshHelper.isRefreshing = isRefreshing
            }
        })

        viewModel.selectedDateChanged.observe(viewLifecycleOwner, Observer { event ->
            if (event != null) {
                viewModel.onDateChanged(event.selectedSection)
            }
        })

        viewModel.showDateSelector.observe(viewLifecycleOwner, Observer { dateSelectorUiModel ->
            drawDateSelector(dateSelectorUiModel)
        })
    }
}
