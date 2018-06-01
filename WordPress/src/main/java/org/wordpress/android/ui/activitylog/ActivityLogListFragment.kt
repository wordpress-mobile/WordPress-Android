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
import org.wordpress.android.R.string
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.posts.BasicFragmentDialog
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.WPSwipeToRefreshHelper.buildSwipeToRefreshHelper
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.viewmodel.activitylog.ActivityLogListItemViewModel
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.ActivityLogListStatus.FETCHING
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.ActivityLogListStatus.LOADING_MORE
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

    fun onRewindConfirmed(activityId: String) {
        viewModel.events.value?.firstOrNull { it.activityId == activityId }?.let { item ->
            viewModel.onRewindConfirmed(ActivityLogListItemViewModel.makeRewindItem(
                    getString(R.string.activity_log_currently_restoring_title),
                    getString(R.string.activity_log_currently_restoring_message, item.date, item.time)))
        }
    }

    private fun setupObservers() {
        viewModel.events.observe(this, Observer<List<ActivityLogListItemViewModel>> {
            reloadEvents()
        })

        viewModel.eventListStatus.observe(this, Observer<ActivityLogViewModel.ActivityLogListStatus> { listStatus ->
            refreshProgressBars(listStatus)
        })

        viewModel.showItemDetail.observe(this, Observer<ActivityLogListItemViewModel> {
            ActivityLauncher.viewActivityLogDetailForResult(activity, viewModel.site, it?.activityId)
        })

        viewModel.showRewindDialog.observe(this, Observer<ActivityLogListItemViewModel> {
            displayRewindDialog(it)
        })
    }

    private fun displayRewindDialog(it: ActivityLogListItemViewModel?) {
        val dialog = BasicFragmentDialog()
        dialog.initialize(it!!.activityId,
                getString(string.activity_log_rewind_site),
                getString(string.activity_log_rewind_dialog_message, it.date, it.time),
                getString(string.activity_log_rewind_site),
                getString(string.cancel))
        dialog.show(fragmentManager, it.activityId)
    }

    private fun refreshProgressBars(eventListStatus: ActivityLogViewModel.ActivityLogListStatus?) {
        if (!isAdded || view == null) {
            return
        }
        // We want to show the swipe refresher for the initial fetch but not while loading more
        swipeToRefreshHelper.isRefreshing = eventListStatus == FETCHING
        // We want to show the progress bar at the bottom while loading more but not for initial fetch
        val showLoadMore = eventListStatus == LOADING_MORE
        activityLogListProgress.visibility = if (showLoadMore) View.VISIBLE else View.GONE

        emptyView.visibility = if (viewModel.events.value?.isNotEmpty() == false &&
                eventListStatus !== LOADING_MORE &&
                eventListStatus !== FETCHING) View.VISIBLE else View.GONE
    }

    private fun reloadEvents() {
        setEvents(viewModel.events.value ?: emptyList())
    }

    private fun onItemClicked(item: ActivityLogListItemViewModel) {
        viewModel.onItemClicked(item)
    }

    private fun onRewindButtonClicked(item: ActivityLogListItemViewModel) {
        viewModel.onRewindButtonClicked(item)
    }

    private fun setEvents(events: List<ActivityLogListItemViewModel>) {
        context?.let {
            val adapter: ActivityLogAdapter
            if (activityLogList.adapter == null) {
                adapter = ActivityLogAdapter(
                        it,
                        viewModel,
                        this::onItemClicked,
                        this::onRewindButtonClicked
                )
                activityLogList.adapter = adapter
            } else {
                adapter = activityLogList.adapter as ActivityLogAdapter
            }
            adapter.updateList(events)

            if (viewModel.eventListStatus.value != LOADING_MORE) {
                activityLogList.scrollToPosition(0)
            }
        }
    }
}
