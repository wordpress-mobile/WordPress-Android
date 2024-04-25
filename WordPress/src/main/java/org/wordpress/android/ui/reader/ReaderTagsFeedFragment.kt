package org.wordpress.android.ui.reader

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.databinding.ReaderTagFeedFragmentLayoutBinding
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.ViewPagerFragment
import org.wordpress.android.ui.compose.theme.AppThemeWithoutBackground
import org.wordpress.android.ui.main.WPMainActivity
import org.wordpress.android.ui.reader.services.update.ReaderUpdateServiceStarter
import org.wordpress.android.ui.reader.subfilter.SubFilterViewModel
import org.wordpress.android.ui.reader.subfilter.SubFilterViewModel.Companion.getViewModelKeyForTag
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem
import org.wordpress.android.ui.reader.viewmodels.ReaderTagsFeedViewModel
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel
import org.wordpress.android.ui.reader.views.compose.tagsfeed.ReaderTagsFeed
import org.wordpress.android.util.NetworkUtils
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
    private val readerViewModel: ReaderViewModel by viewModels(
        ownerProducer = { requireParentFragment() }
    )

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

        initViewModels(savedInstanceState)
    }

    private fun initViewModels(savedInstanceState: Bundle?) {
        subFilterViewModel = ViewModelProvider(this, viewModelFactory).get(
            getViewModelKeyForTag(tagsFeedTag),
            SubFilterViewModel::class.java
        )
        subFilterViewModel.start(tagsFeedTag, tagsFeedTag, savedInstanceState)

        subFilterViewModel.updateTagsAndSites.observe(viewLifecycleOwner) { event ->
            event.applyIfNotHandled {
                if (NetworkUtils.isNetworkAvailable(activity)) {
                    ReaderUpdateServiceStarter.startService(activity, this)
                }
            }
        }

        subFilterViewModel.subFilters.observe(viewLifecycleOwner) { subFilters ->
            readerViewModel.showTopBarFilterGroup(
                tagsFeedTag,
                subFilters
            )

            val tags = subFilters.filterIsInstance<SubfilterListItem.Tag>().map { it.tag }
            viewModel.fetchAll(tags)
        }
        subFilterViewModel.updateTagsAndSites()
    }

    override fun getScrollableViewForUniqueIdProvision(): View {
        return binding.composeView
    }

    override fun onScrollToTop() {
        // TODO scroll current content to top
    }

    companion object {
        private const val ARG_TAGS_FEED_TAG = "tags_feed_tag"

        fun newInstance(
            feedTag: ReaderTag
        ): ReaderTagsFeedFragment = ReaderTagsFeedFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_TAGS_FEED_TAG, feedTag)
            }
        }
    }
}
