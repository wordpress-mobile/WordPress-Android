package org.wordpress.android.ui.activitylog.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_log_list_fragment.*
import kotlinx.android.synthetic.main.activity_log_list_loading_item.*
import org.wordpress.android.R
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
import org.wordpress.android.widgets.WPSnackbar
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

        log_list_view.layoutManager = LinearLayoutManager(nonNullActivity, RecyclerView.VERTICAL, false)

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
        viewModel.events.observe(viewLifecycleOwner, Observer {
            reloadEvents(it ?: emptyList())
        })

        viewModel.eventListStatus.observe(viewLifecycleOwner, Observer { listStatus ->
            refreshProgressBars(listStatus)
        })

        viewModel.showItemDetail.observe(viewLifecycleOwner, Observer {
            if (it is ActivityLogListItem.Event) {
                ActivityLauncher.viewActivityLogDetailForResult(activity, viewModel.site, it.activityId)
            }
        })

        viewModel.showRewindDialog.observe(viewLifecycleOwner, Observer {
            if (it is ActivityLogListItem.Event) {
                displayRewindDialog(it)
            }
        })

        viewModel.showSnackbarMessage.observe(viewLifecycleOwner, Observer { message ->
            val parent: View? = activity?.findViewById(android.R.id.content)
            if (message != null && parent != null) {
                WPSnackbar.make(parent, message, Snackbar.LENGTH_LONG).show()
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
                    getString(R.string.activity_log_rewind_site),
                    getString(R.string.activity_log_rewind_dialog_message, item.formattedDate, item.formattedTime),
                    getString(R.string.activity_log_rewind_site),
                    getString(R.string.cancel))
            dialog.show(requireFragmentManager(), it)
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
