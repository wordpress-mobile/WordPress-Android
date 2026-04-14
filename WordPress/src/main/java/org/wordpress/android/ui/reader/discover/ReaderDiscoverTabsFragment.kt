package org.wordpress.android.ui.reader.discover

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.databinding.ReaderDiscoverTabsFragmentBinding
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.ui.ScrollableViewInitializedListener
import org.wordpress.android.ui.main.WPMainActivity.OnScrollToTopListener
import org.wordpress.android.ui.reader.ReaderPostListFragment
import org.wordpress.android.ui.reader.ReaderTypes
import org.wordpress.android.ui.prefs.AppPrefsWrapper
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
class ReaderDiscoverTabsFragment : Fragment(R.layout.reader_discover_tabs_fragment),
    OnScrollToTopListener, ScrollableViewInitializedListener {
    @Inject
    lateinit var appPrefsWrapper: AppPrefsWrapper

    private var binding: ReaderDiscoverTabsFragmentBinding? = null

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
                    DISCOVER_STREAMS_PATH,
                ),
                titleRes = R.string.reader_discover_tab_latest,
            ),
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = ReaderDiscoverTabsFragmentBinding.bind(view).also { this.binding = it }

        binding.viewPager.adapter = DiscoverTabsAdapter(this, tabs.map { it.tag })

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = getString(tabs[position].titleRes)
        }.attach()

        // Restore last selected sub-tab (defaults to 0 == Freshly Pressed on first open).
        val initialIndex = appPrefsWrapper.readerDiscoverSelectedSubTabIndex
            .coerceIn(0, tabs.lastIndex)
        binding.viewPager.setCurrentItem(initialIndex, false)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                appPrefsWrapper.readerDiscoverSelectedSubTabIndex = position
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    /**
     * Forwards the scrollable view id notification from the active inner
     * [ReaderPostListFragment] up to the outer [org.wordpress.android.ui.reader.ReaderFragment]
     * so that the Reader's lift-on-scroll AppBar keeps working.
     */
    override fun onScrollableViewInitialized(containerId: Int) {
        (parentFragment as? ScrollableViewInitializedListener)
            ?.onScrollableViewInitialized(containerId)
    }

    /**
     * Forwards scroll-to-top to the currently-visible child fragment using the
     * default [FragmentStateAdapter] tag convention ("f{position}").
     */
    override fun onScrollToTop() {
        val currentItem = binding?.viewPager?.currentItem ?: return
        val tag = "f$currentItem"
        (childFragmentManager.findFragmentByTag(tag) as? OnScrollToTopListener)?.onScrollToTop()
    }

    private class DiscoverTabsAdapter(
        fragment: Fragment,
        private val tags: List<ReaderTag>,
    ) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = tags.size

        override fun createFragment(position: Int): Fragment {
            return ReaderPostListFragment.newInstanceForTag(
                tags[position],
                ReaderTypes.ReaderPostListType.TAG_FOLLOWED,
                /* isTopLevel = */ true,
                /* isFilterable = */ false,
            )
        }
    }

    private data class DiscoverSubTab(val tag: ReaderTag, val titleRes: Int)

    companion object {
        // Freshly Pressed uses the v1.2 /freshly-pressed endpoint (matching web and iOS).
        private const val FRESHLY_PRESSED_PATH = "freshly-pressed"
        private const val DISCOVER_STREAMS_PATH = "read/streams/discover"

        private fun createDiscoverTag(slug: String, title: String, endpoint: String): ReaderTag =
            ReaderTag(slug, title, title, endpoint, ReaderTagType.DEFAULT)
    }
}
