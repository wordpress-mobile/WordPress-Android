package org.wordpress.android.ui.reader

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.ListPopupWindow
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.ViewCompat.animate
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commitNow
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.databinding.ReaderTagFeedFragmentLayoutBinding
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.compose.theme.AppThemeWithoutBackground
import org.wordpress.android.ui.main.WPMainActivity
import org.wordpress.android.ui.reader.adapters.ReaderMenuAdapter
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsFragment
import org.wordpress.android.ui.reader.subfilter.SubFilterViewModel
import org.wordpress.android.ui.reader.subfilter.SubFilterViewModelProvider
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.reader.viewmodels.tagsfeed.ReaderTagsFeedViewModel
import org.wordpress.android.ui.reader.viewmodels.tagsfeed.ReaderTagsFeedViewModel.ActionEvent
import org.wordpress.android.ui.reader.views.compose.tagsfeed.ReaderTagsFeed
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.getSerializableCompat
import org.wordpress.android.viewmodel.observeEvent
import org.wordpress.android.widgets.WPSnackbar
import javax.inject.Inject

/**
 * Initial implementation of ReaderTagsFeedFragment with the idea of it containing both a ComposeView, which will host
 * all Compose content related to the new Tags Feed as well as an internal ReaderPostListFragment, which will be used
 * to display "filtered" content based on the currently selected tag on the top app bar filter.
 *
 * It might be tricky to get this working properly since a lot of places expect the ReaderPostListFragment to be the
 * main content of the ReaderFragment (e.g.: initializing the SubFilterViewModel), so a few changes might be needed.
 */
