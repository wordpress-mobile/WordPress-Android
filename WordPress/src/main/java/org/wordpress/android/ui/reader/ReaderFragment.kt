package org.wordpress.android.ui.reader

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.R
import org.wordpress.android.databinding.ReaderFragmentLayoutBinding
import org.wordpress.android.models.JetpackPoweredScreen
import org.wordpress.android.ui.ScrollableViewInitializedListener
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureFullScreenOverlayFragment
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackFeatureOverlayScreenType
import org.wordpress.android.ui.main.WPMainNavigationView.PageType.READER
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredBottomSheetFragment
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.quickstart.QuickStartEvent
import org.wordpress.android.ui.reader.discover.ReaderDiscoverFragment
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsFragment
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask.FOLLOWED_BLOGS
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask.TAGS
import org.wordpress.android.ui.reader.services.update.ReaderUpdateServiceStarter
import org.wordpress.android.ui.reader.subfilter.SubFilterViewModel
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel.ReaderUiState.ContentUiState
import org.wordpress.android.ui.reader.views.compose.ReaderTopAppBar
import org.wordpress.android.ui.reader.views.compose.filter.ReaderFilterType
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.JetpackBrandingUtils
import org.wordpress.android.util.QuickStartUtilsWrapper
import org.wordpress.android.util.SnackbarItem
import org.wordpress.android.util.SnackbarItem.Action
import org.wordpress.android.util.SnackbarItem.Info
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.viewmodel.observeEvent
import java.util.EnumSet
import javax.inject.Inject

@AndroidEntryPoint
class ReaderFragment : Fragment(R.layout.reader_fragment_layout), ScrollableViewInitializedListener {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var uiHelpers: UiHelpers

    @Inject
    lateinit var quickStartUtilsWrapper: QuickStartUtilsWrapper

    @Inject
    lateinit var jetpackBrandingUtils: JetpackBrandingUtils

    @Inject
    lateinit var snackbarSequencer: SnackbarSequencer
    private lateinit var viewModel: ReaderViewModel

    private var binding: ReaderFragmentLayoutBinding? = null

    private var readerSearchResultLauncher: ActivityResultLauncher<Intent>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = ReaderFragmentLayoutBinding.bind(view).apply {
            initTopAppBar()
            initViewModel(savedInstanceState)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        initReaderSearchActivityResultLauncher()
    }

