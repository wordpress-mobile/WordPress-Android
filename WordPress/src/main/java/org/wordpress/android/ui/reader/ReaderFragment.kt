package org.wordpress.android.ui.reader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.databinding.ReaderFragmentLayoutBinding
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.ui.ScrollableViewInitializedListener
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureFullScreenOverlayFragment
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackFeatureOverlayScreenType
import org.wordpress.android.ui.main.WPMainNavigationView.PageType.READER
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredBottomSheetFragment
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.quickstart.QuickStartEvent
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.discover.ReaderDiscoverFragment
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsFragment
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask.FOLLOWED_BLOGS
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask.TAGS
import org.wordpress.android.ui.reader.services.update.ReaderUpdateServiceStarter
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel.ReaderUiState.ContentUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel.ReaderUiState.ContentUiState.TabUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.JetpackBrandingUtils
import org.wordpress.android.util.JetpackBrandingUtils.Screen
import org.wordpress.android.util.QuickStartUtilsWrapper
import org.wordpress.android.util.SnackbarItem
import org.wordpress.android.util.SnackbarItem.Action
import org.wordpress.android.util.SnackbarItem.Info
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.viewmodel.observeEvent
import org.wordpress.android.widgets.QuickStartFocusPoint
import java.util.EnumSet
import javax.inject.Inject

@AndroidEntryPoint
class ReaderFragment : Fragment(R.layout.reader_fragment_layout), ScrollableViewInitializedListener {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var quickStartUtilsWrapper: QuickStartUtilsWrapper
    @Inject lateinit var jetpackBrandingUtils: JetpackBrandingUtils
    @Inject lateinit var snackbarSequencer: SnackbarSequencer
    private lateinit var viewModel: ReaderViewModel

    private var searchMenuItem: MenuItem? = null
    private var settingsMenuItem: MenuItem? = null
    private var settingsMenuItemFocusPoint: QuickStartFocusPoint? = null

    private var binding: ReaderFragmentLayoutBinding? = null

