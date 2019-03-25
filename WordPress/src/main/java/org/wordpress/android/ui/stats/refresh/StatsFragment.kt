package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.design.widget.TabLayout.OnTabSelectedListener
import android.support.design.widget.TabLayout.Tab
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewCompat
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.stats_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.stats.OldStatsActivity.ARG_DESIRED_TIMEFRAME
import org.wordpress.android.ui.stats.OldStatsActivity.ARG_LAUNCHED_FROM
import org.wordpress.android.ui.stats.OldStatsActivity.StatsLaunchedFrom
import org.wordpress.android.ui.stats.StatsTimeframe
import org.wordpress.android.ui.stats.StatsTimeframe.DAY
import org.wordpress.android.ui.stats.StatsTimeframe.MONTH
import org.wordpress.android.ui.stats.StatsTimeframe.WEEK
import org.wordpress.android.ui.stats.StatsTimeframe.YEAR
import org.wordpress.android.ui.stats.refresh.lists.StatsListFragment
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.DAYS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.INSIGHTS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.MONTHS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.WEEKS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.YEARS
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import javax.inject.Inject

private val statsSections = listOf(INSIGHTS, DAYS, WEEKS, MONTHS, YEARS)

class StatsFragment : DaggerFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: StatsViewModel
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper

    private var restorePreviousSearch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.stats_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = checkNotNull(activity)

        initializeViewModels(nonNullActivity, savedInstanceState == null)
        initializeViews(nonNullActivity)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    }

    private fun initializeViews(activity: FragmentActivity) {
        statsPager.adapter = StatsPagerAdapter(activity, childFragmentManager)
        tabLayout.setupWithViewPager(statsPager)
        statsPager.pageMargin = resources.getDimensionPixelSize(R.dimen.margin_extra_large)
        statsPager.setCurrentItem(viewModel.getSelectedSection().ordinal, false)
        tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabReselected(tab: Tab?) {
            }

            override fun onTabUnselected(tab: Tab?) {
            }

            override fun onTabSelected(tab: Tab) {
                viewModel.onSectionSelected(statsSections[tab.position])
            }
        })

        swipeToRefreshHelper = WPSwipeToRefreshHelper.buildSwipeToRefreshHelper(pullToRefresh) {
            viewModel.onPullToRefresh()
        }
    }

    private fun initializeViewModels(activity: FragmentActivity, isFirstStart: Boolean) {
        viewModel = ViewModelProviders.of(activity, viewModelFactory).get(StatsViewModel::class.java)

        setupObservers(activity)

        val site = activity.intent?.getSerializableExtra(WordPress.SITE) as SiteModel?
        val nonNullSite = checkNotNull(site)

        val launchedFrom = activity.intent.getSerializableExtra(ARG_LAUNCHED_FROM)
        val launchedFromWidget = launchedFrom == StatsLaunchedFrom.STATS_WIDGET
        val initialTimeFrame = getInitialTimeFrame(activity)

        viewModel.start(nonNullSite, launchedFromWidget, initialTimeFrame)

        if (!isFirstStart) {
            restorePreviousSearch = true
        }

        statsPager.setOnTouchListener { _, event ->
            swipeToRefreshHelper.setEnabled(false)
            if (event.action == MotionEvent.ACTION_UP) {
                swipeToRefreshHelper.setEnabled(true)
            }
            return@setOnTouchListener false
        }
    }

    private fun getInitialTimeFrame(activity: FragmentActivity): StatsSection? {
        val initialTimeFrame = activity.intent.getSerializableExtra(ARG_DESIRED_TIMEFRAME)
        return when (initialTimeFrame) {
            StatsTimeframe.INSIGHTS -> INSIGHTS
            DAY -> DAYS
            WEEK -> WEEKS
            MONTH -> MONTHS
            YEAR -> YEARS
            else -> null
        }
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

        viewModel.toolbarHasShadow.observe(this, Observer { hasShadow ->
            app_bar_layout.postDelayed(
                    {
                        if (app_bar_layout != null) {
                            val elevation = if (hasShadow == true) {
                                resources.getDimension(R.dimen.appbar_elevation)
                            } else {
                                0f
                            }
                            ViewCompat.setElevation(app_bar_layout, elevation)
                        }
                    },
                    100
            )
        })

        viewModel.siteChanged.observe(this, Observer {
            viewModel.refreshData()
        })
    }
}

class StatsPagerAdapter(val context: Context, val fm: FragmentManager) : FragmentPagerAdapter(fm) {
    override fun getCount(): Int = statsSections.size

    override fun getItem(position: Int): Fragment {
        return StatsListFragment.newInstance(statsSections[position])
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return context.getString(statsSections[position].titleRes)
    }
}
