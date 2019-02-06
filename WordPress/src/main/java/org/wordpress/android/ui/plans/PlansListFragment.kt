package org.wordpress.android.ui.plans

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.history_list_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.plans.PlanOffersModel
import org.wordpress.android.ui.plans.PlansViewModel.PlanOffersListStatus.FETCHING
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import javax.inject.Inject

class PlansListFragment : Fragment() {
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper
    private lateinit var viewModel: PlansViewModel
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    companion object {
        fun newInstance(): PlansListFragment {
            return PlansListFragment()
        }
    }

    interface PlanOffersItemClickInterface {
        fun onPlanItemClicked(plan: PlanOffersModel)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.plans_list_fragment, container, false)
    }

    private fun onItemClicked(item: PlanOffersModel) {
        viewModel.onItemClicked(item)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = checkNotNull(activity)

        empty_recycler_view.layoutManager = LinearLayoutManager(nonNullActivity, LinearLayoutManager.VERTICAL, false)
        empty_recycler_view.setEmptyView(actionable_empty_view)

        swipeToRefreshHelper = WPSwipeToRefreshHelper.buildSwipeToRefreshHelper(swipe_refresh_layout) {
            if (NetworkUtils.checkConnection(nonNullActivity)) {
                viewModel.onPullToRefresh()
            } else {
                swipeToRefreshHelper.isRefreshing = false
            }
        }

        (nonNullActivity.application as WordPress).component()?.inject(this)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(PlansViewModel::class.java)
        setObservers()
        viewModel.create()
    }

    private fun reloadList(data: List<PlanOffersModel>) {
        setList(data)
    }

    private fun setList(list: List<PlanOffersModel>) {
        val adapter: PlansListAdapter

        if (empty_recycler_view.adapter == null) {
            adapter = PlansListAdapter(checkNotNull(activity), this::onItemClicked)
            empty_recycler_view.adapter = adapter
        } else {
            adapter = empty_recycler_view.adapter as PlansListAdapter
        }

        adapter.updateList(list)
    }

    private fun setObservers() {
        viewModel.planOffers.observe(this, Observer {
            reloadList(it ?: emptyList())
        })

        viewModel.listStatus.observe(this, Observer { listStatus ->
            if (isAdded && view != null) {
                swipeToRefreshHelper.isRefreshing = listStatus == FETCHING
            }
        })

        viewModel.showDialog.observe(this, Observer {
            if (it is PlanOffersModel && activity is PlanOffersItemClickInterface) {
                (activity as PlanOffersItemClickInterface).onPlanItemClicked(it)
            }
        })
    }
}
