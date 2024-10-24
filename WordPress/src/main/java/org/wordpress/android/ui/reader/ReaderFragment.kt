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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.R
import org.wordpress.android.databinding.ReaderFragmentLayoutBinding
import org.wordpress.android.models.JetpackPoweredScreen
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.ScrollableViewInitializedListener
import org.wordpress.android.ui.compose.theme.AppThemeM2
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureFullScreenOverlayFragment
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackFeatureOverlayScreenType
import org.wordpress.android.ui.main.WPMainActivity.OnScrollToTopListener
import org.wordpress.android.ui.main.WPMainNavigationView.PageType.READER
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredBottomSheetFragment
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.quickstart.QuickStartEvent
import org.wordpress.android.ui.reader.SubfilterBottomSheetFragment.Companion.newInstance
import org.wordpress.android.ui.reader.discover.ReaderDiscoverFragment
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsFragment
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask.FOLLOWED_BLOGS
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask.TAGS
import org.wordpress.android.ui.reader.services.update.ReaderUpdateServiceStarter
import org.wordpress.android.ui.reader.subfilter.ActionType
import org.wordpress.android.ui.reader.subfilter.ActionType.OpenLoginPage
import org.wordpress.android.ui.reader.subfilter.ActionType.OpenSearchPage
import org.wordpress.android.ui.reader.subfilter.ActionType.OpenSubsAtPage
import org.wordpress.android.ui.reader.subfilter.ActionType.OpenSuggestedTagsPage
import org.wordpress.android.ui.reader.subfilter.BottomSheetUiState
import org.wordpress.android.ui.reader.subfilter.BottomSheetUiState.BottomSheetVisible
import org.wordpress.android.ui.reader.subfilter.SubFilterViewModel
import org.wordpress.android.ui.reader.subfilter.SubFilterViewModelProvider
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel.ReaderUiState.ContentUiState
import org.wordpress.android.ui.reader.views.compose.ReaderTopAppBar
import org.wordpress.android.ui.reader.views.compose.filter.ReaderFilterType
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.JetpackBrandingUtils
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.QuickStartUtilsWrapper
import org.wordpress.android.util.SnackbarItem
import org.wordpress.android.util.SnackbarItem.Action
import org.wordpress.android.util.SnackbarItem.Info
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.main.WPMainActivityViewModel
import org.wordpress.android.viewmodel.observeEvent
import java.util.EnumSet
import javax.inject.Inject

