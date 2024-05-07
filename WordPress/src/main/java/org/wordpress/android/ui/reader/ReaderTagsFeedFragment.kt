package org.wordpress.android.ui.reader

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.ViewCompat.animate
import androidx.core.view.isVisible
import androidx.fragment.app.commitNow
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.databinding.ReaderTagFeedFragmentLayoutBinding
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.ViewPagerFragment
import org.wordpress.android.ui.compose.theme.AppThemeWithoutBackground
import org.wordpress.android.ui.main.WPMainActivity
import org.wordpress.android.ui.reader.subfilter.SubFilterViewModel
import org.wordpress.android.ui.reader.subfilter.SubFilterViewModelProvider
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem
import org.wordpress.android.ui.reader.viewmodels.tagsfeed.ReaderTagsFeedViewModel
import org.wordpress.android.ui.reader.viewmodels.tagsfeed.ReaderTagsFeedViewModel.ActionEvent
import org.wordpress.android.ui.reader.views.compose.tagsfeed.ReaderTagsFeed
import org.wordpress.android.util.extensions.getSerializableCompat
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
class ReaderTagsFeedFragment : ViewPagerFragment(R.layout.reader_tag_feed_fragment_layout),
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

    // binding
    private lateinit var binding: ReaderTagFeedFragmentLayoutBinding

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
            viewModel.start(tags)
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
                is ActionEvent.OpenTagPostsFeed -> {
                    subFilterViewModel.setSubfilterFromTag(it.readerTag)
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

    override fun getScrollableViewForUniqueIdProvision(): View {
        return binding.composeView
    }

    override fun onScrollToTop() {
        // TODO scroll current content to top
    }

    companion object {
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
