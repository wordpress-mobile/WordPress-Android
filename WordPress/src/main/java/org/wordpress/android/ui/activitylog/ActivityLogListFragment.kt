package org.wordpress.android.ui.activitylog

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_log_list_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.WPSwipeToRefreshHelper.buildSwipeToRefreshHelper
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.viewmodel.activitylog.ActivityLogListItemViewModel
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel
import javax.inject.Inject

class ActivityLogListFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: ActivityLogViewModel
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_log_list_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activityLogList.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)

        swipeToRefreshHelper = buildSwipeToRefreshHelper(
                activityLogPullToRefresh,
                {
                    if (NetworkUtils.checkConnection(activity)) {
                        viewModel.pullToRefresh()
                    } else {
                        swipeToRefreshHelper.isRefreshing = false
                    }
                })

        (activity?.application as WordPress).component()?.inject(this)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(ActivityLogViewModel::class.java)

        val site = if (savedInstanceState == null) {
            activity?.intent?.getSerializableExtra(WordPress.SITE) as SiteModel
        } else {
            savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
        }

        setupObservers()

        viewModel.start(site)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(WordPress.SITE, viewModel.site)
        super.onSaveInstanceState(outState)
    }

    private fun setupObservers() {
        viewModel.events.observe(this, Observer<List<ActivityLogListItemViewModel>> {
            reloadEvents()
        })

        viewModel.eventListStatus.observe(this, Observer<ActivityLogViewModel.ActivityLogListStatus> { listStatus ->
            refreshProgressBars(listStatus)
        })
    }

    private fun refreshProgressBars(eventListStatus: ActivityLogViewModel.ActivityLogListStatus?) {
        if (!isAdded || view == null) {
            return
        }
        // We want to show the swipe refresher for the initial fetch but not while loading more
        swipeToRefreshHelper.isRefreshing = eventListStatus === ActivityLogViewModel.ActivityLogListStatus.FETCHING
        // We want to show the progress bar at the bottom while loading more but not for initial fetch
        val showLoadMore = eventListStatus === ActivityLogViewModel.ActivityLogListStatus.LOADING_MORE
        activityLogListProgress.visibility = if (showLoadMore) View.VISIBLE else View.GONE
    }

    private fun reloadEvents() {
        setEvents(viewModel.events.value ?: emptyList())
    }

    private fun setEvents(events: List<ActivityLogListItemViewModel>) {
        context?.let {
            val adapter: ActivityLogAdapter
            if (activityLogList.adapter == null) {
                adapter = ActivityLogAdapter(
                        it,
                        viewModel,
                        { ActivityLauncher.viewActivityLogDetail(activity, viewModel.site, it.activityId) },
                        { }
                )
                activityLogList.adapter = adapter
            } else {
                adapter = activityLogList.adapter as ActivityLogAdapter
            }
            adapter.updateList(events)
        }
    }
}
