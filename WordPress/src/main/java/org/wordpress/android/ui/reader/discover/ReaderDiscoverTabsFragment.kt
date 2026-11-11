package org.wordpress.android.ui.reader.discover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.AndroidFragment
import androidx.lifecycle.Lifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.WPMainActivity.OnScrollToTopListener
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.ReaderPostListFragment
import org.wordpress.android.ui.reader.ReaderTypes
import org.wordpress.android.ui.reader.views.compose.ReaderScrollingFeed
import org.wordpress.android.ui.reader.views.compose.RetainedComposeView
import org.wordpress.android.ui.reader.views.compose.enableNestedScrollInterop
import javax.inject.Inject

/**
 * Container for the Reader Discover experience. Hosts three sub-tabs
 * (Freshly Pressed, Recommended, Latest), each backed by a
 * [ReaderPostListFragment] driven by an in-memory [ReaderTag].
 *
 * Defaults to Freshly Pressed on first open; restores the last selected
 * sub-tab from [AppPrefsWrapper] on subsequent opens.
 */
@AndroidEntryPoint
class ReaderDiscoverTabsFragment : Fragment(), OnScrollToTopListener, ReaderScrollingFeed {
    @Inject
    lateinit var appPrefsWrapper: AppPrefsWrapper

    // state, so the Reader's top bar follows the current page's list
    private val pageFragments = mutableStateMapOf<Int, ReaderPostListFragment>()
    private var currentPage by mutableIntStateOf(0)

    private val contentView = RetainedComposeView(this) {
        AppThemeM3 {
            DiscoverTabs()
        }
    }

    override val feedListState get() = pageFragments[currentPage]?.feedListState

    private val tabs: List<DiscoverSubTab> by lazy {
        listOf(
            DiscoverSubTab(
                tag = createDiscoverTag(
                    ReaderTag.TAG_SLUG_FRESHLY_PRESSED,
                    ReaderTag.TAG_TITLE_FRESHLY_PRESSED,
                    FRESHLY_PRESSED_PATH,
                ),
                titleRes = R.string.reader_discover_tab_freshly_pressed,
            ),
            DiscoverSubTab(
                tag = createDiscoverTag(
                    ReaderTag.TAG_SLUG_RECOMMENDED,
                    ReaderTag.TAG_TITLE_RECOMMENDED,
                    DISCOVER_STREAMS_PATH,
                ),
                titleRes = R.string.reader_discover_tab_recommended,
            ),
            DiscoverSubTab(
                tag = createDiscoverTag(
                    ReaderTag.TAG_SLUG_LATEST,
                    ReaderTag.TAG_TITLE_LATEST,
                    LATEST_PATH,
                ),
                titleRes = R.string.reader_discover_tab_latest,
            ),
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        contentView.view()

    override fun onDestroyView() {
        contentView.onDestroyView()
        super.onDestroyView()
    }

    @Composable
    private fun DiscoverTabs() {
        // restore the last selected sub-tab (defaults to 0 == Freshly Pressed on first open)
        val pagerState = rememberPagerState(
            initialPage = resolveInitialTabIndex(appPrefsWrapper.readerDiscoverSelectedSubTabIndex, tabs.size)
        ) { tabs.size }
        // a page's list is only created once the page is shown, as ViewPager2 did, and then kept
        var shownPages by rememberSaveable { mutableStateOf(listOf(pagerState.currentPage)) }
        val scope = rememberCoroutineScope()
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.settledPage }.collect { page ->
                currentPage = page
                appPrefsWrapper.readerDiscoverSelectedSubTabIndex = page
            }
        }
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.layoutInfo.visiblePagesInfo.map { it.index } }.collect { visible ->
                if (!shownPages.containsAll(visible)) shownPages = (shownPages + visible).distinct()
            }
        }
        // passes the lists' scrolling on, so the Reader's top bar still hides as they scroll
        Column(modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection())) {
            // scrollable, so large font sizes scroll the tabs rather than wrapping their labels mid-word
            PrimaryScrollableTabRow(selectedTabIndex = pagerState.currentPage, edgePadding = 0.dp) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(stringResource(tab.titleRes), maxLines = 1) }
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                // keeps every shown list alive, as ViewPager2 did, so switching tabs doesn't reload them
                beyondViewportPageCount = tabs.size - 1,
                key = { tabs[it].tag.tagSlug },
                modifier = Modifier.fillMaxSize()
            ) { page ->
                if (page in shownPages) {
                    DiscoverPage(page, isCurrent = page == pagerState.settledPage)
                }
            }
        }
    }

    @Composable
    private fun DiscoverPage(page: Int, isCurrent: Boolean) {
        val arguments = remember(page) {
            ReaderPostListFragment.argumentsForTag(
                tabs[page].tag,
                ReaderTypes.ReaderPostListType.TAG_FOLLOWED,
                isTopLevel = true,
                isFilterable = false
            )
        }
        AndroidFragment<ReaderPostListFragment>(
            modifier = Modifier.fillMaxSize(),
            arguments = arguments,
            // only the current page is resumed, as FragmentStateAdapter did, since the lists refresh on resume
            maxLifecycle = if (isCurrent) Lifecycle.State.RESUMED else Lifecycle.State.STARTED
        ) { fragment ->
            pageFragments[page] = fragment
            fragment.enableNestedScrollInterop()
        }
    }

    override fun onScrollToTop() {
        pageFragments[currentPage]?.onScrollToTop()
    }

    private data class DiscoverSubTab(val tag: ReaderTag, val titleRes: Int)

    companion object {
        // Freshly Pressed uses the v1.2 /freshly-pressed endpoint (matching web and iOS).
        private const val FRESHLY_PRESSED_PATH = "freshly-pressed"
        // Recommended uses the editorially-curated v2 cards endpoint.
        private const val DISCOVER_STREAMS_PATH = "read/streams/discover"
        // Latest reads from the standard v2 tags/posts endpoint (matching web), which
        // returns posts in date-descending order and supports before=<date> pagination.
        private const val LATEST_PATH = "read/tags/posts"

        private fun createDiscoverTag(slug: String, title: String, endpoint: String): ReaderTag =
            ReaderTag(slug, title, title, endpoint, ReaderTagType.DEFAULT)

        /**
         * Clamps a persisted sub-tab index into the current tab range. Returns 0 when
         * the tab list is empty so callers can safely hand the result to ViewPager2.
         */
        @JvmStatic
        internal fun resolveInitialTabIndex(storedIndex: Int, tabCount: Int): Int {
            if (tabCount <= 0) return 0
            return storedIndex.coerceIn(0, tabCount - 1)
        }
    }
}