    private val viewPagerCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            viewModel.uiState.value?.let {
                if (it is ContentUiState) {
                    val selectedTag = it.readerTagList[position]
                    viewModel.onTagChanged(selectedTag)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        binding = ReaderFragmentLayoutBinding.bind(view).apply {
            initToolbar()
            initViewPager()
            initViewModel(savedInstanceState)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchMenuItem = null
        settingsMenuItem = null
        settingsMenuItemFocusPoint = null
        binding = null
    }

    override fun onResume() {
        super.onResume()
        viewModel.onScreenInForeground()
    }

    override fun onPause() {
        super.onPause()
        activity?.let { viewModel.onScreenInBackground(it.isChangingConfigurations) }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.reader_home, menu)
        menu.findItem(R.id.menu_search).apply {
            searchMenuItem = this
            this.isVisible = viewModel.uiState.value?.searchMenuItemUiState?.isVisible ?: false
        }
        menu.findItem(R.id.menu_settings).apply {
            settingsMenuItem = this
            settingsMenuItemFocusPoint = this.actionView?.findViewById(R.id.menu_quick_start_focus_point)
            this.isVisible = viewModel.uiState.value?.settingsMenuItemUiState?.isVisible ?: false
            settingsMenuItemFocusPoint?.isVisible =
                    viewModel.uiState.value?.settingsMenuItemUiState?.showQuickStartFocusPoint ?: false
            this.actionView?.setOnClickListener { viewModel.onSettingsActionClicked() }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_search -> {
                viewModel.onSearchActionClicked()
                true
            }
            R.id.menu_settings -> {
                viewModel.onSettingsActionClicked()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun ReaderFragmentLayoutBinding.initToolbar() {
        toolbar.title = getString(string.reader_screen_title)
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
    }

    private fun ReaderFragmentLayoutBinding.initViewPager() {
        viewPager.registerOnPageChangeCallback(viewPagerCallback)
    }

    private fun ReaderFragmentLayoutBinding.initViewModel(savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this@ReaderFragment, viewModelFactory).get(ReaderViewModel::class.java)
        startObserving(savedInstanceState)
    }

    private fun ReaderFragmentLayoutBinding.startObserving(savedInstanceState: Bundle?) {
        viewModel.uiState.observe(viewLifecycleOwner) { uiState ->
            uiState?.let {
                when (it) {
                    is ContentUiState -> {
                        updateTabs(it)
                    }
                }
                uiHelpers.updateVisibility(tabLayout, uiState.tabLayoutVisible)
                searchMenuItem?.isVisible = uiState.searchMenuItemUiState.isVisible
                settingsMenuItem?.isVisible = uiState.settingsMenuItemUiState.isVisible
                settingsMenuItemFocusPoint?.isVisible =
                        viewModel.uiState.value?.settingsMenuItemUiState?.showQuickStartFocusPoint ?: false
            }
        }

        viewModel.updateTags.observeEvent(viewLifecycleOwner) {
            ReaderUpdateServiceStarter.startService(context, EnumSet.of(TAGS, FOLLOWED_BLOGS))
        }

        viewModel.selectTab.observeEvent(viewLifecycleOwner) { navTarget ->
            viewPager.setCurrentItem(navTarget.position, navTarget.smoothAnimation)
        }

        viewModel.showSearch.observeEvent(viewLifecycleOwner) {
            ReaderActivityLauncher.showReaderSearch(context)
        }

        viewModel.showSettings.observeEvent(viewLifecycleOwner) {
            ReaderActivityLauncher.showReaderSubs(context)
        }

        viewModel.showReaderInterests.observeEvent(viewLifecycleOwner) {
            showReaderInterests()
        }

        viewModel.closeReaderInterests.observeEvent(viewLifecycleOwner) {
            closeReaderInterests()
        }

        viewModel.quickStartPromptEvent.observeEvent(viewLifecycleOwner) { prompt ->
            val message = quickStartUtilsWrapper.stylizeQuickStartPrompt(
                    requireActivity(),
                    prompt.shortMessagePrompt,
                    prompt.iconId
            )

            showSnackbar(
                    SnackbarMessageHolder(
                            message = UiStringText(message),
                            duration = prompt.duration,
                            onDismissAction = {
                                viewModel.onQuickStartPromptDismissed()
                            },
                            isImportant = false
                    )
            )
        }

        viewModel.showJetpackPoweredBottomSheet.observeEvent(viewLifecycleOwner) {
            JetpackPoweredBottomSheetFragment
                    .newInstance(it, READER)
                    .show(childFragmentManager, JetpackPoweredBottomSheetFragment.TAG)
        }

        observeJetpackOverlayEvent(savedInstanceState)

        viewModel.start()
    }

    private fun observeJetpackOverlayEvent(savedInstanceState: Bundle?) {
        if (savedInstanceState == null)
            viewModel.showJetpackOverlay.observeEvent(viewLifecycleOwner) {
                JetpackFeatureFullScreenOverlayFragment
                        .newInstance(JetpackFeatureOverlayScreenType.READER)
                        .show(childFragmentManager, JetpackFeatureFullScreenOverlayFragment.TAG)
            }
    }

    private fun ReaderFragmentLayoutBinding.showSnackbar(holder: SnackbarMessageHolder) {
        if (!isAdded || view == null) return
        snackbarSequencer.enqueue(
                SnackbarItem(
                        info = Info(
                                view = coordinatorLayout,
                                textRes = holder.message,
                                duration = holder.duration,
                                isImportant = holder.isImportant
                        ),
                        action = holder.buttonTitle?.let {
                            Action(
                                    textRes = holder.buttonTitle,
                                    clickListener = { holder.buttonAction() }
                            )
                        }
                )
        )
    }

    private fun ReaderFragmentLayoutBinding.updateTabs(uiState: ContentUiState) {
        if (viewPager.adapter == null || uiState.shouldUpdateViewPager) {
            updateViewPagerAdapterAndMediator(uiState)
        }
        uiState.tabUiStates.forEachIndexed { index, tabUiState ->
            val tab = tabLayout.getTabAt(index) as TabLayout.Tab
            updateTab(tab, tabUiState)
        }
    }

    private fun ReaderFragmentLayoutBinding.updateTab(tab: TabLayout.Tab, tabUiState: TabUiState) {
        val customView = tab.customView ?: createTabCustomView(tab)
        with(customView) {
            val title = findViewById<TextView>(R.id.tab_label)
            title.text = uiHelpers.getTextOfUiString(requireContext(), tabUiState.label)
        }
    }

    private fun ReaderFragmentLayoutBinding.updateViewPagerAdapterAndMediator(uiState: ContentUiState) {
        viewPager.adapter = TabsAdapter(this@ReaderFragment, uiState.readerTagList)
        TabLayoutMediator(tabLayout, viewPager, ReaderTabConfigurationStrategy(uiState)).attach()
    }

    private inner class ReaderTabConfigurationStrategy(
        private val uiState: ContentUiState
    ) : TabLayoutMediator.TabConfigurationStrategy {
        override fun onConfigureTab(@NonNull tab: TabLayout.Tab, position: Int) {
            binding?.updateTab(tab, uiState.tabUiStates[position])
        }
    }

    private fun ReaderFragmentLayoutBinding.createTabCustomView(tab: TabLayout.Tab): View {
        val customView = LayoutInflater.from(context)
                .inflate(R.layout.tab_custom_view, tabLayout, false)
        tab.customView = customView
        return customView
    }

    fun requestBookmarkTab() {
        viewModel.bookmarkTabRequested()
    }

    private class TabsAdapter(parent: Fragment, private val tags: ReaderTagList) : FragmentStateAdapter(parent) {
        override fun getItemCount(): Int = tags.size

        override fun createFragment(position: Int): Fragment {
            return if (tags[position].isDiscover) {
                ReaderDiscoverFragment()
            } else {
                ReaderPostListFragment.newInstanceForTag(
                        tags[position],
                        ReaderPostListType.TAG_FOLLOWED,
                        true
                )
            }
        }
    }

    private fun showReaderInterests() {
        val readerInterestsFragment = childFragmentManager.findFragmentByTag(ReaderInterestsFragment.TAG)
        if (readerInterestsFragment == null) {
            childFragmentManager.beginTransaction()
                    .replace(
                            R.id.interests_fragment_container,
                            ReaderInterestsFragment(),
                            ReaderInterestsFragment.TAG
                    )
                    .commitNow()
        }
    }

    private fun closeReaderInterests() {
        val readerInterestsFragment = childFragmentManager.findFragmentByTag(ReaderInterestsFragment.TAG)
        if (readerInterestsFragment?.isAdded == true) {
            childFragmentManager.beginTransaction()
                    .remove(readerInterestsFragment)
                    .commitNow()
        }
    }

    override fun onScrollableViewInitialized(containerId: Int) {
        binding?.appBar?.liftOnScrollTargetViewId = containerId
        if (jetpackBrandingUtils.shouldShowJetpackBranding()) {
            binding?.root?.post {
                // post is used to create a minimal delay here. containerId changes just before
                // onScrollableViewInitialized is called, and findViewById can't find the new id before the delay.
                val jetpackBannerView = binding?.jetpackBanner?.root ?: return@post
                val scrollableView = binding?.root?.findViewById<View>(containerId) as? RecyclerView ?: return@post
                jetpackBrandingUtils.showJetpackBannerIfScrolledToTop(jetpackBannerView, scrollableView)
                jetpackBrandingUtils.initJetpackBannerAnimation(jetpackBannerView, scrollableView)

                if (jetpackBrandingUtils.shouldShowJetpackPoweredBottomSheet()) {
                    jetpackBannerView.setOnClickListener {
                        jetpackBrandingUtils.trackBannerTapped(Screen.READER)
                        JetpackPoweredBottomSheetFragment
                                .newInstance()
                                .show(childFragmentManager, JetpackPoweredBottomSheetFragment.TAG)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(sticky = true, threadMode = MAIN)
    fun onEvent(event: QuickStartEvent) {
        if (!isAdded || view == null) {
            return
        }
        viewModel.onQuickStartEventReceived(event)
        EventBus.getDefault().removeStickyEvent(event)
    }
}
