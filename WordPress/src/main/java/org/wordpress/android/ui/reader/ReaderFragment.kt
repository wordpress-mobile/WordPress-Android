package org.wordpress.android.ui.reader

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.AndroidFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.WPMainActivity.OnScrollToTopListener
import org.wordpress.android.ui.main.WPMainNavigationView.PageType.READER
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredBottomSheetFragment
import org.wordpress.android.ui.reader.SubfilterBottomSheetFragment.Companion.newInstance
import org.wordpress.android.ui.reader.discover.ReaderDiscoverTabsFragment
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
import org.wordpress.android.ui.reader.views.compose.MeasureOnLayoutComposeView
import org.wordpress.android.ui.reader.views.compose.ReaderShellLayout
import org.wordpress.android.ui.reader.views.compose.ReaderTopAppBar
import org.wordpress.android.ui.reader.views.compose.ReaderTopBarScrollConnection
import org.wordpress.android.ui.reader.views.compose.enableNestedScrollInterop
import org.wordpress.android.ui.reader.views.compose.filter.ReaderFilterType
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.main.WPMainActivityViewModel
import org.wordpress.android.viewmodel.observeEvent
import java.util.EnumSet
import javax.inject.Inject

@AndroidEntryPoint
class ReaderFragment : Fragment(), OnScrollToTopListener, SubFilterViewModelProvider {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var uiHelpers: UiHelpers

    @Inject
    lateinit var snackbarSequencer: SnackbarSequencer

    private lateinit var viewModel: ReaderViewModel

    private val isInterestsVisible = mutableStateOf(false)

    private val topBarScrollConnection = ReaderTopBarScrollConnection()

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, viewModelFactory)[ReaderViewModel::class.java]
        isInterestsVisible.value = savedInstanceState?.getBoolean(KEY_IS_INTERESTS_VISIBLE) == true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        MeasureOnLayoutComposeView(requireContext()) {
            AppThemeM3 {
                ReaderContent()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startReaderViewModel(savedInstanceState)
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

    @Composable
    private fun ReaderContent() {
        val topBarState by viewModel.topBarUiState.observeAsState()
        val uiState by viewModel.uiState.observeAsState()
        val selectedTag = (uiState as? ContentUiState)?.selectedReaderTag
        Box {
            ReaderShellLayout(
                scrollConnection = topBarScrollConnection,
                topBar = {
                    topBarState?.let { state ->
                        // lifted while content is scrolled under it, as AppBarLayout's liftOnScroll did
                        val elevation by animateDpAsState(
                            if (topBarScrollConnection.isContentScrolled) TOP_BAR_LIFTED_ELEVATION else 0.dp,
                            label = "topBarElevation"
                        )
                        Surface(shadowElevation = elevation) {
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
                },
                content = {
                    selectedTag?.let { tag ->
                        key(tag.tagSlug) {
                            ReaderFeed(tag)
                        }
                    }
                }
            )
            if (isInterestsVisible.value) {
                AndroidFragment<ReaderInterestsFragment>(modifier = Modifier.fillMaxSize())
            }
        }
    }

    /**
     * The selected feed's fragment. The key on the tag's slug means changing feeds replaces it, as the old
     * fragment transaction did
     */
    @Composable
    private fun ReaderFeed(tag: ReaderTag) {
        // only report a feed the first time it's shown, not when it's restored
        var hasReportedTag by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            topBarScrollConnection.reset()
            if (!hasReportedTag) {
                hasReportedTag = true
                viewModel.onTagChanged(tag)
            }
        }
        val modifier = Modifier.fillMaxSize()
        when {
            tag.isDiscover -> AndroidFragment<ReaderDiscoverTabsFragment>(modifier) {
                it.enableNestedScrollInterop()
            }
            tag.isTags -> AndroidFragment<ReaderTagsFeedFragment>(
                modifier,
                arguments = remember { ReaderTagsFeedFragment.argumentsFor(tag) }
            ) {
                it.enableNestedScrollInterop()
            }
            else -> AndroidFragment<ReaderPostListFragment>(
                modifier,
                arguments = remember {
                    ReaderPostListFragment.argumentsForTag(
                        tag,
                        ReaderTypes.ReaderPostListType.TAG_FOLLOWED,
                        true,
                        tag.isFilterable
                    )
                }
            ) {
                it.enableNestedScrollInterop()
            }
        }
    }

    private fun startReaderViewModel(savedInstanceState: Bundle?) {
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

        viewModel.showJetpackPoweredBottomSheet.observeEvent(viewLifecycleOwner) {
            JetpackPoweredBottomSheetFragment
                .newInstance(it, READER)
                .show(childFragmentManager, JetpackPoweredBottomSheetFragment.TAG)
        }

        viewModel.start(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_IS_INTERESTS_VISIBLE, isInterestsVisible.value)
        viewModel.onSaveInstanceState(outState)
    }

    fun requestBookmarkTab() {
        viewModel.bookmarkTabRequested()
    }

    fun requestDiscoverTab() {
        viewModel.discoverTabRequested()
    }

    private fun showReaderInterests() {
        isInterestsVisible.value = true
    }

    private fun closeReaderInterests() {
        isInterestsVisible.value = false
    }

    /**
     * The fragment showing the selected feed. AndroidFragment adds it, along with the interests overlay, to this
     * fragment's child fragment manager, which also holds sheets and the post lists' web view caching fragments
     */
    private fun getCurrentFeedFragment(): Fragment? {
        return childFragmentManager.fragments.lastOrNull {
            it is ReaderPostListFragment || it is ReaderTagsFeedFragment || it is ReaderDiscoverTabsFragment
        }
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
        topBarScrollConnection.reset()
        // Instance of ReaderPostListFragment or ReaderDiscoverTabsFragment
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
        private const val KEY_IS_INTERESTS_VISIBLE = "is_interests_visible"
        private val TOP_BAR_LIFTED_ELEVATION = 4.dp
    }
}