    private fun initReaderSearchActivityResultLauncher() {
        readerSearchResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                if (data != null) {
                    val shouldRefreshSubscriptions =
                        data.getBooleanExtra(ReaderSearchActivity.RESULT_SHOULD_REFRESH_SUBSCRIPTIONS, false)
                    if (shouldRefreshSubscriptions) {
                        getSubFilterViewModel()?.loadSubFilters()
                    }
                }
            }
        }
    }

    private fun ReaderFragmentLayoutBinding.initTopAppBar() {
        readerTopBarComposeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val topAppBarState by viewModel.topBarUiState.observeAsState()
                val state = topAppBarState ?: return@setContent

                AppTheme {
                    ReaderTopAppBar(
                        topBarUiState = state,
                        onMenuItemClick = viewModel::onTopBarMenuItemClick,
                        onFilterClick = ::tryOpenFilterList,
                        onClearFilterClick = ::clearFilter,
                        isSearchVisible = state.isSearchActionVisible,
                        onSearchClick = viewModel::onSearchActionClicked,
                    )
                }
            }
        }
    }

    private fun ReaderFragmentLayoutBinding.initViewModel(savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this@ReaderFragment, viewModelFactory).get(ReaderViewModel::class.java)
        startReaderViewModel(savedInstanceState)
    }

    private fun ReaderFragmentLayoutBinding.startReaderViewModel(savedInstanceState: Bundle?) {
        viewModel.uiState.observe(viewLifecycleOwner) { uiState ->
            uiState?.let { updateUiState(it) }
        }

        viewModel.updateTags.observeEvent(viewLifecycleOwner) {
            ReaderUpdateServiceStarter.startService(context, EnumSet.of(TAGS, FOLLOWED_BLOGS))
        }

        viewModel.showSearch.observeEvent(viewLifecycleOwner) {
            context?.let {
                val intent = ReaderActivityLauncher.createReaderSearchIntent(it)
                readerSearchResultLauncher?.launch(intent)
            }
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

        viewModel.start(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::viewModel.isInitialized) {
            viewModel.onSaveInstanceState(outState)
        }
    }

    private fun updateUiState(uiState: ReaderViewModel.ReaderUiState) {
        when (uiState) {
            is ContentUiState -> {
                binding?.readerTopBarComposeView?.isVisible = true
                initContentContainer(uiState)
            }
        }
    }

    private fun initContentContainer(uiState: ContentUiState) {
        // only initialize the fragment if there's one selected and it's not already initialized
        val currentFragmentTag = childFragmentManager.findFragmentById(R.id.container)?.tag
        if (uiState.selectedReaderTag == null || uiState.selectedReaderTag.tagSlug == currentFragmentTag) {
            return
        }

        childFragmentManager.beginTransaction().apply {
            val fragment = if (uiState.selectedReaderTag.isDiscover) {
                ReaderDiscoverFragment()
            } else {
                ReaderPostListFragment.newInstanceForTag(
                    uiState.selectedReaderTag,
                    ReaderTypes.ReaderPostListType.TAG_FOLLOWED,
                    true,
                    uiState.selectedReaderTag.isFilterable
                )
            }
            replace(R.id.container, fragment, uiState.selectedReaderTag.tagSlug)
            commit()
        }
        viewModel.uiState.value?.let {
            if (it is ContentUiState) {
                viewModel.onTagChanged(uiState.selectedReaderTag)
            }
        }
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

    fun requestBookmarkTab() {
        viewModel.bookmarkTabRequested()
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
            val screen = JetpackPoweredScreen.WithDynamicText.READER
            binding?.root?.post {
                // post is used to create a minimal delay here. containerId changes just before
                // onScrollableViewInitialized is called, and findViewById can't find the new id before the delay.
                val jetpackBannerView = binding?.jetpackBanner?.root ?: return@post
                val scrollableView = binding?.root?.findViewById<View>(containerId) as? RecyclerView ?: return@post
                jetpackBrandingUtils.showJetpackBannerIfScrolledToTop(jetpackBannerView, scrollableView)
                jetpackBrandingUtils.initJetpackBannerAnimation(jetpackBannerView, scrollableView)
                binding?.jetpackBanner?.jetpackBannerText?.text = uiHelpers.getTextOfUiString(
                    requireContext(),
                    jetpackBrandingUtils.getBrandingTextForScreen(screen)
                )

                if (jetpackBrandingUtils.shouldShowJetpackPoweredBottomSheet()) {
                    jetpackBannerView.setOnClickListener {
                        jetpackBrandingUtils.trackBannerTapped(screen)
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

    private fun getCurrentFeedFragment(): Fragment? {
        return childFragmentManager.findFragmentById(R.id.container)
    }

    // The view model is started by the ReaderPostListFragment for feeds that support filtering
    private fun getSubFilterViewModel(): SubFilterViewModel? {
        val currentFragment = getCurrentFeedFragment()
        val selectedTag = (viewModel.uiState.value as? ContentUiState)?.selectedReaderTag

        if (currentFragment == null || selectedTag == null) return null

        return ViewModelProvider(currentFragment, viewModelFactory).get(
            SubFilterViewModel.getViewModelKeyForTag(selectedTag),
            SubFilterViewModel::class.java
        )
    }

    private fun tryOpenFilterList(type: ReaderFilterType) {
        val viewModel = getSubFilterViewModel() ?: return

        val category = when (type) {
            ReaderFilterType.BLOG -> SubfilterCategory.SITES
            ReaderFilterType.TAG -> SubfilterCategory.TAGS
        }

        viewModel.onSubFiltersListButtonClicked(category)
    }

    private fun clearFilter() {
        val viewModel = getSubFilterViewModel() ?: return
        viewModel.setDefaultSubfilter(isClearingFilter = true)
    }
}
