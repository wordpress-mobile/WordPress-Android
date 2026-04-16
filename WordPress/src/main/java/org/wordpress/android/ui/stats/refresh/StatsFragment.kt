package org.wordpress.android.ui.stats.refresh

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.MarginPageTransformer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayout.Tab
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.databinding.StatsFragmentBinding
import org.wordpress.android.models.JetpackPoweredScreen
import org.wordpress.android.ui.ScrollableViewInitializedListener
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureFullScreenOverlayFragment
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackFeatureOverlayScreenType
import org.wordpress.android.ui.jetpackoverlay.JetpackOverlayConnectedFeature
import org.wordpress.android.ui.main.WPMainNavigationView.PageType.MY_SITE
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredBottomSheetFragment
import org.wordpress.android.ui.newstats.NewStatsActivity
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures.Feature
import org.wordpress.android.ui.stats.refresh.StatsViewModel.StatsModuleUiModel
import org.wordpress.android.ui.stats.refresh.lists.StatsListFragment
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.ANNUAL_STATS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.DAYS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.DETAIL
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.INSIGHTS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.INSIGHT_DETAIL
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.MONTHS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.SUBSCRIBERS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.TOTAL_COMMENTS_DETAIL
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.TOTAL_FOLLOWERS_DETAIL
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.TOTAL_LIKES_DETAIL
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.TRAFFIC
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.WEEKS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.YEARS
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider.SiteUpdateResult
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.JetpackBrandingUtils
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.StatsTrafficSubscribersTabsFeatureConfig
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.viewmodel.observeEvent
import org.wordpress.android.widgets.WPSnackbar
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private val NEW_STATS_SUGGESTION_RESHOW_DELAY_MS = TimeUnit.DAYS.toMillis(7)

private val statsSections = listOf(INSIGHTS, DAYS, WEEKS, MONTHS, YEARS)
private val statsSectionsWithTrafficTab = listOf(TRAFFIC, INSIGHTS, SUBSCRIBERS)
private var statsTrafficTabEnabled = false

