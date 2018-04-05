package org.wordpress.android.ui.activitylog

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_log_list_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.WPSwipeToRefreshHelper.buildSwipeToRefreshHelper
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel
import javax.inject.Inject

class ActivityLogListFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: ActivityLogViewModel
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper

    companion object {
        val TAG = ActivityLogListFragment::class.java.name

        fun newInstance(site: SiteModel): ActivityLogListFragment {
            val fragment = ActivityLogListFragment()
            val bundle = Bundle()
            bundle.putSerializable(WordPress.SITE, site)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity?.application as WordPress).component()?.inject(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Use the same view model as the ActivityLogActivity
        viewModel = ViewModelProviders.of(activity!!, viewModelFactory)
                .get<ActivityLogViewModel>(ActivityLogViewModel::class.java)

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.events.observe(this, Observer<List<ActivityLogModel>> {
            reloadEvents()
        })

        viewModel.eventListStatus.observe(this, Observer<ActivityLogViewModel.ActivityLogListStatus> { listStatus ->
            refreshProgressBars(listStatus)
        })
    }

    protected fun refreshProgressBars(eventListStatus: ActivityLogViewModel.ActivityLogListStatus?) {
        if (!isAdded || view == null) {
            return
        }
        // We want to show the swipe refresher for the initial fetch but not while loading more
        swipeToRefreshHelper.isRefreshing = eventListStatus === ActivityLogViewModel.ActivityLogListStatus.FETCHING
        // We want to show the progress bar at the bottom while loading more but not for initial fetch
        val showLoadMore = eventListStatus === ActivityLogViewModel.ActivityLogListStatus.LOADING_MORE
        activityLogListProgress.visibility = if (showLoadMore) View.VISIBLE else View.GONE
    }

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
    }

    internal fun reloadEvents() {
        setEvents(viewModel.events.value ?: emptyList())
    }

    private fun setEvents(events: List<ActivityLogModel>) {
        val adapter: ActivityLogAdapter
        if (activityLogList.adapter == null) {
            adapter = ActivityLogAdapter(context!!)
            activityLogList.adapter = adapter
        } else {
            adapter = activityLogList.adapter as ActivityLogAdapter
        }
        adapter.setEvents(events)
    }

    inner class ActivityLogAdapter(context: Context) : RecyclerView.Adapter<ActivityLogViewHolder>() {
        private val list = ArrayList<ActivityLogModel>()
        private var layoutInflater: LayoutInflater = LayoutInflater.from(context)

        override fun onBindViewHolder(holder: ActivityLogViewHolder, position: Int) {
            holder.bind(getItem(position))

            if (position == itemCount - 1) {
                viewModel.loadMore()
            }

            if (position % 4 == 0) {
                holder.header.visibility = View.VISIBLE
            }
        }

        init {
            setHasStableIds(true)
        }

        internal fun setEvents(items: List<ActivityLogModel>) {
            list.clear()
            list.addAll(items.toTypedArray())
            notifyDataSetChanged()
        }

        protected fun getItem(position: Int): ActivityLogModel {
            return list[position]
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun getItemId(position: Int): Long {
            return list[position].activityID.hashCode().toLong()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityLogViewHolder {
            val view = layoutInflater.inflate(R.layout.activity_log_list_item, parent, false) as ViewGroup
            return ActivityLogViewHolder(view)
        }
    }
}
