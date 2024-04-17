package org.wordpress.android.ui.reader

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.databinding.ReaderTagFeedFragmentLayoutBinding
import org.wordpress.android.ui.ViewPagerFragment
import org.wordpress.android.ui.compose.theme.AppThemeWithoutBackground
import org.wordpress.android.ui.main.WPMainActivity
import org.wordpress.android.ui.reader.viewmodels.ReaderTagsFeedViewModel

/**
 * Initial implementation of ReaderTagFeedFragment with the idea of it containing both a ComposeView, which will host
 * all Compose content related to the new Tags Feed as well as an internal ReaderPostListFragment, which will be used
 * to display "filtered" content based on the currently selected tag on the top app bar filter.
 *
 * It might be tricky to get this working properly since a lot of places expect the ReaderPostListFragment to be the
 * main content of the ReaderFragment (e.g.: initializing the SubFilterViewModel), so a few changes might be needed.
 */
@AndroidEntryPoint
class ReaderTagsFeedFragment : ViewPagerFragment(R.layout.reader_tag_feed_fragment_layout),
    WPMainActivity.OnScrollToTopListener {
    private val viewModel: ReaderTagsFeedViewModel by viewModels()

    // binding
    private lateinit var binding: ReaderTagFeedFragmentLayoutBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = ReaderTagFeedFragmentLayoutBinding.bind(view)

        binding.composeView.setContent {
            AppThemeWithoutBackground {
                val uiState by viewModel.uiStateFlow.collectAsState()
                ReaderTagsFeedScreen(uiState)
            }
        }

        viewModel.fetchAll()
    }

    override fun getScrollableViewForUniqueIdProvision(): View {
        return binding.composeView
    }

    override fun onScrollToTop() {
        // TODO scroll current content to top
    }
}

/**
 * Throwaway UI code just for testing the initial Tags Feed fetching code.
 */
@Composable
private fun ReaderTagsFeedScreen(
    uiState: ReaderTagsFeedViewModel.UiState,
) {
    AppThemeWithoutBackground {
        Column(
            modifier = Modifier
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
                            Text(
                                text = "Error loading posts",
                                style = MaterialTheme.typography.body1,
                            )
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
}
