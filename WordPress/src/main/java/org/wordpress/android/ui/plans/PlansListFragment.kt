package org.wordpress.android.ui.plans

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.PlansListFragmentBinding
import org.wordpress.android.fluxc.model.plans.PlanOffersModel
import org.wordpress.android.ui.plans.PlansViewModel.PlansListStatus.ERROR
import org.wordpress.android.ui.plans.PlansViewModel.PlansListStatus.ERROR_WITH_CACHE
import org.wordpress.android.ui.plans.PlansViewModel.PlansListStatus.FETCHING
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import javax.inject.Inject

class PlansListFragment : Fragment() {
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper
    private lateinit var viewModel: PlansViewModel

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    interface PlansListInterface {
        fun onPlanItemClicked(plan: PlanOffersModel)
        fun onPlansUpdating()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.plans_list_fragment, container, false)
    }

    private fun onItemClicked(item: PlanOffersModel) {
        viewModel.onItemClicked(item)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = requireActivity()
        with(PlansListFragmentBinding.bind(view)) {
            emptyRecyclerView.layoutManager = LinearLayoutManager(nonNullActivity, RecyclerView.VERTICAL, false)
            emptyRecyclerView.setEmptyView(actionableEmptyView)

            swipeToRefreshHelper = WPSwipeToRefreshHelper.buildSwipeToRefreshHelper(swipeRefreshLayout) {
                if (NetworkUtils.checkConnection(nonNullActivity)) {
                    viewModel.onPullToRefresh()
                } else {
                    swipeToRefreshHelper.isRefreshing = false
                }
            }

            (nonNullActivity.application as WordPress).component().inject(this@PlansListFragment)

            viewModel = ViewModelProvider(this@PlansListFragment, viewModelFactory).get(PlansViewModel::class.java)
            setObservers()
            viewModel.create()
        }
    }

    private fun PlansListFragmentBinding.reloadList(data: List<PlanOffersModel>) {
        setList(data)
    }

    private fun PlansListFragmentBinding.setList(list: List<PlanOffersModel>) {
        val adapter: PlansListAdapter

        if (emptyRecyclerView.adapter == null) {
            adapter = PlansListAdapter(requireActivity(), this@PlansListFragment::onItemClicked)
            emptyRecyclerView.adapter = adapter
        } else {
            adapter = emptyRecyclerView.adapter as PlansListAdapter
        }

        adapter.updateList(list)
    }

    private fun PlansListFragmentBinding.setObservers() {
        viewModel.plans.observe(viewLifecycleOwner, Observer {
            reloadList(it ?: emptyList())
        })

        viewModel.listStatus.observe(viewLifecycleOwner, Observer { listStatus ->
            if (isAdded && view != null) {
                swipeToRefreshHelper.isRefreshing = listStatus == FETCHING
            }

            when (listStatus) {
                ERROR -> {
                    actionableEmptyView.title.text = getString(R.string.plans_loading_error_network_title)
                    actionableEmptyView.subtitle.text = getString(R.string.plans_loading_error_no_cache_subtitle)
                    actionableEmptyView.button.visibility = View.GONE
                }
                ERROR_WITH_CACHE -> {
                    actionableEmptyView.title.text = getString(R.string.plans_loading_error_network_title)
                    actionableEmptyView.subtitle.text = getString(R.string.plans_loading_error_with_cache_subtitle)
                    actionableEmptyView.button.visibility = View.VISIBLE
                    actionableEmptyView.button.setOnClickListener {
                        viewModel.onShowCachedPlansButtonClicked()
                    }
                }
                FETCHING -> {
                    if (activity is PlansListInterface) {
                        (activity as PlansListInterface).onPlansUpdating()
                    }
                }
                else -> {
                    // show generic error in case there are no plans to show for any reason
                    actionableEmptyView.title.text = getString(R.string.plans_loading_error_no_plans_title)
                    actionableEmptyView.subtitle.text = getString(R.string.plans_loading_error_no_plans_subtitle)
                    actionableEmptyView.button.visibility = View.GONE
                }
            }
        })

        viewModel.showDialog.observe(viewLifecycleOwner, Observer {
            if (it is PlanOffersModel && activity is PlansListInterface) {
                (activity as PlansListInterface).onPlanItemClicked(it)
            }
        })
    }
}
