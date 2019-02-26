package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.os.Parcelable
import android.support.design.widget.AppBarLayout
import android.support.design.widget.AppBarLayout.LayoutParams
import android.support.design.widget.Snackbar
import android.support.design.widget.TabLayout.OnTabSelectedListener
import android.support.design.widget.TabLayout.Tab
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView.LayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.stats_view_all_fragment.*
import org.wordpress.android.R
import org.wordpress.android.R.dimen
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.stats.StatsAbstractFragment
import org.wordpress.android.ui.stats.StatsViewType
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Success
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.LOADING
import org.wordpress.android.ui.stats.refresh.lists.StatsBlockAdapter
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.UiModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TabsItem
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

    private var layoutManager: LayoutManager? = null

    private val listStateKey = "list_state"

    companion object {
        private const val typeKey = "type_key"

        fun newInstance(statsType: StatsViewType): StatsViewAllFragment {
            val fragment = StatsViewAllFragment()
            val bundle = Bundle()
            bundle.putSerializable(typeKey, statsType)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.stats_view_all_fragment, container, false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        layoutManager?.let {
            outState.putParcelable(listStateKey, it.onSaveInstanceState())
        }

        val intent = activity?.intent
        if (intent != null) {
            if (intent.hasExtra(WordPress.SITE)) {
                outState.putSerializable(WordPress.SITE, intent.getSerializableExtra(WordPress.SITE))
            }
            if (intent.hasExtra(WordPress.SITE)) {
                outState.putSerializable(StatsAbstractFragment.ARGS_VIEW_TYPE,
                        intent.getSerializableExtra(StatsAbstractFragment.ARGS_VIEW_TYPE))
            }
        }

        super.onSaveInstanceState(outState)
    }

    private fun initializeViews(savedInstanceState: Bundle?) {
        val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)

        savedInstanceState?.getParcelable<Parcelable>(listStateKey)?.let {
            layoutManager.onRestoreInstanceState(it)
        }

        this.layoutManager = layoutManager
        recyclerView.layoutManager = this.layoutManager
        recyclerView.addItemDecoration(
                StatsListItemDecoration(
                        resources.getDimensionPixelSize(dimen.stats_list_card_horizontal_spacing),
                        resources.getDimensionPixelSize(dimen.stats_list_card_top_spacing),
                        resources.getDimensionPixelSize(dimen.stats_list_card_bottom_spacing),
                        resources.getDimensionPixelSize(dimen.stats_list_card_first_spacing),
                        resources.getDimensionPixelSize(dimen.stats_list_card_last_spacing),
                        1
                )
        )

        swipeToRefreshHelper = WPSwipeToRefreshHelper.buildSwipeToRefreshHelper(pullToRefresh) {
            viewModel.onPullToRefresh()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = checkNotNull(activity)

        initializeViews(savedInstanceState)
        initializeViewModels(nonNullActivity, savedInstanceState)
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

        val clazz = when (type) {
            StatsViewType.FOLLOWERS -> StatsViewAllFollowersViewModel::class.java
            StatsViewType.COMMENTS -> StatsViewAllCommentsViewModel::class.java
            StatsViewType.TAGS_AND_CATEGORIES -> StatsViewAllTagsAndCategoriesViewModel::class.java
            StatsViewType.INSIGHTS_ALL_TIME -> TODO()
            StatsViewType.INSIGHTS_LATEST_POST_SUMMARY -> TODO()
            StatsViewType.INSIGHTS_MOST_POPULAR -> TODO()
            StatsViewType.INSIGHTS_TODAY -> TODO()
            StatsViewType.PUBLICIZE -> TODO()
            StatsViewType.TOP_POSTS_AND_PAGES -> TODO()
            StatsViewType.REFERRERS -> TODO()
            StatsViewType.CLICKS -> TODO()
            StatsViewType.AUTHORS -> TODO()
            StatsViewType.GEOVIEWS -> TODO()
            StatsViewType.SEARCH_TERMS -> TODO()
            StatsViewType.VIDEO_PLAYS -> TODO()
            else -> throw IllegalStateException("View all screen: Unsupported use case type: ${type.name}")
        }

        viewModel = ViewModelProviders.of(activity, viewModelFactory).get(clazz)
        setupObservers(site, activity)
        viewModel.start(site)
    }

    private fun setupObservers(site: SiteModel, activity: FragmentActivity) {
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

        viewModel.uiModel.observe(this, Observer {
            if (it != null) {
                when (it) {
                    is UiModel.Success -> {
                        if (it.data.isNotEmpty()) {
                            displayData(it.data.first())
                        }
                    }
                    is UiModel.Error -> {
                        recyclerView.visibility = View.GONE
                    }
                }
            }
        })

        viewModel.navigationTarget.observeEvent(this) { target ->
            navigator.navigate(site, activity, target)
            return@observeEvent true
        }
    }

    private fun displayData(statsBlock: StatsBlock) {
        recyclerView.visibility = View.VISIBLE
        val adapter: StatsBlockAdapter
        if (recyclerView.adapter == null) {
            adapter = StatsBlockAdapter(imageManager)
            recyclerView.adapter = adapter
        } else {
            adapter = recyclerView.adapter as StatsBlockAdapter
        }
        val layoutManager = recyclerView?.layoutManager
        val recyclerViewState = layoutManager?.onSaveInstanceState()

        val data = prepareLayout(statsBlock)

        adapter.update(data)
        layoutManager?.onRestoreInstanceState(recyclerViewState)
    }

    private fun prepareLayout(statsBlock: StatsBlock): List<StatsBlock> {
        val tabs = statsBlock.data.firstOrNull { it is TabsItem } as? TabsItem
        return if (tabs != null) {
            if (tabLayout.tabCount == 0) {
                setupTabs(tabs)
            }

            if (tabLayout.selectedTabPosition != tabs.selectedTabPosition) {
                tabLayout.getTabAt(tabs.selectedTabPosition)?.select()
            }

            (toolbar.layoutParams as AppBarLayout.LayoutParams).scrollFlags =
                    AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL.or(AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS)
            tabLayout.visibility = View.VISIBLE

            listOf(Success(statsBlock.statsTypes, statsBlock.data.filter { it !is TabsItem }))
        } else {
            if (statsBlock.type != LOADING) {
                (toolbar.layoutParams as LayoutParams).scrollFlags = 0
                tabLayout.visibility = View.GONE
            }

            listOf(statsBlock)
        }
    }

    private fun setupTabs(item: TabsItem) {
        tabLayout.clearOnTabSelectedListeners()
        tabLayout.removeAllTabs()
        item.tabs.forEach { tabItem ->
            tabLayout.addTab(tabLayout.newTab().setText(tabItem))
        }
        tabLayout.getTabAt(item.selectedTabPosition)?.select()

        tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabReselected(tab: Tab) {
            }

            override fun onTabUnselected(tab: Tab) {
            }

            override fun onTabSelected(tab: Tab) {
                item.onTabSelected(tab.position)
            }
        })
    }
}
