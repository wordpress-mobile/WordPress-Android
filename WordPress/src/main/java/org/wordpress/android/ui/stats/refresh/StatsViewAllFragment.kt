package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.os.Parcelable
import android.support.design.widget.AppBarLayout
import android.support.design.widget.AppBarLayout.LayoutParams
import android.support.design.widget.Snackbar
import android.support.design.widget.TabLayout.OnTabSelectedListener
import android.support.design.widget.TabLayout.Tab
import android.support.v4.app.FragmentActivity
import android.support.v4.view.ViewCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.stats_date_selector.*
import kotlinx.android.synthetic.main.stats_empty_view.*
import kotlinx.android.synthetic.main.stats_error_view.*
import kotlinx.android.synthetic.main.stats_list_fragment.*
import kotlinx.android.synthetic.main.stats_view_all_fragment.*
import org.wordpress.android.R
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.ui.stats.StatsAbstractFragment
import org.wordpress.android.ui.stats.StatsViewType
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.LOADING
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListAdapter
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TabsItem
import org.wordpress.android.ui.stats.refresh.utils.StatsNavigator
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.observeEvent
import javax.inject.Inject

class StatsViewAllFragment : DaggerFragment() {
    @Inject lateinit var viewModelFactoryBuilder: StatsViewAllViewModelFactory.Builder
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var navigator: StatsNavigator
    private lateinit var viewModel: StatsViewAllViewModel
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper

    private val listStateKey = "list_state"

    companion object {
        const val SELECTED_TAB_KEY = "selected_tab_key"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.stats_view_all_fragment, container, false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        recyclerView.layoutManager?.let {
            outState.putParcelable(listStateKey, it.onSaveInstanceState())
        }

        val intent = activity?.intent
        if (intent != null) {
            if (intent.hasExtra(StatsAbstractFragment.ARGS_VIEW_TYPE)) {
                outState.putSerializable(
                        StatsAbstractFragment.ARGS_VIEW_TYPE,
                        intent.getSerializableExtra(StatsAbstractFragment.ARGS_VIEW_TYPE)
                )
            }
            if (intent.hasExtra(StatsAbstractFragment.ARGS_TIMEFRAME)) {
                outState.putSerializable(
                        StatsAbstractFragment.ARGS_TIMEFRAME,
                        intent.getSerializableExtra(StatsAbstractFragment.ARGS_TIMEFRAME)
                )
            }
        }

        super.onSaveInstanceState(outState)
    }

    private fun initializeViews(savedInstanceState: Bundle?) {
        val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)

        savedInstanceState?.getParcelable<Parcelable>(listStateKey)?.let {
            layoutManager.onRestoreInstanceState(it)
        }

