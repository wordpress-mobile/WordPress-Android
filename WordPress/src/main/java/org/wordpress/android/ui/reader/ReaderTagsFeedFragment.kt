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
import androidx.appcompat.app.AlertDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.compose.AndroidFragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.compose.theme.AppThemeM3WithoutBackground
import org.wordpress.android.ui.main.WPMainActivity
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents
import org.wordpress.android.ui.reader.subfilter.SubFilterViewModel
import org.wordpress.android.ui.reader.subfilter.SubFilterViewModelProvider
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel
import org.wordpress.android.ui.reader.viewmodels.tagsfeed.ReaderTagsFeedViewModel
import org.wordpress.android.ui.reader.viewmodels.tagsfeed.ReaderTagsFeedViewModel.ActionEvent
import org.wordpress.android.ui.reader.views.compose.RetainedComposeView
import org.wordpress.android.ui.reader.views.compose.enableNestedScrollInterop
import org.wordpress.android.ui.reader.views.compose.tagsfeed.ReaderTagsFeed
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.getSerializableCompat
import org.wordpress.android.viewmodel.observeEvent
import org.wordpress.android.widgets.WPSnackbar
import javax.inject.Inject

/**
 * The Tags feed: a row of recent posts for each followed tag. Picking a tag in the top bar's filter fades in that
 * tag's post list over the rows.
 */
