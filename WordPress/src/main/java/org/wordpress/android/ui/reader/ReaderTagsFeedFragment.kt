package org.wordpress.android.ui.reader

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import org.wordpress.android.ui.reader.subfilter.SubFilterViewModelOwner
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem
import org.wordpress.android.ui.reader.viewmodels.ReaderTagsFeedViewModel
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel
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
                ReaderTagsFeedScreen(
                    uiState = uiState,
                    onRetryClicked = viewModel::fetchTag,
                )
            }
        }

        initViewModels(savedInstanceState)
    }

    private fun initViewModels(savedInstanceState: Bundle?) {
        subFilterViewModel = SubFilterViewModelOwner.getSubFilterViewModelForTag(this, tagsFeedTag, savedInstanceState)

        subFilterViewModel.subFilters.observe(viewLifecycleOwner) { subFilters ->
            val tags = subFilters.filterIsInstance<SubfilterListItem.Tag>().map { it.tag }
            viewModel.fetchAll(tags)
        }
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

/**
 * Throwaway UI code just for testing the initial Tags Feed fetching code.
 * TODO remove this and replace with the final Compose content.
 */
@Composable
private fun ReaderTagsFeedScreen(
    uiState: ReaderTagsFeedViewModel.UiState,
    onRetryClicked: (ReaderTag) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        uiState.tagStates.forEach { (tag, fetchState) ->
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = tag.tagTitle,
                    style = MaterialTheme.typography.h4,
                )

                when (fetchState) {
                    is ReaderTagsFeedViewModel.FetchState.Loading -> {
                        Text(
                            text = "Loading...",
                            style = MaterialTheme.typography.body1,
                        )
                    }

                    is ReaderTagsFeedViewModel.FetchState.Error -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = "Error loading posts.",
                                style = MaterialTheme.typography.body1,
                            )

                            Text(
                                text = "Retry",
                                style = MaterialTheme.typography.body1,
                                textDecoration = TextDecoration.Underline,
                                color = MaterialTheme.colors.primary,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .clickable { onRetryClicked(tag) },
                            )
                        }
                    }

                    is ReaderTagsFeedViewModel.FetchState.Success -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                        ) {
                            fetchState.posts.forEach { post ->
                                Column(
                                    modifier = Modifier
                                        .width(300.dp)
                                        .background(
                                            MaterialTheme.colors.surface,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = post.title,
                                        style = MaterialTheme.typography.h5,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )

                                    Text(
                                        text = post.excerpt,
                                        style = MaterialTheme.typography.body1,
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
