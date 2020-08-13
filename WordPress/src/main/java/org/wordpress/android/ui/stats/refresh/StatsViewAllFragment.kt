package org.wordpress.android.ui.stats.refresh

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout.LayoutParams
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayout.Tab
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.stats_date_selector.*
import kotlinx.android.synthetic.main.stats_empty_view.*
import kotlinx.android.synthetic.main.stats_error_view.*
import kotlinx.android.synthetic.main.stats_list_fragment.*
import kotlinx.android.synthetic.main.stats_view_all_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.ui.stats.StatsViewType
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.LOADING
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListAdapter
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TabsItem
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider.SelectedDate
import org.wordpress.android.ui.stats.refresh.utils.StatsNavigator
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.drawDateSelector
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.widgets.WPSnackbar
import javax.inject.Inject

class StatsViewAllFragment : DaggerFragment() {
    @Inject lateinit var viewModelFactoryBuilder: StatsViewAllViewModelFactory.Builder
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var navigator: StatsNavigator
    @Inject lateinit var statsSiteProvider: StatsSiteProvider
    private lateinit var viewModel: StatsViewAllViewModel
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper

    private val listStateKey = "list_state"

    companion object {
        const val SELECTED_TAB_KEY = "selected_tab_key"
        const val ARGS_VIEW_TYPE = "ARGS_VIEW_TYPE"
        const val ARGS_TIMEFRAME = "ARGS_TIMEFRAME"
        const val ARGS_SELECTED_DATE = "ARGS_SELECTED_DATE"
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
            if (intent.hasExtra(ARGS_VIEW_TYPE)) {
                outState.putSerializable(
                        ARGS_VIEW_TYPE,
                        intent.getSerializableExtra(ARGS_VIEW_TYPE)
                )
            }
            if (intent.hasExtra(ARGS_TIMEFRAME)) {
                outState.putSerializable(
                        ARGS_TIMEFRAME,
                        intent.getSerializableExtra(ARGS_TIMEFRAME)
                )
            }
            outState.putInt(WordPress.LOCAL_SITE_ID, intent.getIntExtra(WordPress.LOCAL_SITE_ID, 0))
        }
        outState.putParcelable(ARGS_SELECTED_DATE, viewModel.getSelectedDate())
        super.onSaveInstanceState(outState)
    }

    private fun initializeViews(savedInstanceState: Bundle?) {
        val layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)

        savedInstanceState?.getParcelable<Parcelable>(listStateKey)?.let {
            layoutManager.onRestoreInstanceState(it)
        }

        recyclerView.layoutManager = layoutManager
        loadingRecyclerView.layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)

        swipeToRefreshHelper = WPSwipeToRefreshHelper.buildSwipeToRefreshHelper(pullToRefresh) {
            viewModel.onPullToRefresh()
        }

        nextDateButton.setOnClickListener {
            viewModel.onNextDateSelected()
        }
        previousDateButton.setOnClickListener {
            viewModel.onPreviousDateSelected()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = requireActivity()

        initializeViews(savedInstanceState)
        initializeViewModels(nonNullActivity, savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        (activity as AppCompatActivity).supportActionBar?.title = getString(viewModel.title)
    }

    private fun initializeViewModels(activity: FragmentActivity, savedInstanceState: Bundle?) {
        val nonNullIntent = checkNotNull(activity.intent)
        val type = if (savedInstanceState == null) {
            nonNullIntent.getSerializableExtra(ARGS_VIEW_TYPE) as StatsViewType
        } else {
            savedInstanceState.getSerializable(ARGS_VIEW_TYPE) as StatsViewType
        }

        val granularity = if (savedInstanceState == null) {
            nonNullIntent.getSerializableExtra(ARGS_TIMEFRAME) as StatsGranularity?
        } else {
            savedInstanceState.getSerializable(ARGS_TIMEFRAME) as StatsGranularity?
        }

        val siteId = savedInstanceState?.getInt(WordPress.LOCAL_SITE_ID, 0)
                ?: nonNullIntent.getIntExtra(WordPress.LOCAL_SITE_ID, 0)
        statsSiteProvider.start(siteId)

        val viewModelFactory = viewModelFactoryBuilder.build(type, granularity)
        viewModel = ViewModelProviders.of(activity, viewModelFactory).get(StatsViewAllViewModel::class.java)

        val selectedDate = if (savedInstanceState == null) {
            nonNullIntent.getParcelableExtra(ARGS_SELECTED_DATE) as SelectedDate?
        } else {
            savedInstanceState.getParcelable(ARGS_SELECTED_DATE) as SelectedDate?
        }
        setupObservers(activity)

        viewModel.start(selectedDate)
    }

    private fun setupObservers(activity: FragmentActivity) {
        viewModel.isRefreshing.observe(viewLifecycleOwner, Observer {
            it?.let { isRefreshing ->
                swipeToRefreshHelper.isRefreshing = isRefreshing
            }
        })

        viewModel.showSnackbarMessage.observe(viewLifecycleOwner, Observer { event ->
            event?.getContentIfNotHandled()?.let { holder ->
                val parent = activity.findViewById<View>(R.id.coordinatorLayout)
                if (parent != null) {
                    if (holder.buttonTitleRes == null) {
                        WPSnackbar.make(parent, getString(holder.messageRes), Snackbar.LENGTH_LONG).show()
                    } else {
                        val snackbar = WPSnackbar.make(parent, getString(holder.messageRes), Snackbar.LENGTH_LONG)
                        snackbar.setAction(getString(holder.buttonTitleRes)) { holder.buttonAction() }
                        snackbar.show()
                    }
                }
            }
        })

        viewModel.data.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                recyclerView.visibility = if (it is StatsBlock.Success) View.VISIBLE else View.GONE
                loadingContainer.visibility = if (it is StatsBlock.Loading) View.VISIBLE else View.GONE
                statsErrorView.visibility = if (it is StatsBlock.Error) View.VISIBLE else View.GONE
                statsEmptyView.visibility = if (it is StatsBlock.EmptyBlock) View.VISIBLE else View.GONE
                when (it) {
                    is StatsBlock.Success -> {
                        loadData(recyclerView, prepareLayout(it.data, it.type))
                    }
                    is StatsBlock.Loading -> {
                        loadData(loadingRecyclerView, prepareLayout(it.data, it.type))
                    }
                    is StatsBlock.Error -> {
                        statsErrorView.button.setOnClickListener {
                            viewModel.onRetryClick()
                        }
                    }
                }
            }
        })
        viewModel.navigationTarget.observe(viewLifecycleOwner, Observer { event ->
            event?.getContentIfNotHandled()?.let { target ->
                navigator.navigate(activity, target)
            }
        })

        viewModel.dateSelectorData.observe(viewLifecycleOwner, Observer { dateSelectorUiModel ->
            drawDateSelector(dateSelectorUiModel)
        })

        viewModel.navigationTarget.observe(viewLifecycleOwner, Observer { event ->
            event?.getContentIfNotHandled()?.let { target ->
                navigator.navigate(activity, target)
            }
        })

        viewModel.selectedDate.observe(viewLifecycleOwner, Observer { event ->
            if (event != null) {
                viewModel.onDateChanged()
            }
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

            (toolbar.layoutParams as LayoutParams).scrollFlags =
                    LayoutParams.SCROLL_FLAG_SCROLL.or(LayoutParams.SCROLL_FLAG_ENTER_ALWAYS)
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
                activity?.intent?.putExtra(SELECTED_TAB_KEY, tab.position)
            }
        })

        val selectedTab = activity?.intent?.getIntExtra(SELECTED_TAB_KEY, 0) ?: 0
        tabLayout.getTabAt(selectedTab)?.select()
    }
}
