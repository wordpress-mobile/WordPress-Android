package org.wordpress.android.ui.stats.refresh

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.MarginPageTransformer
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayout.Tab
import com.google.android.material.tabs.TabLayoutMediator
import dagger.android.support.DaggerFragment
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.StatsFragmentBinding
import org.wordpress.android.ui.ScrollableViewInitializedListener
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.stats.refresh.StatsViewModel.StatsModuleUiModel
import org.wordpress.android.ui.stats.refresh.lists.StatsListFragment
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.ANNUAL_STATS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.DAYS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.DETAIL
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.INSIGHTS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.MONTHS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.WEEKS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.YEARS
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider.SiteUpdateResult
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.viewmodel.observeEvent
import org.wordpress.android.widgets.WPSnackbar
import javax.inject.Inject

private val statsSections = listOf(INSIGHTS, DAYS, WEEKS, MONTHS, YEARS)

class StatsFragment : DaggerFragment(R.layout.stats_fragment), ScrollableViewInitializedListener {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiHelpers: UiHelpers
    private lateinit var viewModel: StatsViewModel
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper
    private val selectedTabListener: SelectedTabListener
        get() = SelectedTabListener(viewModel)

    private var restorePreviousSearch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = requireActivity()
        with(StatsFragmentBinding.bind(view)) {
            with(nonNullActivity as AppCompatActivity) {
                setSupportActionBar(toolbar)
                supportActionBar?.let {
                    it.setHomeButtonEnabled(true)
                    it.setDisplayHomeAsUpEnabled(true)
                }
            }
            initializeViewModels(nonNullActivity, savedInstanceState == null, savedInstanceState)
            initializeViews(nonNullActivity)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(WordPress.LOCAL_SITE_ID, activity?.intent?.getIntExtra(WordPress.LOCAL_SITE_ID, 0) ?: 0)
        viewModel.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    private fun StatsFragmentBinding.initializeViews(activity: FragmentActivity) {
        val adapter = StatsPagerAdapter(activity)
        statsPager.adapter = adapter
        statsPager.setPageTransformer(
                MarginPageTransformer(resources.getDimensionPixelSize(R.dimen.margin_extra_large))
        )
        TabLayoutMediator(tabLayout, statsPager) { tab, position ->
            tab.text = adapter.getTabTitle(position)
        }.attach()
        tabLayout.addOnTabSelectedListener(selectedTabListener)

        swipeToRefreshHelper = WPSwipeToRefreshHelper.buildSwipeToRefreshHelper(pullToRefresh) {
            viewModel.onPullToRefresh()
        }
        disabledView.statsDisabledView.button.setOnClickListener {
            viewModel.onEnableStatsModuleClick()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun StatsFragmentBinding.initializeViewModels(
        activity: FragmentActivity,
        isFirstStart: Boolean,
        savedInstanceState: Bundle?
    ) {
        viewModel = ViewModelProvider(activity, viewModelFactory).get(StatsViewModel::class.java)

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

    private fun StatsFragmentBinding.setupObservers(activity: FragmentActivity) {
        viewModel.isRefreshing.observe(viewLifecycleOwner, {
            it?.let { isRefreshing ->
                swipeToRefreshHelper.isRefreshing = isRefreshing
            }
        })

        viewModel.showSnackbarMessage.observe(viewLifecycleOwner, { holder ->
            showSnackbar(activity, holder)
        })

        viewModel.toolbarHasShadow.observe(viewLifecycleOwner, { hasShadow ->
            appBarLayout.showShadow(hasShadow == true)
        })

        viewModel.siteChanged.observeEvent(viewLifecycleOwner, { siteUpdateResult ->
            when (siteUpdateResult) {
                is SiteUpdateResult.SiteConnected -> viewModel.onSiteChanged()
                is SiteUpdateResult.NotConnectedJetpackSite -> getActivity()?.finish()
            }
        })

        viewModel.hideToolbar.observeEvent(viewLifecycleOwner, { hideToolbar ->
            appBarLayout.setExpanded(!hideToolbar, true)
        })

        viewModel.selectedSection.observe(viewLifecycleOwner, { selectedSection ->
            selectedSection?.let {
                handleSelectedSection(selectedSection)
            }
        })

        viewModel.statsModuleUiModel.observeEvent(viewLifecycleOwner, { event ->
            updateUi(event)
        })
    }

    private fun StatsFragmentBinding.updateUi(statsModuleUiModel: StatsModuleUiModel) {
        if (statsModuleUiModel.disabledStatsViewVisible) {
            disabledView.statsDisabledView.visibility = View.VISIBLE
            tabLayout.visibility = View.GONE
            pullToRefresh.visibility = View.GONE

            if (statsModuleUiModel.disabledStatsProgressVisible) {
                disabledView.statsDisabledView.progressBar.visibility = View.VISIBLE
                disabledView.statsDisabledView.button.visibility = View.GONE
            } else {
                disabledView.statsDisabledView.progressBar.visibility = View.GONE
                disabledView.statsDisabledView.button.visibility = View.VISIBLE
            }
        } else {
            disabledView.statsDisabledView.visibility = View.GONE
            tabLayout.visibility = View.VISIBLE
            pullToRefresh.visibility = View.VISIBLE
        }
    }

    private fun StatsFragmentBinding.handleSelectedSection(
        selectedSection: StatsSection
    ) {
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

    private fun showSnackbar(
        activity: FragmentActivity,
        holder: SnackbarMessageHolder?
    ) {
        val parent = activity.findViewById<View>(R.id.coordinatorLayout)
        if (holder != null && parent != null) {
            if (holder.buttonTitle == null) {
                WPSnackbar.make(
                        parent,
                        uiHelpers.getTextOfUiString(requireContext(), holder.message),
                        Snackbar.LENGTH_LONG
                ).show()
            } else {
                val snackbar = WPSnackbar.make(
                        parent,
                        uiHelpers.getTextOfUiString(requireContext(), holder.message),
                        Snackbar.LENGTH_LONG
                )
                snackbar.setAction(uiHelpers.getTextOfUiString(requireContext(), holder.buttonTitle)) {
                    holder.buttonAction()
                }
                snackbar.show()
            }
        }
    }

    override fun onScrollableViewInitialized(containerId: Int) {
        StatsFragmentBinding.bind(requireView()).appBarLayout.liftOnScrollTargetViewId = containerId
    }
}

class StatsPagerAdapter(val activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = statsSections.size

    override fun createFragment(position: Int): Fragment {
        return StatsListFragment.newInstance(statsSections[position])
    }

    fun getTabTitle(position: Int): CharSequence {
        return activity.getString(statsSections[position].titleRes)
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