@AndroidEntryPoint
class ReaderFragment : Fragment(R.layout.reader_fragment_layout), ScrollableViewInitializedListener,
    OnScrollToTopListener, SubFilterViewModelProvider {
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

    private var readerSubsActivityResultLauncher: ActivityResultLauncher<Intent>? = null

    private val wpMainActivityViewModel by lazy {
        ViewModelProvider(
            requireActivity(),
            viewModelFactory
        )[WPMainActivityViewModel::class.java]
    }

    // region SubgroupFilterViewModel Observers
    // we need a reference to the observers so they are properly handled by the lifecycle and ViewModel owners, avoiding
    // duplication, and ensuring they are properly removed when the Fragment is destroyed
    private val currentSubfilterObserver = Observer<SubfilterListItem> { subfilterListItem ->
        viewModel.onSubFilterItemSelected(subfilterListItem)
    }

    private val updateTagsAndSitesObserver = Observer<Event<EnumSet<UpdateTask>>> { event ->
        event.applyIfNotHandled {
            if (NetworkUtils.isNetworkAvailable(activity)) {
                ReaderUpdateServiceStarter.startService(activity, this)
            }
        }
    }

    private val subFiltersObserver = Observer<List<SubfilterListItem>> { subFilters ->
        val selectedTag = (viewModel.uiState.value as? ContentUiState)?.selectedReaderTag ?: return@Observer
        viewModel.showTopBarFilterGroup(
            selectedTag,
            subFilters
        )
    }

    private val bottomSheetUiStateObserver = Observer<Event<BottomSheetUiState>> { event ->
        event.applyIfNotHandled {
            val selectedTag = (viewModel.uiState.value as? ContentUiState)?.selectedReaderTag
                ?: return@applyIfNotHandled
            val viewModelKey = SubFilterViewModel.getViewModelKeyForTag(selectedTag)

            val fm = childFragmentManager
            var bottomSheet = fm.findFragmentByTag(SUBFILTER_BOTTOM_SHEET_TAG) as SubfilterBottomSheetFragment?
            if (isVisible && bottomSheet == null) {
                val (title, category) = this as BottomSheetVisible
                bottomSheet = newInstance(
                    viewModelKey,
                    category,
                    uiHelpers.getTextOfUiString(requireContext(), title)
                )
                bottomSheet.show(childFragmentManager, SUBFILTER_BOTTOM_SHEET_TAG)
            } else if (!isVisible && bottomSheet != null) {
                bottomSheet.dismiss()
            }
        }
    }

    private val bottomSheetActionObserver = Observer<Event<ActionType>> { event ->
        event.applyIfNotHandled {
            when (this) {
                is OpenSubsAtPage -> {
                    readerSubsActivityResultLauncher?.launch(
                        ReaderActivityLauncher.createIntentShowReaderSubs(
                            requireActivity(),
                            tabIndex
                        )
                    )
                }

                is OpenLoginPage -> {
                    wpMainActivityViewModel.onOpenLoginPage()
                }

                is OpenSearchPage -> {
                    ReaderActivityLauncher.showReaderSearch(requireActivity())
                }

                is OpenSuggestedTagsPage -> {
                    ReaderActivityLauncher.showReaderInterests(requireActivity())
                }
            }
        }
    }
    // endregion

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
        initReaderSubsActivityResultLauncher()
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

    private fun initReaderSubsActivityResultLauncher() {
        readerSubsActivityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                if (data != null) {
                    val shouldRefreshSubscriptions = data.getBooleanExtra(
                        ReaderSubsActivity.RESULT_SHOULD_REFRESH_SUBSCRIPTIONS,
                        false
                    )
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

                AppThemeM2 {
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
        viewModel = ViewModelProvider(this@ReaderFragment, viewModelFactory)[ReaderViewModel::class.java]
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
            val selectedTag = uiState.selectedReaderTag
            val fragment = when {
                selectedTag.isDiscover -> ReaderDiscoverFragment()
                selectedTag.isTags -> ReaderTagsFeedFragment.newInstance(selectedTag)
                else -> ReaderPostListFragment.newInstanceForTag(
                    selectedTag,
                    ReaderTypes.ReaderPostListType.TAG_FOLLOWED,
                    true,
                    selectedTag.isFilterable
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
        if (!::viewModel.isInitialized) {
            viewModel = ViewModelProvider(this@ReaderFragment, viewModelFactory)[ReaderViewModel::class.java]
        }
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

    override fun onScrollToTop() {
        binding?.appBar?.setExpanded(true, true)
        // Instance of ReaderPostListFragment or ReaderDiscoverFragment
        val currentFragment = getCurrentFeedFragment()
        if (currentFragment is OnScrollToTopListener) {
            currentFragment.onScrollToTop()
        }
    }

    /**
     * The owner of the SubFilterViewModel should be the current feed Fragment, so it can be properly cleared when the
     * feed is changed, since it will be properly tied to the expected feed Fragment lifecycle instead of the
     * [ReaderFragment] lifecycle.
     *
     * This method exists mainly for readability purposes and to avoid passing the Fragment as a parameter.
     *
     * Note: it can cause a crash if the current feed Fragment is not available for any reason, which should never
     * happen since the calling methods are always called by the feed Fragment or their children.
     */
    private fun getSubFilterViewModelOwner(): ViewModelStoreOwner {
        return getCurrentFeedFragment() as ViewModelStoreOwner
    }

    private fun getSubFilterViewModel(): SubFilterViewModel? {
        val selectedTag = (viewModel.uiState.value as? ContentUiState)?.selectedReaderTag ?: return null
        return getSubFilterViewModelForTag(selectedTag)
    }

    /**
     * Get the SubFilterViewModel for the given key. It doesn't initialize the ViewModel if it's not already started, so
     * should only be used for getting a ViewModel that's already been started.
     */
    override fun getSubFilterViewModelForKey(key: String): SubFilterViewModel {
        return ViewModelProvider(getSubFilterViewModelOwner(), viewModelFactory)[key, SubFilterViewModel::class.java]
    }

    override fun getSubFilterViewModelForTag(tag: ReaderTag, savedInstanceState: Bundle?): SubFilterViewModel {
        return ViewModelProvider(getSubFilterViewModelOwner(), viewModelFactory)[
            SubFilterViewModel.getViewModelKeyForTag(tag),
            SubFilterViewModel::class.java
        ].also {
            it.initSubFilterViewModel(tag, savedInstanceState)
        }
    }

    private fun SubFilterViewModel.initSubFilterViewModel(startedTag: ReaderTag, savedInstanceState: Bundle?) {
        bottomSheetUiState.observe(
            viewLifecycleOwner,
            bottomSheetUiStateObserver
        )

        bottomSheetAction.observe(
            viewLifecycleOwner,
            bottomSheetActionObserver
        )

        currentSubFilter.observe(
            viewLifecycleOwner,
            currentSubfilterObserver
        )


        updateTagsAndSites.observe(
            viewLifecycleOwner,
            updateTagsAndSitesObserver
        )

        if (startedTag.isFilterable) {
            subFilters.observe(
                viewLifecycleOwner,
                subFiltersObserver
            )

            updateTagsAndSites()
        } else {
            viewModel.hideTopBarFilterGroup(startedTag)
        }

        start(startedTag, startedTag, savedInstanceState)
    }

    companion object {
        private const val SUBFILTER_BOTTOM_SHEET_TAG = "SUBFILTER_BOTTOM_SHEET_TAG"
    }
}
