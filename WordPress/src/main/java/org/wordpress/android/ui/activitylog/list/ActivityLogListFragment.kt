package org.wordpress.android.ui.activitylog.list

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_log_list_fragment.*
import kotlinx.android.synthetic.main.activity_log_list_loading_item.*
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

        val nonNullActivity = checkNotNull(activity)

        log_list_view.layoutManager = LinearLayoutManager(nonNullActivity, LinearLayoutManager.VERTICAL, false)

        swipeToRefreshHelper = buildSwipeToRefreshHelper(swipe_refresh_layout) {
            if (NetworkUtils.checkConnection(nonNullActivity)) {
                viewModel.onPullToRefresh()
            } else {
                swipeToRefreshHelper.isRefreshing = false
            }
        }

        (nonNullActivity.application as WordPress).component()?.inject(this)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(ActivityLogViewModel::class.java)

        val site = if (savedInstanceState == null) {
            val nonNullIntent = checkNotNull(nonNullActivity.intent)
            nonNullIntent.getSerializableExtra(WordPress.SITE) as SiteModel
        } else {
            savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
        }

        log_list_view.setEmptyView(actionable_empty_view)
        log_list_view.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (!recyclerView.canScrollVertically(1) && dy != 0) {
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
            reloadEvents(it ?: emptyList())
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

        viewModel.showSnackbarMessage.observe(this, Observer { message ->
            val parent: View? = activity?.findViewById(android.R.id.content)
            if (message != null && parent != null) {
                val snackbar = Snackbar.make(parent, message, Snackbar.LENGTH_LONG)
                val snackbarText = snackbar.view.findViewById<TextView>(android.support.design.R.id.snackbar_text)
                snackbarText.maxLines = 2
                snackbar.show()
            }
        })

        viewModel.moveToTop.observe(this, Observer {
            log_list_view.scrollToPosition(0)
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
        progress?.visibility = if (showLoadMore) View.VISIBLE else View.GONE
    }

    private fun reloadEvents(data: List<ActivityLogListItem>) {
        setEvents(data)
    }

    private fun onItemClicked(item: ActivityLogListItem) {
        viewModel.onItemClicked(item)
    }

    private fun onItemButtonClicked(item: ActivityLogListItem) {
        viewModel.onActionButtonClicked(item)
    }

    private fun setEvents(events: List<ActivityLogListItem>) {
        val adapter: ActivityLogAdapter
        if (log_list_view.adapter == null) {
            adapter = ActivityLogAdapter(this::onItemClicked, this::onItemButtonClicked)
            log_list_view.adapter = adapter
        } else {
            adapter = log_list_view.adapter as ActivityLogAdapter
        }
        adapter.updateList(events)
    }
}