@AndroidEntryPoint
class ReaderTagsFeedFragment : Fragment(), WPMainActivity.OnScrollToTopListener {
    private val tagsFeedTag by lazy {
        // TODO maybe we can just create a static function somewhere that returns the Tags Feed ReaderTag, since it's
        //  used in multiple places, client-side only, and always the same.
        requireArguments().getSerializableCompat<ReaderTag>(ARG_TAGS_FEED_TAG)!!
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var subFilterViewModel: SubFilterViewModel

    private val viewModel: ReaderTagsFeedViewModel by viewModels()

    @Inject
    lateinit var readerUtilsWrapper: ReaderUtilsWrapper

    @Inject
    lateinit var readerTracker: ReaderTracker

    @Inject
    lateinit var uiHelpers: UiHelpers

    private var bookmarksSavedLocallyDialog: AlertDialog? = null

    private var readerPostListActivityResultLauncher: ActivityResultLauncher<Intent>? = null

    // the tag the top bar's filter is set to, if any, and the last one shown, which stays while the list fades out
    private var filteredTag by mutableStateOf<ReaderTag?>(null)
    private var shownTag by mutableStateOf<ReaderTag?>(null)

    // retained, since the filtered tag's list is a fragment hosted with AndroidFragment
    private val contentView = RetainedComposeView(this) {
        AppThemeM3WithoutBackground {
            TagsFeedContent()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        contentView.view()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeSubFilterViewModel(savedInstanceState)
        observeActionEvents()
        observeNavigationEvents()
        observeErrorMessageEvents()
        observeSnackbarEvents()
        viewModel.onViewCreated()
    }

    override fun onDestroyView() {
        contentView.onDestroyView()
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        bookmarksSavedLocallyDialog?.dismiss()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        initReaderPostListActivityResultLauncher()
    }

    @Composable
    private fun TagsFeedContent() {
        val overlayState = remember { MutableTransitionState(false) }
        overlayState.targetState = filteredTag != null
        // once the list has faded in, the rows under it are hidden from drawing and from TalkBack, but stay composed
        // so they keep their scroll position
        val isFeedCovered = overlayState.isIdle && overlayState.currentState
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = if (isFeedCovered) Modifier.alpha(0f).clearAndSetSemantics {} else Modifier
            ) {
                val uiState by viewModel.uiStateFlow.collectAsState()
                ReaderTagsFeed(uiState)
            }
            AnimatedVisibility(
                visibleState = overlayState,
                // passes the list's scrolling on, so the Reader's top bar still hides as it scrolls
                modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection()),
                // delayed to give the list time to load some content
                enter = fadeIn(tween(POST_LIST_FADE_DURATION, delayMillis = POST_LIST_FADE_IN_DELAY)),
                exit = fadeOut(tween(POST_LIST_FADE_DURATION))
            ) {
                shownTag?.let { tag ->
                    key(tag.tagSlug) {
                        AndroidFragment<ReaderPostListFragment>(
                            modifier = Modifier.fillMaxSize(),
                            arguments = remember {
                                ReaderPostListFragment.argumentsForTag(tag, ReaderTypes.ReaderPostListType.TAG_FOLLOWED)
                            }
                        ) { it.enableNestedScrollInterop() }
                    }
                }
            }
        }
    }

    private fun observeSubFilterViewModel(savedInstanceState: Bundle?) {
        subFilterViewModel = SubFilterViewModelProvider.getSubFilterViewModelForTag(
            this,
            tagsFeedTag,
            savedInstanceState
        )

        // TODO not triggered when there's no internet, so the error/no connection UI is not shown.
        subFilterViewModel.subFilters.observe(viewLifecycleOwner) { subFilters ->
            val tags = subFilters.filterIsInstance<SubfilterListItem.Tag>().map { it.tag }
            viewModel.onTagsChanged(tags)
        }

        subFilterViewModel.currentSubFilter.observe(viewLifecycleOwner) { subFilter ->
            val tag = (subFilter as? SubfilterListItem.Tag)?.tag
            if (tag != null) shownTag = tag
            filteredTag = tag
        }
    }

    private fun observeActionEvents() {
        viewModel.actionEvents.observe(viewLifecycleOwner) {
            when (it) {
                is ActionEvent.FilterTagPostsFeed -> {
                    subFilterViewModel.setSubfilterFromTag(it.readerTag)
                }

                is ActionEvent.OpenTagPostList -> {
                    if (!isAdded) {
                        return@observe
                    }
                    readerTracker.trackTag(
                        Stat.READER_TAG_PREVIEWED,
                        it.readerTag.tagSlug,
                        ReaderTracker.SOURCE_TAGS_FEED
                    )
                    readerPostListActivityResultLauncher?.launch(
                        ReaderActivityLauncher.createReaderTagPreviewIntent(
                            requireActivity(), it.readerTag, ReaderTracker.SOURCE_TAGS_FEED
                        )
                    )
                }

                ActionEvent.RefreshTags -> {
                    subFilterViewModel.updateTagsAndSites()
                }

                ActionEvent.ShowTagsList -> {
                    ViewModelProvider(requireParentFragment())[ReaderViewModel::class.java].onShowReaderInterests()
                }
            }
        }
    }

    @Suppress("LongMethod")
    private fun observeNavigationEvents() {
        viewModel.navigationEvents.observeEvent(viewLifecycleOwner) { event ->
            when (event) {
                is ReaderNavigationEvents.ShowPostDetail -> ReaderActivityLauncher.showReaderPostDetail(
                    requireActivity(),
                    event.post.blogId,
                    event.post.postId
                )

                is ReaderNavigationEvents.SharePost -> ReaderActivityLauncher.sharePost(requireActivity(), event.post)
                is ReaderNavigationEvents.OpenPost -> ReaderActivityLauncher.openPost(requireActivity(), event.post)
                is ReaderNavigationEvents.ShowBookmarkedSavedOnlyLocallyDialog -> {
                    showBookmarkSavedLocallyDialog(event)
                }

                is ReaderNavigationEvents.ShowBlogPreview -> ReaderActivityLauncher.showReaderBlogOrFeedPreview(
                    requireActivity(),
                    event.siteId,
                    event.feedId,
                    event.isFollowed,
                    ReaderTracker.SOURCE_TAGS_FEED,
                    readerTracker
                )

                is ReaderNavigationEvents.ShowReportPost -> ReaderActivityLauncher.openUrl(
                    requireActivity(),
                    readerUtilsWrapper.getReportPostUrl(event.url),
                    ReaderActivityLauncher.OpenUrlType.INTERNAL
                )

                is ReaderNavigationEvents.ShowReportUser -> ReaderActivityLauncher.openUrl(
                    requireActivity(),
                    readerUtilsWrapper.getReportUserUrl(event.url, event.authorId),
                    ReaderActivityLauncher.OpenUrlType.INTERNAL
                )

                else -> Unit // Do Nothing
            }
        }
    }

    private fun observeErrorMessageEvents() {
        viewModel.errorMessageEvents.observeEvent(viewLifecycleOwner) { stringRes ->
            if (isAdded) {
                WPSnackbar.make(snackbarParent(), getString(stringRes), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    // the main activity's coordinator, which sits above the bottom navigation
    private fun snackbarParent(): View = requireActivity().findViewById(R.id.coordinator) ?: requireView()

    private fun observeSnackbarEvents() {
        viewModel.snackbarEvents.observeEvent(viewLifecycleOwner) { snackbarMessageHolder ->
            if (isAdded) {
                with(snackbarMessageHolder) {
                    val snackbar = WPSnackbar.make(
                        snackbarParent(),
                        uiHelpers.getTextOfUiString(requireContext(), message),
                        Snackbar.LENGTH_LONG
                    )
                    if (buttonTitle != null) {
                        snackbar.setAction(uiHelpers.getTextOfUiString(requireContext(), buttonTitle)) {
                            buttonAction.invoke()
                        }
                    }
                    snackbar.show()
                }
            }
        }
    }

    private fun showBookmarkSavedLocallyDialog(
        bookmarkDialog: ReaderNavigationEvents.ShowBookmarkedSavedOnlyLocallyDialog
    ) {
        // TODO show bookmark saved dialog?
        bookmarkDialog.buttonLabel
        if (bookmarksSavedLocallyDialog == null) {
            MaterialAlertDialogBuilder(requireActivity())
                .setTitle(getString(bookmarkDialog.title))
                .setMessage(getString(bookmarkDialog.message))
                .setPositiveButton(getString(bookmarkDialog.buttonLabel)) { _, _ ->
                    bookmarkDialog.okButtonAction.invoke()
                }
                .setOnDismissListener {
                    bookmarksSavedLocallyDialog = null
                }
                .setCancelable(false)
                .create()
                .let {
                    bookmarksSavedLocallyDialog = it
                    it.show()
                }
        }
    }

    private fun initReaderPostListActivityResultLauncher() {
        readerPostListActivityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                if (data != null) {
                    val shouldRefreshTagsFeed = data.getBooleanExtra(RESULT_SHOULD_REFRESH_TAGS_FEED, false)
                    if (shouldRefreshTagsFeed) {
                        viewModel.onBackFromTagDetails()
                    }
                }
            }
        }
    }

    override fun onScrollToTop() {
        // TODO scroll current content to top
    }

    companion object {
        const val RESULT_SHOULD_REFRESH_TAGS_FEED = "RESULT_SHOULD_REFRESH_TAGS_FEED"

        private const val ARG_TAGS_FEED_TAG = "tags_feed_tag"
        private const val POST_LIST_FADE_DURATION = 250
        private const val POST_LIST_FADE_IN_DELAY = 300

        /*
         * arguments for the tags feed, which the Reader tab hosts with AndroidFragment
         */
        fun argumentsFor(feedTag: ReaderTag) = Bundle().apply {
            putSerializable(ARG_TAGS_FEED_TAG, feedTag)
        }
    }
}
