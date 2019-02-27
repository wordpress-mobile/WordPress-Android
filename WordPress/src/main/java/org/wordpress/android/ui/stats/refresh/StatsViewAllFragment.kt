package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.stats_date_selector.*
import kotlinx.android.synthetic.main.stats_date_selector.view.*
import kotlinx.android.synthetic.main.stats_view_all_fragment.*
import org.wordpress.android.R
import org.wordpress.android.R.dimen
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.ui.stats.StatsAbstractFragment
import org.wordpress.android.ui.stats.StatsViewType
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock
import org.wordpress.android.ui.stats.refresh.lists.StatsBlockAdapter
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsNavigator
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.observeEvent
import javax.inject.Inject

class StatsViewAllFragment : DaggerFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var statsDateFormatter: StatsDateFormatter
    @Inject lateinit var navigator: StatsNavigator
    private lateinit var viewModel: StatsViewAllViewModel
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper

    private val listStateKey = "list_state"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.stats_view_all_fragment, container, false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        recyclerView.layoutManager?.let {
            outState.putParcelable(listStateKey, it.onSaveInstanceState())
        }

        val intent = activity?.intent
        if (intent != null) {
            if (intent.hasExtra(WordPress.SITE)) {
                outState.putSerializable(WordPress.SITE, intent.getSerializableExtra(WordPress.SITE))
            }
            if (intent.hasExtra(StatsAbstractFragment.ARGS_VIEW_TYPE)) {
                outState.putSerializable(StatsAbstractFragment.ARGS_VIEW_TYPE,
                        intent.getSerializableExtra(StatsAbstractFragment.ARGS_VIEW_TYPE))
            }
            if (intent.hasExtra(StatsAbstractFragment.ARGS_TIMEFRAME)) {
                outState.putSerializable(StatsAbstractFragment.ARGS_TIMEFRAME,
                        intent.getSerializableExtra(StatsAbstractFragment.ARGS_TIMEFRAME))
            }
        }

        super.onSaveInstanceState(outState)
    }

    private fun initializeViews(savedInstanceState: Bundle?) {
        recyclerView.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)

        savedInstanceState?.getParcelable<Parcelable>(listStateKey)?.let {
            recyclerView.layoutManager.onRestoreInstanceState(it)
        }

        recyclerView.addItemDecoration(
                StatsListItemDecoration(
                        resources.getDimensionPixelSize(dimen.margin_small),
                        resources.getDimensionPixelSize(dimen.margin_small),
                        1
                )
        )

        swipeToRefreshHelper = WPSwipeToRefreshHelper.buildSwipeToRefreshHelper(pullToRefresh) {
            viewModel.onPullToRefresh()
        }

        select_next_date.setOnClickListener {
            viewModel.onNextDateSelected()
        }
        select_previous_date.setOnClickListener {
            viewModel.onPreviousDateSelected()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = checkNotNull(activity)

        initializeViews(savedInstanceState)
        initializeViewModels(nonNullActivity, savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        (activity as AppCompatActivity).supportActionBar?.title = getString(viewModel.title)
    }

    private fun initializeViewModels(activity: FragmentActivity, savedInstanceState: Bundle?) {
        val site = if (savedInstanceState == null) {
            val nonNullIntent = checkNotNull(activity.intent)
            nonNullIntent.getSerializableExtra(WordPress.SITE) as SiteModel
        } else {
            savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
        }

        val type = if (savedInstanceState == null) {
            val nonNullIntent = checkNotNull(activity.intent)
            nonNullIntent.getSerializableExtra(StatsAbstractFragment.ARGS_VIEW_TYPE) as StatsViewType
        } else {
            savedInstanceState.getSerializable(StatsAbstractFragment.ARGS_VIEW_TYPE) as StatsViewType
        }

        val granularity = if (savedInstanceState == null) {
            val nonNullIntent = checkNotNull(activity.intent)
            nonNullIntent.getSerializableExtra(StatsAbstractFragment.ARGS_TIMEFRAME) as StatsGranularity?
        } else {
            savedInstanceState.getSerializable(StatsAbstractFragment.ARGS_TIMEFRAME) as StatsGranularity?
        }

        val viewModelType = StatsViewAllViewModel.get(type, granularity)
        viewModel = ViewModelProviders.of(activity, viewModelFactory).get(viewModelType)
        setupObservers(site, activity)
        viewModel.start(site, granularity)
    }

    private fun setupObservers(site: SiteModel, activity: FragmentActivity) {
        viewModel.isRefreshing.observe(this, Observer {
            it?.let { isRefreshing ->
                swipeToRefreshHelper.isRefreshing = isRefreshing
            }
        })

        viewModel.data.observe(this, Observer {
            if (it != null) {
                updateInsights(it)
            }
        })

        viewModel.navigationTarget.observeEvent(this) { target ->
            navigator.navigate(site, activity, target)
            return@observeEvent true
        }

        viewModel.dateSelectorUiModel.observe(this, Observer { dateSelectorUiModel ->
            val dateSelectorVisibility = if (dateSelectorUiModel?.isVisible == true) View.VISIBLE else View.GONE
            if (dateSelectionToolbar.visibility != dateSelectorVisibility) {
                dateSelectionToolbar.visibility = dateSelectorVisibility
            }
            selected_date.text = dateSelectorUiModel?.date ?: ""
            val enablePreviousButton = dateSelectorUiModel?.enableSelectPrevious == true
            if (dateSelectionToolbar.select_previous_date.isEnabled != enablePreviousButton) {
                dateSelectionToolbar.select_previous_date.isEnabled = enablePreviousButton
            }
            val enableNextButton = dateSelectorUiModel?.enableSelectNext == true
            if (dateSelectionToolbar.select_next_date.isEnabled != enableNextButton) {
                dateSelectionToolbar.select_next_date.isEnabled = enableNextButton
            }
        })

        viewModel.selectedDateChanged.observe(this, Observer {
            viewModel.onSelectedDateChange()
        })
    }

    private fun updateInsights(statsState: List<StatsBlock>) {
        val adapter: StatsBlockAdapter
        if (recyclerView.adapter == null) {
            adapter = StatsBlockAdapter(imageManager)
            recyclerView.adapter = adapter
        } else {
            adapter = recyclerView.adapter as StatsBlockAdapter
        }

        val recyclerViewState = recyclerView?.layoutManager?.onSaveInstanceState()
        adapter.update(statsState)
        recyclerView?.layoutManager?.onRestoreInstanceState(recyclerViewState)
    }
}