@AndroidEntryPoint
class ReaderTagsFeedFragment : Fragment(R.layout.reader_tag_feed_fragment_layout),
    WPMainActivity.OnScrollToTopListener {
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

    // binding
    private lateinit var binding: ReaderTagFeedFragmentLayoutBinding

    private var bookmarksSavedLocallyDialog: AlertDialog? = null

    private var readerPostListActivityResultLauncher: ActivityResultLauncher<Intent>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = ReaderTagFeedFragmentLayoutBinding.bind(view)

        binding.composeView.setContent {
            AppThemeWithoutBackground {
                val uiState by viewModel.uiStateFlow.collectAsState()
                ReaderTagsFeed(uiState)
            }
        }
        observeSubFilterViewModel(savedInstanceState)
        observeActionEvents()
        observeNavigationEvents()
        observeErrorMessageEvents()
        observeSnackbarEvents()
        observeOpenMoreMenuEvents()
        viewModel.onViewCreated()
    }

    override fun onDestroy() {
        super.onDestroy()
        bookmarksSavedLocallyDialog?.dismiss()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        initReaderPostListActivityResultLauncher()
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
            if (subFilter is SubfilterListItem.Tag) {
                showTagPostList(subFilter.tag)
            } else {
                hideTagPostList()
            }
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
                    val readerInterestsFragment = childFragmentManager.findFragmentByTag(ReaderInterestsFragment.TAG)
                    if (readerInterestsFragment == null) {
                        (parentFragment as? ReaderFragment)?.childFragmentManager?.beginTransaction()?.replace(
                            R.id.interests_fragment_container,
                            ReaderInterestsFragment(),
                            ReaderInterestsFragment.TAG
                        )?.commitNow()
                    }
                }
            }
        }
    }

    private fun showTagPostList(tag: ReaderTag) {
        startPostListFragment(tag)
        binding.postListContainer.fadeIn(
            withEndAction = { binding.composeView.isVisible = false },
        )
    }

    private fun hideTagPostList() {
        binding.composeView.isVisible = true
        binding.postListContainer.fadeOut(
            withEndAction = { removeCurrentPostListFragment() },
        )
    }

    private fun startPostListFragment(tag: ReaderTag) {
        val tagPostListFragment = ReaderPostListFragment.newInstanceForTag(
            tag,
            ReaderTypes.ReaderPostListType.TAG_FOLLOWED
        )

        childFragmentManager.commitNow {
            replace(R.id.post_list_container, tagPostListFragment)
        }
    }

    private fun removeCurrentPostListFragment() {
        childFragmentManager.run {
            findFragmentById(R.id.post_list_container)?.let {
                commitNow {
                    remove(it)
                }
            }
        }
    }

    private fun View.fadeIn(
        withEndAction: (() -> Unit)? = null
    ) {
        alpha = 0f
        isVisible = true

        animate(this)
            // add quick delay to give time for the fragment to be added and load some content
            .setStartDelay(POST_LIST_FADE_IN_DELAY)
            .setDuration(POST_LIST_FADE_DURATION)
            .withEndAction { withEndAction?.invoke() }
            .alpha(1f)
    }

    private fun View.fadeOut(
        withEndAction: (() -> Unit)? = null,
    ) {
        animate(this)
            .withEndAction {
                isVisible = false
                alpha = 1f
                withEndAction?.invoke()
            }
            .setDuration(POST_LIST_FADE_DURATION)
            .alpha(0f)
    }

    @Suppress("LongMethod")
    private fun observeNavigationEvents() {
        viewModel.navigationEvents.observeEvent(viewLifecycleOwner) { event ->
            when (event) {
                is ReaderNavigationEvents.ShowPostDetail -> ReaderActivityLauncher.showReaderPostDetail(
                    context,
                    event.post.blogId,
                    event.post.postId
                )

                is ReaderNavigationEvents.SharePost -> ReaderActivityLauncher.sharePost(context, event.post)
                is ReaderNavigationEvents.OpenPost -> ReaderActivityLauncher.openPost(context, event.post)
                is ReaderNavigationEvents.ShowBookmarkedSavedOnlyLocallyDialog -> {
                    showBookmarkSavedLocallyDialog(event)
                }

                is ReaderNavigationEvents.ShowBlogPreview -> ReaderActivityLauncher.showReaderBlogOrFeedPreview(
                    context,
                    event.siteId,
                    event.feedId,
                    event.isFollowed,
                    ReaderTracker.SOURCE_TAGS_FEED,
                    readerTracker
                )

                is ReaderNavigationEvents.ShowReportPost -> ReaderActivityLauncher.openUrl(
                    context,
                    readerUtilsWrapper.getReportPostUrl(event.url),
                    ReaderActivityLauncher.OpenUrlType.INTERNAL
                )

                is ReaderNavigationEvents.ShowReportUser -> ReaderActivityLauncher.openUrl(
                    context,
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
                WPSnackbar.make(binding.root, getString(stringRes), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun observeSnackbarEvents() {
        viewModel.snackbarEvents.observeEvent(viewLifecycleOwner) { snackbarMessageHolder ->
            if (isAdded) {
                with(snackbarMessageHolder) {
                    val snackbar = WPSnackbar.make(
                        binding.root,
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

    private fun observeOpenMoreMenuEvents() {
        viewModel.openMoreMenuEvents.observe(viewLifecycleOwner) {
            val readerCardUiState = it.readerCardUiState
            val blogId = readerCardUiState.blogId
            val postId = readerCardUiState.postId
            val anchorView = binding.composeView.findViewWithTag<View>("$blogId$postId")
            if (anchorView != null) {
                readerTracker.track(AnalyticsTracker.Stat.POST_CARD_MORE_TAPPED)
                val listPopup = ListPopupWindow(anchorView.context)
                listPopup.width = anchorView.context.resources.getDimensionPixelSize(R.dimen.menu_item_width)
                listPopup.setAdapter(ReaderMenuAdapter(anchorView.context, uiHelpers, it.readerPostCardActions))
                listPopup.setDropDownGravity(Gravity.END)
                listPopup.anchorView = anchorView
                listPopup.isModal = true
                listPopup.setOnItemClickListener { _, _, position, _ ->
                    listPopup.dismiss()
                    val item = it.readerPostCardActions[position]
                    item.onClicked?.invoke(postId, blogId, item.type)
                }
                listPopup.setOnDismissListener { readerCardUiState.onMoreDismissed.invoke(readerCardUiState) }
                listPopup.show()
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
        private const val POST_LIST_FADE_DURATION = 250L
        private const val POST_LIST_FADE_IN_DELAY = 300L

        fun newInstance(
            feedTag: ReaderTag
        ): ReaderTagsFeedFragment = ReaderTagsFeedFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_TAGS_FEED_TAG, feedTag)
            }
        }
    }
}
