package org.wordpress.android.ui.mysite

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.MySiteFragmentBinding
import org.wordpress.android.databinding.MySiteInfoHeaderCardBinding
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.main.SitePickerActivity
import org.wordpress.android.ui.main.utils.MeGravatarLoader
import org.wordpress.android.ui.mysite.MySiteCardAndItem.SiteInfoHeaderCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.SiteInfoHeaderCard.IconState
import org.wordpress.android.ui.mysite.MySiteViewModel.SiteInfoToolbarViewParams
import org.wordpress.android.ui.mysite.MySiteViewModel.State
import org.wordpress.android.ui.mysite.MySiteViewModel.TabsUiState
import org.wordpress.android.ui.mysite.MySiteViewModel.TabsUiState.TabUiState
import org.wordpress.android.ui.mysite.tabs.MySiteTabFragment
import org.wordpress.android.ui.mysite.tabs.MySiteTabsAdapter
import org.wordpress.android.ui.posts.QuickStartPromptDialogFragment.QuickStartPromptClickInterface
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.setVisible
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.BLAVATAR
import org.wordpress.android.util.image.ImageType.USER
import org.wordpress.android.viewmodel.observeEvent
import org.wordpress.android.widgets.QuickStartFocusPoint
import javax.inject.Inject

class MySiteFragment : Fragment(R.layout.my_site_fragment),
        QuickStartPromptClickInterface {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var meGravatarLoader: MeGravatarLoader
    @Inject lateinit var imageManager: ImageManager
    private lateinit var viewModel: MySiteViewModel

    private var binding: MySiteFragmentBinding? = null
    private var siteTitle: String? = null

    private val viewPagerCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            viewModel.onTabChanged(position)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSoftKeyboard()
        initDagger()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
        binding = MySiteFragmentBinding.bind(view).apply {
            setupToolbar()
            setupContentViews()
            setupObservers()
        }
    }

    private fun initSoftKeyboard() {
        // The following prevents the soft keyboard from leaving a white space when dismissed.
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
    }

    private fun initDagger() {
        (requireActivity().application as WordPress).component().inject(this)
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this, viewModelFactory).get(MySiteViewModel::class.java)
    }

    private fun MySiteFragmentBinding.setupToolbar() {
        toolbarMain.let { toolbar ->
            toolbar.inflateMenu(R.menu.my_site_menu)
            toolbar.menu.findItem(R.id.me_item)?.let { meMenu ->
                meMenu.actionView.let { actionView ->
                    actionView?.contentDescription = meMenu.title
                    actionView?.setOnClickListener { viewModel.onAvatarPressed() }
                    actionView?.let { TooltipCompat.setTooltipText(it, meMenu.title) }
                }
            }
        }
        val avatar = root.findViewById<ImageView>(R.id.avatar)

        appbarMain.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val maxOffset = appBarLayout.totalScrollRange
            val currentOffset = maxOffset + verticalOffset

            val percentage = if (maxOffset == 0) {
                updateCollapsibleToolbar(1)
                MAX_PERCENT
            } else {
                updateCollapsibleToolbar(currentOffset)
                ((currentOffset.toFloat() / maxOffset.toFloat()) * MAX_PERCENT).toInt()
            }

            fadeSiteInfoHeader(percentage)
            avatar?.let { avatar ->
                val minSize = avatar.minimumHeight
                val maxSize = avatar.maxHeight
                val modifierPx = (minSize.toFloat() - maxSize.toFloat()) * (percentage.toFloat() / 100) * -1
                val modifierPercentage = modifierPx / minSize
                val newScale = 1 + modifierPercentage

                avatar.scaleX = newScale
                avatar.scaleY = newScale
            }
        })
    }

    private fun MySiteFragmentBinding.updateCollapsibleToolbar(currentOffset: Int) {
        if (currentOffset == 0) {
            collapsingToolbar.title = siteTitle
            siteInfo.siteInfoCard.visibility = View.INVISIBLE
        } else {
            collapsingToolbar.title = null
            siteInfo.siteInfoCard.visibility = View.VISIBLE
        }
    }

    private fun MySiteFragmentBinding.fadeSiteInfoHeader(percentage: Int) {
        siteInfo.siteInfoCard.alpha = percentage.toFloat() / 100
    }

    private fun MySiteFragmentBinding.setupContentViews() {
        setupViewPager()
        setupActionableEmptyView()
    }

    private fun MySiteFragmentBinding.setupViewPager() {
        viewPager.registerOnPageChangeCallback(viewPagerCallback)
    }

    private fun MySiteFragmentBinding.setupActionableEmptyView() {
        actionableEmptyView.button.setOnClickListener { viewModel.onAddSitePressed() }
    }

    private fun MySiteFragmentBinding.setupObservers() {
        viewModel.uiModel.observe(viewLifecycleOwner) { uiModel ->
            loadGravatar(uiModel.accountAvatarUrl)
            when (val state = uiModel.state) {
                is State.SiteSelected -> loadData(state)
                is State.NoSites -> loadEmptyView(state)
            }
        }
        viewModel.onNavigation.observeEvent(viewLifecycleOwner, { handleNavigationAction(it) })

        viewModel.onScrollTo.observeEvent(viewLifecycleOwner) {
            var quickStartScrollPosition = it
            if (quickStartScrollPosition == -1) {
                appbarMain.setExpanded(true, true)
                quickStartScrollPosition = 0
            }
            if (quickStartScrollPosition > 0) appbarMain.setExpanded(false, true)
            binding?.viewPager?.getCurrentFragment()?.handleScrollTo(quickStartScrollPosition)
        }
        viewModel.onTrackWithTabSource.observeEvent(viewLifecycleOwner) {
            binding?.viewPager?.getCurrentFragment()?.onTrackWithTabSource(it)
        }
        viewModel.selectTab.observeEvent(viewLifecycleOwner) { navTarget ->
            viewPager.setCurrentItem(navTarget.position, navTarget.smoothAnimation)
        }
    }

    private fun MySiteFragmentBinding.loadGravatar(avatarUrl: String) =
            root.findViewById<ImageView>(R.id.avatar)?.let {
                meGravatarLoader.load(
                        false,
                        meGravatarLoader.constructGravatarUrl(avatarUrl),
                        null,
                        it,
                        USER,
                        null
                )
            }

    private fun MySiteFragmentBinding.loadData(state: State.SiteSelected) {
        tabLayout.setVisible(state.tabsUiState.showTabs)
        updateTabs(state.tabsUiState)
        actionableEmptyView.setVisible(false)
        viewModel.setActionableEmptyViewGone(actionableEmptyView.isVisible) {
            actionableEmptyView.setVisible(false)
        }
        if (state.siteInfoHeaderState.hasUpdates || !header.isVisible) {
            siteInfo.loadMySiteDetails(state.siteInfoHeaderState.siteInfoHeader)
        }
        updateSiteInfoToolbarView(state.siteInfoToolbarViewParams)
    }

    private fun MySiteInfoHeaderCardBinding.loadMySiteDetails(siteInfoHeader: SiteInfoHeaderCard) {
        siteTitle = siteInfoHeader.title
        if (siteInfoHeader.iconState is IconState.Visible) {
            mySiteBlavatar.visibility = View.VISIBLE
            imageManager.load(mySiteBlavatar, BLAVATAR, siteInfoHeader.iconState.url ?: "")
            mySiteIconProgress.visibility = View.GONE
            mySiteBlavatar.setOnClickListener { siteInfoHeader.onIconClick.click() }
        } else if (siteInfoHeader.iconState is IconState.Progress) {
            mySiteBlavatar.setOnClickListener(null)
            mySiteIconProgress.visibility = View.VISIBLE
            mySiteBlavatar.visibility = View.GONE
        }
        quickStartIconFocusPoint.setVisibleOrGone(siteInfoHeader.showIconFocusPoint)
        if (siteInfoHeader.onTitleClick != null) {
            siteInfoContainer.title.setOnClickListener { siteInfoHeader.onTitleClick.click() }
        } else {
            siteInfoContainer.title.setOnClickListener(null)
        }
        siteInfoContainer.title.text = siteInfoHeader.title
        quickStartTitleFocusPoint.setVisibleOrGone(siteInfoHeader.showTitleFocusPoint)
        quickStartSubTitleFocusPoint.setVisibleOrGone(siteInfoHeader.showSubtitleFocusPoint)
        siteInfoContainer.subtitle.text = siteInfoHeader.url
        siteInfoContainer.subtitle.setOnClickListener { siteInfoHeader.onUrlClick.click() }
        switchSite.setOnClickListener { siteInfoHeader.onSwitchSiteClick.click() }
    }

    private fun MySiteFragmentBinding.updateSiteInfoToolbarView(siteInfoToolbarViewParams: SiteInfoToolbarViewParams) {
        showHeader(siteInfoToolbarViewParams.headerVisible)
        val appBarHeight = resources.getDimension(siteInfoToolbarViewParams.appBarHeight).toInt()
        appbarMain.layoutParams.height = appBarHeight
        val toolbarBottomMargin = resources.getDimension(siteInfoToolbarViewParams.toolbarBottomMargin).toInt()
        updateToolbarBottomMargin(toolbarBottomMargin)
        appbarMain.isLiftOnScroll = siteInfoToolbarViewParams.appBarLiftOnScroll
        appbarMain.requestLayout()
    }

    private fun MySiteFragmentBinding.updateToolbarBottomMargin(appBarHeight: Int) {
        val bottomMargin = (appBarHeight / resources.displayMetrics.density).toInt()
        val layoutParams = (toolbarMain.layoutParams as? MarginLayoutParams)
        layoutParams?.setMargins(0, 0, 0, bottomMargin)
        toolbarMain.layoutParams = layoutParams
    }

    private fun MySiteFragmentBinding.loadEmptyView(state: State.NoSites) {
        tabLayout.setVisible(state.tabsUiState.showTabs)
        viewModel.setActionableEmptyViewVisible(actionableEmptyView.isVisible) {
            actionableEmptyView.setVisible(true)
            actionableEmptyView.image.setVisible(state.shouldShowImage)
        }
        actionableEmptyView.image.setVisible(state.shouldShowImage)
        siteTitle = getString(R.string.my_site_section_screen_title)
        updateSiteInfoToolbarView(state.siteInfoToolbarViewParams)
        appbarMain.setExpanded(false, true)
    }

    private fun MySiteFragmentBinding.showHeader(visibility: Boolean) {
        header.visibility = if (visibility) View.VISIBLE else View.INVISIBLE
    }

    private fun MySiteFragmentBinding.updateViewPagerAdapterAndMediatorIfNeeded(state: TabsUiState) {
        if (viewPager.adapter == null || state.shouldUpdateViewPager) {
            viewPager.adapter = MySiteTabsAdapter(this@MySiteFragment, state.tabUiStates)
            TabLayoutMediator(tabLayout, viewPager, MySiteTabConfigurationStrategy(state.tabUiStates)).attach()
        }
    }

    private fun MySiteFragmentBinding.updateTabs(state: TabsUiState) {
        updateViewPagerAdapterAndMediatorIfNeeded(state)
        state.tabUiStates.forEachIndexed { index, tabUiState ->
            val tab = tabLayout.getTabAt(index) as TabLayout.Tab
            updateTab(tab, tabUiState)
        }
    }

    private fun MySiteFragmentBinding.updateTab(tab: TabLayout.Tab, tabUiState: TabUiState) {
        val customView = tab.customView ?: createTabCustomView(tab)
        with(customView) {
            val title = findViewById<TextView>(R.id.tab_label)
            val quickStartFocusPoint = findViewById<QuickStartFocusPoint>(R.id.tab_quick_start_focus_point)
            title.text = uiHelpers.getTextOfUiString(requireContext(), tabUiState.label)
            quickStartFocusPoint?.setVisible(tabUiState.showQuickStartFocusPoint)
        }
    }

    private fun handleNavigationAction(action: SiteNavigationAction) = when (action) {
        is SiteNavigationAction.OpenMeScreen -> ActivityLauncher.viewMeActivityForResult(activity)
        is SiteNavigationAction.AddNewSite -> SitePickerActivity.addSite(activity, action.hasAccessToken, action.source)
        else -> {
            /* Pass all other navigationAction on to the child fragment, so they can be handled properly.
               Added brief delay before passing action to nested (view pager) tab fragments to give them time to get
               created. */
            view?.postDelayed({
                binding?.viewPager?.getCurrentFragment()?.handleNavigationAction(action)
            }, PASS_TO_TAB_FRAGMENT_DELAY)
            Unit
        }
    }

    override fun onPositiveClicked(instanceTag: String) {
        binding?.viewPager?.getCurrentFragment()?.onPositiveClicked(instanceTag)
    }

    override fun onNegativeClicked(instanceTag: String) {
        binding?.viewPager?.getCurrentFragment()?.onNegativeClicked(instanceTag)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        /* Add brief delay before passing result to nested (view pager) tab fragments to give them time to get created.
           This is a workaround to fix API Level 25 (GitHub #16225) issue where we noticed that nested fragments
           were created after parent fragment was shown the first time and received activity result. It might not be a
           real issue as we could only test it on an emulator, we added it to be safe in such cases. */
        view?.postDelayed({
            binding?.viewPager?.getCurrentFragment()?.onActivityResult(requestCode, resultCode, data)
        }, PASS_TO_TAB_FRAGMENT_DELAY)
    }

    private fun ViewPager2.getCurrentFragment() =
            this@MySiteFragment.childFragmentManager.findFragmentByTag("f$currentItem") as? MySiteTabFragment

    private fun MySiteFragmentBinding.createTabCustomView(tab: TabLayout.Tab): View {
        val customView = LayoutInflater.from(context)
                .inflate(R.layout.tab_custom_view, tabLayout, false)
        tab.customView = customView
        return customView
    }

    private inner class MySiteTabConfigurationStrategy(
        private val tabUiStates: List<TabUiState>
    ) : TabLayoutMediator.TabConfigurationStrategy {
        override fun onConfigureTab(@NonNull tab: TabLayout.Tab, position: Int) {
            binding?.updateTab(tab, tabUiStates[position])
        }
    }

    override fun onPause() {
        super.onPause()
        activity?.let {
            if (!it.isChangingConfigurations) {
                viewModel.clearActiveQuickStartTask()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        private const val PASS_TO_TAB_FRAGMENT_DELAY = 300L
        private const val MAX_PERCENT = 100
        fun newInstance(): MySiteFragment {
            return MySiteFragment()
        }
    }
}