        recyclerView.layoutManager = layoutManager
        loadingRecyclerView.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)

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

        val viewModelFactory = viewModelFactoryBuilder.build(type, granularity)
        viewModel = ViewModelProviders.of(activity, viewModelFactory).get(StatsViewAllViewModel::class.java)
        setupObservers(activity)
        viewModel.start()
    }

    private fun setupObservers(activity: FragmentActivity) {
        viewModel.isRefreshing.observe(this, Observer {
            it?.let { isRefreshing ->
                swipeToRefreshHelper.isRefreshing = isRefreshing
            }
        })

        viewModel.showSnackbarMessage.observe(this, Observer { holder ->
            val parent = activity.findViewById<View>(R.id.coordinatorLayout)
            if (holder != null && parent != null) {
                if (holder.buttonTitleRes == null) {
                    Snackbar.make(parent, getString(holder.messageRes), Snackbar.LENGTH_LONG).show()
                } else {
                    val snackbar = Snackbar.make(parent, getString(holder.messageRes), Snackbar.LENGTH_LONG)
                    snackbar.setAction(getString(holder.buttonTitleRes)) { holder.buttonAction() }
                    snackbar.show()
                }
            }
        })

        viewModel.data.observe(this, Observer {
            if (it != null) {
                recyclerView.visibility = if (it is StatsBlock.Success) View.VISIBLE else View.GONE
                loadingContainer.visibility = if (it is StatsBlock.Loading) View.VISIBLE else View.GONE
                actionable_error_view.visibility = if (it is StatsBlock.Error) View.VISIBLE else View.GONE
                actionable_empty_view.visibility = if (it is StatsBlock.EmptyBlock) View.VISIBLE else View.GONE
                when (it) {
                    is StatsBlock.Success -> {
                        loadData(recyclerView, prepareLayout(it.data, it.type))
                    }
                    is StatsBlock.Loading -> {
                        loadData(loadingRecyclerView, prepareLayout(it.data, it.type))
                    }
                    is StatsBlock.Error -> {
                        actionable_error_view.button.setOnClickListener {
                            viewModel.onRetryClick()
                        }
                    }
                }
            }
        })

        viewModel.navigationTarget.observeEvent(this) { target ->
            navigator.navigate(activity, target)
            return@observeEvent true
        }

        viewModel.dateSelectorData.observe(this, Observer { dateSelectorUiModel ->
            val dateSelectorVisibility = if (dateSelectorUiModel?.isVisible == true) View.VISIBLE else View.GONE
            if (date_selection_toolbar.visibility != dateSelectorVisibility) {
                date_selection_toolbar.visibility = dateSelectorVisibility
            }
            selected_date.text = dateSelectorUiModel?.date ?: ""
            val enablePreviousButton = dateSelectorUiModel?.enableSelectPrevious == true
            if (select_previous_date.isEnabled != enablePreviousButton) {
                select_previous_date.isEnabled = enablePreviousButton
            }
            val enableNextButton = dateSelectorUiModel?.enableSelectNext == true
            if (select_next_date.isEnabled != enableNextButton) {
                select_next_date.isEnabled = enableNextButton
            }
        })

        viewModel.navigationTarget.observeEvent(this) { target ->
            navigator.navigate(activity, target)
            return@observeEvent true
        }

        viewModel.selectedDate.observeEvent(this) {
            viewModel.onDateChanged()
            true
        }

        viewModel.toolbarHasShadow.observe(this, Observer { hasShadow ->
            val elevation = if (hasShadow == true) resources.getDimension(R.dimen.appbar_elevation) else 0f
            app_bar_layout.postDelayed({ ViewCompat.setElevation(app_bar_layout, elevation) }, 100)
        })
    }

    private fun loadData(recyclerView: RecyclerView, data: List<BlockListItem>) {
        val adapter: BlockListAdapter
        if (recyclerView.adapter == null) {
            adapter = BlockListAdapter(imageManager)
            recyclerView.adapter = adapter
        } else {
            adapter = recyclerView.adapter as BlockListAdapter
        }

        val recyclerViewState = recyclerView.layoutManager?.onSaveInstanceState()
        adapter.update(data)
        recyclerView.layoutManager?.onRestoreInstanceState(recyclerViewState)
    }

    private fun prepareLayout(data: List<BlockListItem>, type: StatsBlock.Type): List<BlockListItem> {
        val tabs = data.firstOrNull { it is TabsItem } as? TabsItem
        return if (tabs != null) {
            if (tabLayout.tabCount == 0) {
                setupTabs(tabs)
            } else if (tabLayout.selectedTabPosition != tabs.selectedTabPosition) {
                tabLayout.getTabAt(tabs.selectedTabPosition)?.select()
            }

            (toolbar.layoutParams as AppBarLayout.LayoutParams).scrollFlags =
                    AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL.or(AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS)
            tabLayout.visibility = View.VISIBLE

            data.filter { it !is TabsItem }
        } else {
            if (type != LOADING) {
                (toolbar.layoutParams as LayoutParams).scrollFlags = 0
                tabLayout.visibility = View.GONE
            }
            data
        }
    }

    private fun setupTabs(item: TabsItem) {
        tabLayout.clearOnTabSelectedListeners()
        tabLayout.removeAllTabs()
        item.tabs.forEach { tabItem ->
            tabLayout.addTab(tabLayout.newTab().setText(tabItem))
        }

        tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabReselected(tab: Tab) {
            }

            override fun onTabUnselected(tab: Tab) {
            }

            override fun onTabSelected(tab: Tab) {
                item.onTabSelected(tab.position)
                activity?.intent?.putExtra(StatsViewAllFragment.SELECTED_TAB_KEY, tab.position)
            }
        })

        val selectedTab = activity?.intent?.getIntExtra(StatsViewAllFragment.SELECTED_TAB_KEY, 0) ?: 0
        tabLayout.getTabAt(selectedTab)?.select()
    }
}
