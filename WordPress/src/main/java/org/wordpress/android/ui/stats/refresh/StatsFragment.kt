package org.wordpress.android.ui.stats.refresh

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayout.Tab
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.stats_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.ScrollableViewInitializedListener
import org.wordpress.android.ui.stats.refresh.lists.StatsListFragment
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.ANNUAL_STATS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.DAYS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.DETAIL
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.INSIGHTS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.MONTHS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.WEEKS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.YEARS
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider.SiteUpdateResult
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.widgets.WPSnackbar
import javax.inject.Inject

private val statsSections = listOf(INSIGHTS, DAYS, WEEKS, MONTHS, YEARS)

class StatsFragment : DaggerFragment(), ScrollableViewInitializedListener {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: StatsViewModel
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper
    private val selectedTabListener: SelectedTabListener
        get() = SelectedTabListener(viewModel)

    private var restorePreviousSearch = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.stats_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = requireActivity()

        initializeViewModels(nonNullActivity, savedInstanceState == null, savedInstanceState)
        initializeViews(nonNullActivity)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(WordPress.LOCAL_SITE_ID, activity?.intent?.getIntExtra(WordPress.LOCAL_SITE_ID, 0) ?: 0)
        viewModel.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    private fun initializeViews(activity: FragmentActivity) {
        statsPager.adapter = StatsPagerAdapter(activity, childFragmentManager)
        tabLayout.setupWithViewPager(statsPager)
        statsPager.pageMargin = resources.getDimensionPixelSize(R.dimen.margin_extra_large)
        tabLayout.addOnTabSelectedListener(selectedTabListener)

        swipeToRefreshHelper = WPSwipeToRefreshHelper.buildSwipeToRefreshHelper(pullToRefresh) {
            viewModel.onPullToRefresh()
        }
    }

    private fun initializeViewModels(
        activity: FragmentActivity,
        isFirstStart: Boolean,
        savedInstanceState: Bundle?
    ) {
        viewModel = ViewModelProviders.of(activity, viewModelFactory).get(StatsViewModel::class.java)

        viewModel.onRestoreInstanceState(savedInstanceState)

        setupObservers(activity)

        viewModel.start(activity.intent)

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

    private fun setupObservers(activity: FragmentActivity) {
        viewModel.isRefreshing.observe(viewLifecycleOwner, Observer {
            it?.let { isRefreshing ->
                swipeToRefreshHelper.isRefreshing = isRefreshing
            }
        })

        viewModel.showSnackbarMessage.observe(viewLifecycleOwner, Observer { holder ->
            val parent = activity.findViewById<View>(R.id.coordinatorLayout)
            if (holder != null && parent != null) {
                if (holder.buttonTitleRes == null) {
                    WPSnackbar.make(parent, getString(holder.messageRes), Snackbar.LENGTH_LONG).show()
                } else {
                    val snackbar = WPSnackbar.make(parent, getString(holder.messageRes), Snackbar.LENGTH_LONG)
                    snackbar.setAction(getString(holder.buttonTitleRes)) { holder.buttonAction() }
                    snackbar.show()
                }
            }
        })

        viewModel.siteChanged.observe(viewLifecycleOwner, Observer { siteChangedEvent ->
            siteChangedEvent?.applyIfNotHandled {
                when (this) {
                    is SiteUpdateResult.SiteConnected -> viewModel.onSiteChanged()
                    is SiteUpdateResult.NotConnectedJetpackSite -> getActivity()?.finish()
                }
            }
        })

        viewModel.hideToolbar.observe(viewLifecycleOwner, Observer { event ->
            event?.getContentIfNotHandled()?.let { hideToolbar ->
                app_bar_layout.setExpanded(!hideToolbar, true)
            }
        })

        viewModel.selectedSection.observe(viewLifecycleOwner, Observer { selectedSection ->
            selectedSection?.let {
                val position = when (selectedSection) {
                    INSIGHTS -> 0
                    DAYS -> 1
                    WEEKS -> 2
                    MONTHS -> 3
                    YEARS -> 4
                    DETAIL -> null
                    ANNUAL_STATS -> null
                }
                position?.let {
                    if (statsPager.currentItem != position) {
                        tabLayout.removeOnTabSelectedListener(selectedTabListener)
                        statsPager.setCurrentItem(position, false)
                        tabLayout.addOnTabSelectedListener(selectedTabListener)
                    }
                }
            }
        })
    }

    override fun onScrollableViewInitialized(containerId: Int) {
        app_bar_layout.liftOnScrollTargetViewId = containerId
    }
}

class StatsPagerAdapter(val context: Context, val fm: FragmentManager) : FragmentPagerAdapter(
        fm,
        BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
) {
    override fun getCount(): Int = statsSections.size

    override fun getItem(position: Int): Fragment {
        return StatsListFragment.newInstance(statsSections[position])
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return context.getString(statsSections[position].titleRes)
    }
}

private class SelectedTabListener(val viewModel: StatsViewModel) : OnTabSelectedListener {
    override fun onTabReselected(tab: Tab?) {
    }

    override fun onTabUnselected(tab: Tab?) {
    }

    override fun onTabSelected(tab: Tab) {
        viewModel.onSectionSelected(statsSections[tab.position])
    }
}
