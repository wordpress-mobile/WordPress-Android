package org.wordpress.android.ui.mysite

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.LayoutParams
import com.google.android.material.tabs.TabLayoutMediator
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.MySiteFragmentBinding
import org.wordpress.android.databinding.MySiteInfoCardBinding
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.main.SitePickerActivity
import org.wordpress.android.ui.main.utils.MeGravatarLoader
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.SiteInfoCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.SiteInfoCard.IconState
import org.wordpress.android.ui.mysite.MySiteViewModel.State
import org.wordpress.android.ui.mysite.tabs.MySiteTabFragment
import org.wordpress.android.ui.mysite.tabs.MySiteTabsAdapter
import org.wordpress.android.ui.posts.QuickStartPromptDialogFragment.QuickStartPromptClickInterface
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.BLAVATAR
import org.wordpress.android.util.image.ImageType.USER
import org.wordpress.android.util.setVisible
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

@Suppress("TooManyFunctions")
class MySiteFragment : Fragment(R.layout.my_site_fragment),
        QuickStartPromptClickInterface {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var meGravatarLoader: MeGravatarLoader
    @Inject lateinit var imageManager: ImageManager
    private lateinit var viewModel: MySiteViewModel

    private var binding: MySiteFragmentBinding? = null
    private var siteTitle:String? = null

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
                    actionView.setOnClickListener { viewModel.onAvatarPressed() }
                    TooltipCompat.setTooltipText(actionView, meMenu.title)
                }
            }
        }
        setupTabs(viewModel.tabTitles)
        val avatar = root.findViewById<ImageView>(R.id.avatar)

        appbarMain.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val maxOffset = appBarLayout.totalScrollRange
            val currentOffset = maxOffset + verticalOffset

            updateCollapsibleToolbarTitle(currentOffset)

            val percentage = ((currentOffset.toFloat() / maxOffset.toFloat()) * 100).toInt()
            animateSiteInfoCard(percentage)
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

    private fun MySiteFragmentBinding.updateCollapsibleToolbarTitle(currentOffset: Int) {
        if(currentOffset==0)
            collapsingToolbar.title = siteTitle
        else
            collapsingToolbar.title = null
    }

    private fun MySiteFragmentBinding.animateSiteInfoCard(percentage: Int) {
        siteInfo.siteInfoCard.alpha = percentage.toFloat()/100
    }

    private fun MySiteFragmentBinding.setupTabs(tabTitles: List<UiString>) {
        val adapter = MySiteTabsAdapter(this@MySiteFragment, tabTitles)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = uiHelpers.getTextOfUiString(requireContext(), tabTitles[position])
        }.attach()
    }

    private fun MySiteFragmentBinding.setupContentViews() {
        actionableEmptyView.button.setOnClickListener { viewModel.onAddSitePressed() }
    }

    private fun MySiteFragmentBinding.setupObservers() {
        viewModel.uiModel.observe(viewLifecycleOwner, { uiModel ->
            loadGravatar(uiModel.accountAvatarUrl)
            when (val state = uiModel.state) {
                is State.SiteSelected -> loadData(state)
                is State.NoSites -> loadEmptyView(state)
            }
        })
        viewModel.onNavigation.observeEvent(viewLifecycleOwner, { handleNavigationAction(it) })
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
        handleTabVisibility(state.showTabs)
        actionableEmptyView.setVisible(false)
        viewModel.setActionableEmptyViewGone(actionableEmptyView.isVisible) {
            actionableEmptyView.setVisible(false)
        }
        siteInfo.loadMySiteDetails(state.siteInfoCardItem)
    }

    private fun MySiteFragmentBinding.loadEmptyView(state: State.NoSites) {
        handleTabVisibility(state.showTabs)
        viewModel.setActionableEmptyViewVisible(actionableEmptyView.isVisible) {
            actionableEmptyView.setVisible(true)
            actionableEmptyView.image.setVisible(state.shouldShowImage)
        }
        actionableEmptyView.image.setVisible(state.shouldShowImage)
    }

    private fun MySiteFragmentBinding.handleTabVisibility(visible: Boolean) {
        tabLayout.setVisible(visible)
        if(visible)
            showTabs()
        else
            hideTabs()
    }

    private fun MySiteFragmentBinding.showTabs() {
        val newHeight = dpToPx(200) // New height in pixels
        appbarMain.requestLayout()
        appbarMain.layoutParams.height = newHeight

        val layoutParams = (toolbarMain.layoutParams as? MarginLayoutParams)
        layoutParams?.setMargins(0,0,0, 150)
        toolbarMain.layoutParams = layoutParams;
    }

    private fun MySiteFragmentBinding.hideTabs() {
        val newHeight = dpToPx(156) // New height in pixels
        appbarMain.requestLayout()
        appbarMain.layoutParams.height = newHeight

        val layoutParams = (toolbarMain.layoutParams as? MarginLayoutParams)
        layoutParams?.setMargins(0,0,0, 0)
        toolbarMain.layoutParams = layoutParams;
    }

    fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun handleNavigationAction(action: SiteNavigationAction) = when (action) {
        is SiteNavigationAction.OpenMeScreen -> ActivityLauncher.viewMeActivityForResult(activity)
        is SiteNavigationAction.AddNewSite -> SitePickerActivity.addSite(activity, action.hasAccessToken)
        else -> {
            // Pass all other navigationAction on to the child fragment, so they can be handled properly
            binding?.viewPager?.getCurrentFragment()?.handleNavigationAction(action)
        }
    }

    private fun MySiteInfoCardBinding.loadMySiteDetails(site: SiteInfoCard?) {
        site?.let {
            siteTitle = site.title
            if (site.iconState is IconState.Visible) {
                mySiteBlavatar.visibility = View.VISIBLE
                imageManager.load(mySiteBlavatar, BLAVATAR, site.iconState.url ?: "")
                mySiteIconProgress.visibility = View.GONE
                mySiteBlavatar.setOnClickListener { site.onIconClick.click() }
            } else if (site.iconState is IconState.Progress) {
                mySiteBlavatar.setOnClickListener(null)
                mySiteIconProgress.visibility = View.VISIBLE
                mySiteBlavatar.visibility = View.GONE
            }
            quickStartIconFocusPoint.setVisibleOrGone(site.showIconFocusPoint)
            if (site.onTitleClick != null) {
                siteInfoContainer.title.setOnClickListener { site.onTitleClick.click() }
            } else {
                siteInfoContainer.title.setOnClickListener(null)
            }
            siteInfoContainer.title.text = site.title
            quickStartTitleFocusPoint.setVisibleOrGone(site.showTitleFocusPoint)
            siteInfoContainer.subtitle.text = site.url
            siteInfoContainer.subtitle.setOnClickListener { site.onUrlClick.click() }
            switchSite.setOnClickListener { site.onSwitchSiteClick.click() }
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
        binding?.viewPager?.getCurrentFragment()?.onActivityResult(requestCode, resultCode, data)
    }

    private fun ViewPager2.getCurrentFragment() =
            this@MySiteFragment.childFragmentManager.findFragmentByTag("f$currentItem") as? MySiteTabFragment

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        fun newInstance(): MySiteFragment {
            return MySiteFragment()
        }
    }
}