@AndroidEntryPoint
class StatsFragment : Fragment(R.layout.stats_fragment), ScrollableViewInitializedListener,
    StatsPullToRefreshListener.PullToRefreshReceiverListener {
    @Inject
    lateinit var uiHelpers: UiHelpers

    @Inject
    lateinit var jetpackBrandingUtils: JetpackBrandingUtils

    @Inject
    lateinit var experimentalFeatures: ExperimentalFeatures

    @Inject
    lateinit var analyticsTracker: AnalyticsTrackerWrapper

    @Inject
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Inject
    lateinit var jetpackFeatureRemovalOverlayUtil: JetpackFeatureRemovalOverlayUtil

    @Inject
    lateinit var mStatsTrafficSubscribersTabsFeatureConfig: StatsTrafficSubscribersTabsFeatureConfig

    private val viewModel: StatsViewModel by activityViewModels()
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper
    private lateinit var selectedTabListener: SelectedTabListener

    private var restorePreviousSearch = false

    private var binding: StatsFragmentBinding? = null

    private var currentStatsPullToRefreshListener:
            WeakReference<StatsPullToRefreshListener.PullToRefreshEmitterListener>? = null

    override fun setPullToRefreshReceiver(
        emitterListener: StatsPullToRefreshListener.PullToRefreshEmitterListener
    ) {
        currentStatsPullToRefreshListener = WeakReference(emitterListener)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = requireActivity()
        with(StatsFragmentBinding.bind(view)) {
            binding = this
            with(nonNullActivity as AppCompatActivity) {
                setSupportActionBar(toolbar)
                supportActionBar?.let {
                    it.setHomeButtonEnabled(true)
                    it.setDisplayHomeAsUpEnabled(true)
                }
            }
            initializeViewModels(nonNullActivity, savedInstanceState == null, savedInstanceState)
            initializeViews()
            setupMenu()
        }
        maybeShowNewStatsSuggestion()
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.stats_toolbar_menu, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.menu_try_new_stats -> {
                            switchToNewStats()
                            true
                        }
                        else -> false
                    }
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private fun switchToNewStats() {
        if (!isAdded) return
        analyticsTracker.track(Stat.STATS_NEW_STATS_ENABLED)
        experimentalFeatures.setEnabled(Feature.NEW_STATS, true)
        NewStatsActivity.start(requireContext())
        requireActivity().finish()
    }

    @Suppress("ReturnCount")
    private fun maybeShowNewStatsSuggestion() {
        if (appPrefsWrapper.getStatsNewStatsSuggestionShown()) return
        // Avoid stacking on top of the Jetpack-powered bottom sheet or the feature-removal overlay,
        // both of which may show on a fresh Stats activity launch.
        if (jetpackBrandingUtils.shouldShowJetpackPoweredBottomSheet()) return
        if (jetpackFeatureRemovalOverlayUtil.shouldShowFeatureSpecificJetpackOverlay(
                JetpackOverlayConnectedFeature.STATS
            )
        ) return
        val lastDismissedAt = appPrefsWrapper.getStatsNewStatsSuggestionLastDismissedAt()
        val isSecondAttempt = lastDismissedAt > 0L
        if (isSecondAttempt &&
            System.currentTimeMillis() - lastDismissedAt < NEW_STATS_SUGGESTION_RESHOW_DELAY_MS
        ) {
            return
        }
        val onDismiss = {
            if (isSecondAttempt) {
                appPrefsWrapper.setStatsNewStatsSuggestionShown(true)
            } else {
                appPrefsWrapper.setStatsNewStatsSuggestionLastDismissedAt(System.currentTimeMillis())
            }
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.stats_new_stats_suggestion_title)
            .setMessage(R.string.stats_new_stats_suggestion_message)
            .setPositiveButton(R.string.stats_new_stats_suggestion_positive) { _, _ ->
                appPrefsWrapper.setStatsNewStatsSuggestionShown(true)
                switchToNewStats()
            }
            .setNegativeButton(R.string.stats_new_stats_suggestion_negative) { _, _ -> onDismiss() }
            .setOnCancelListener { onDismiss() }
            .show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(WordPress.LOCAL_SITE_ID, activity?.intent?.getIntExtra(WordPress.LOCAL_SITE_ID, 0) ?: 0)
        viewModel.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    private fun StatsFragmentBinding.initializeViews() {
        statsTrafficTabEnabled = mStatsTrafficSubscribersTabsFeatureConfig.isEnabled()

        val adapter = StatsPagerAdapter(this@StatsFragment)
        statsPager.adapter = adapter
        statsPager.setPageTransformer(
            MarginPageTransformer(resources.getDimensionPixelSize(R.dimen.margin_extra_large))
        )
        statsPager.offscreenPageLimit = 2
        selectedTabListener = SelectedTabListener(viewModel)
        TabLayoutMediator(tabLayout, statsPager) { tab, position ->
            tab.text = adapter.getTabTitle(position)
        }.attach()
        tabLayout.addOnTabSelectedListener(selectedTabListener)

        swipeToRefreshHelper = WPSwipeToRefreshHelper.buildSwipeToRefreshHelper(pullToRefresh) {
            viewModel.onPullToRefresh()
            currentStatsPullToRefreshListener?.get()?.onPullRefresh()
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

        viewModel.showJetpackPoweredBottomSheet.observeEvent(viewLifecycleOwner) {
            if (isFirstStart) {
                JetpackPoweredBottomSheetFragment
                    .newInstance(it, MY_SITE)
                    .show(childFragmentManager, JetpackPoweredBottomSheetFragment.TAG)
            }
        }

        viewModel.showJetpackOverlay.observeEvent(viewLifecycleOwner) {
            if (isFirstStart) {
                JetpackFeatureFullScreenOverlayFragment
                    .newInstance(JetpackFeatureOverlayScreenType.STATS)
                    .show(childFragmentManager, JetpackFeatureFullScreenOverlayFragment.TAG)
            }
        }
    }

    private fun StatsFragmentBinding.setupObservers(activity: FragmentActivity) {
        viewModel.isRefreshing.observe(viewLifecycleOwner) {
            it?.let { isRefreshing ->
                swipeToRefreshHelper.isRefreshing = isRefreshing
            }
        }

        viewModel.showSnackbarMessage.observe(viewLifecycleOwner) { holder ->
            showSnackbar(activity, holder)
        }

        viewModel.toolbarHasShadow.observe(viewLifecycleOwner) { hasShadow ->
            appBarLayout.showShadow(hasShadow == true)
        }

        viewModel.siteChanged.observeEvent(viewLifecycleOwner) { siteUpdateResult ->
            when (siteUpdateResult) {
                is SiteUpdateResult.SiteConnected -> viewModel.onSiteChanged()
                is SiteUpdateResult.NotConnectedJetpackSite -> getActivity()?.finish()
            }
        }

        viewModel.hideToolbar.observeEvent(viewLifecycleOwner) { hideToolbar ->
            appBarLayout.setExpanded(!hideToolbar, true)
        }

        viewModel.selectedSection.observe(viewLifecycleOwner) { selectedSection ->
            selectedSection?.let {
                if (statsTrafficTabEnabled) {
                    handleSelectedSectionWithTrafficTab(selectedSection)
                } else {
                    handleSelectedSection(selectedSection)
                }
            }
        }

        viewModel.statsModuleUiModel.observeEvent(viewLifecycleOwner) { event ->
            updateUi(event)
        }
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

    private fun StatsFragmentBinding.handleSelectedSectionWithTrafficTab(selectedSection: StatsSection) {
        val position = when (selectedSection) {
            TRAFFIC -> 0
            INSIGHTS -> 1
            SUBSCRIBERS -> 2
            DETAIL,
            INSIGHT_DETAIL,
            TOTAL_LIKES_DETAIL,
            TOTAL_COMMENTS_DETAIL,
            TOTAL_FOLLOWERS_DETAIL,
            ANNUAL_STATS -> null
            else -> null
        }
        position?.let {
            if (statsPager.currentItem != position) {
                tabLayout.removeOnTabSelectedListener(selectedTabListener)
                statsPager.currentItem = position
                tabLayout.addOnTabSelectedListener(selectedTabListener)
            }
        }
    }

    @Suppress("MagicNumber")
    private fun StatsFragmentBinding.handleSelectedSection(selectedSection: StatsSection) {
        val position = when (selectedSection) {
            INSIGHTS -> 0
            DAYS -> 1
            WEEKS -> 2
            MONTHS -> 3
            YEARS -> 4
            DETAIL,
            INSIGHT_DETAIL,
            TOTAL_LIKES_DETAIL,
            TOTAL_COMMENTS_DETAIL,
            TOTAL_FOLLOWERS_DETAIL,
            ANNUAL_STATS -> null
            else -> null
        }
        position?.let {
            if (statsPager.currentItem != position) {
                tabLayout.removeOnTabSelectedListener(selectedTabListener)
                statsPager.currentItem = position
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
        initJetpackBanner(containerId)
    }

    private fun initJetpackBanner(scrollableContainerId: Int) {
        if (jetpackBrandingUtils.shouldShowJetpackBranding()) {
            val screen = JetpackPoweredScreen.WithDynamicText.STATS
            binding?.root?.post {
                val jetpackBannerView = binding?.jetpackBanner?.root ?: return@post
                val scrollableView = binding?.root?.findViewById<View>(scrollableContainerId) as? RecyclerView
                    ?: return@post

                jetpackBrandingUtils.showJetpackBannerIfScrolledToTop(jetpackBannerView, scrollableView)
                jetpackBrandingUtils.initJetpackBannerAnimation(jetpackBannerView, scrollableView)
                binding?.jetpackBanner?.jetpackBannerText?.text = uiHelpers.getTextOfUiString(
                    requireContext(),
                    jetpackBrandingUtils.getBrandingTextForScreen(screen)
                )

                if (jetpackBrandingUtils.shouldShowJetpackPoweredBottomSheet()) {
                    binding?.jetpackBanner?.root?.setOnClickListener {
                        jetpackBrandingUtils.trackBannerTapped(screen)
                        JetpackPoweredBottomSheetFragment
                            .newInstance()
                            .show(childFragmentManager, JetpackPoweredBottomSheetFragment.TAG)
                    }
                }
            }
        }
    }
}

class StatsPagerAdapter(private val parent: Fragment) : FragmentStateAdapter(parent) {
    private val statsTabs = if (statsTrafficTabEnabled) statsSectionsWithTrafficTab else statsSections
    override fun getItemCount(): Int = statsTabs.size

    override fun createFragment(position: Int): Fragment {
        return StatsListFragment.newInstance(statsTabs[position])
    }

    fun getTabTitle(position: Int): CharSequence {
        return parent.context?.getString(statsTabs[position].titleRes).orEmpty()
    }
}

private class SelectedTabListener(val viewModel: StatsViewModel) : OnTabSelectedListener {
    private val statsTabs = if (statsTrafficTabEnabled) statsSectionsWithTrafficTab else statsSections
    override fun onTabReselected(tab: Tab?) {
        // Do nothing
    }

    override fun onTabUnselected(tab: Tab?) {
        // Do nothing
    }

    override fun onTabSelected(tab: Tab) {
        viewModel.onSectionSelected(statsTabs[tab.position])
    }
}

interface StatsPullToRefreshListener {
    interface PullToRefreshReceiverListener {
        fun setPullToRefreshReceiver(emitterListener: PullToRefreshEmitterListener)
    }
    interface PullToRefreshEmitterListener {
        fun onPullRefresh()
    }
}
