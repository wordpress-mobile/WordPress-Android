package org.wordpress.android.ui.activitylog.list

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
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

        swipeToRefreshHelper = buildSwipeToRefreshHelper(activityLogPullToRefresh) {
            if (NetworkUtils.checkConnection(activity)) {
                viewModel.onPullToRefresh()
            } else {
                swipeToRefreshHelper.isRefreshing = false
            }
        }

        (activity?.application as WordPress).component()?.inject(this)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(ActivityLogViewModel::class.java)

        val site = if (savedInstanceState == null) {
            activity?.intent?.getSerializableExtra(WordPress.SITE) as SiteModel
        } else {
            savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
        }

        activityLogList.addOnScrollListener(object :RecyclerView.OnScrollListener(){
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                if (!recyclerView!!.canScrollVertically(1) && dy != 0) {
                    viewModel.onScrolledToBottom()
                }
            }
        })

        setupObservers()

        viewModel.start(site)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(WordPress.SITE, viewModel.site)
        super.onSaveInstanceState(outState)
    }

    fun onRewindConfirmed(activityId: String) {
        viewModel.onRewindConfirmed(activityId)
    }

    private fun setupObservers() {
        viewModel.events.observe(this, Observer {
            reloadEvents()
        })

        viewModel.eventListStatus.observe(this, Observer { listStatus ->
            refreshProgressBars(listStatus)
        })

        viewModel.showItemDetail.observe(this, Observer {
            if (it is ActivityLogListItem.Event) {
                ActivityLauncher.viewActivityLogDetailForResult(activity, viewModel.site, it.activityId)
            }
        })

        viewModel.showRewindDialog.observe(this, Observer {
            if (it is ActivityLogListItem.Event) {
                displayRewindDialog(it)
            }
        })

        viewModel.moveToTop.observe(this, Observer {
            activityLogList.scrollToPosition(0)
        })
    }

    private fun displayRewindDialog(item: ActivityLogListItem.Event) {
        val dialog = BasicFragmentDialog()
        item.rewindId?.let {
            dialog.initialize(it,
                    getString(string.activity_log_rewind_site),
                    getString(string.activity_log_rewind_dialog_message, item.formattedDate, item.formattedTime),
                    getString(string.activity_log_rewind_site),
                    getString(string.cancel))
            dialog.show(fragmentManager, it)
        }
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

    private fun onItemClicked(item: ActivityLogListItem) {
        viewModel.onItemClicked(item)
    }

    private fun onItemButtonClicked(item: ActivityLogListItem) {
        viewModel.onActionButtonClicked(item)
    }

    private fun setEvents(events: List<ActivityLogListItem>) {
        context?.let {
            val adapter: ActivityLogAdapter
            if (activityLogList.adapter == null) {
                adapter = ActivityLogAdapter(
                        it,
                        this::onItemClicked,
                        this::onItemButtonClicked
                )
                activityLogList.adapter = adapter
            } else {
                adapter = activityLogList.adapter as ActivityLogAdapter
            }
            adapter.updateList(events)
        }
    }
}
