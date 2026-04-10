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
import org.wordpress.android.ui.ViewPagerFragment
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
class ReaderDiscoverTabsFragment : ViewPagerFragment(R.layout.reader_discover_tabs_fragment),
    OnScrollToTopListener, ScrollableViewInitializedListener {
    @Inject
    lateinit var appPrefsWrapper: AppPrefsWrapper

    private var binding: ReaderDiscoverTabsFragmentBinding? = null

    private val tabs: List<ReaderTag> by lazy {
        listOf(
            createFreshlyPressedTag(),
            createRecommendedTag(),
            createLatestTag(),
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = ReaderDiscoverTabsFragmentBinding.bind(view).also { this.binding = it }

        binding.viewPager.adapter = DiscoverTabsAdapter(this, tabs)

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = getString(tabTitleResFor(position))
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
     * The active inner [ReaderPostListFragment] notifies the parent chain of its
     * scrollable view id during its own [onResume]; we return null here so we
     * don't interfere with that.
     */
    override fun getScrollableViewForUniqueIdProvision(): View? = null

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

    private fun tabTitleResFor(position: Int): Int = when (position) {
        INDEX_FRESHLY_PRESSED -> R.string.reader_discover_tab_freshly_pressed
        INDEX_RECOMMENDED -> R.string.reader_discover_tab_recommended
        INDEX_LATEST -> R.string.reader_discover_tab_latest
        else -> error("Unknown Discover sub-tab position: $position")
    }

    private class DiscoverTabsAdapter(
        fragment: Fragment,
        private val tabs: List<ReaderTag>,
    ) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = tabs.size

        override fun createFragment(position: Int): Fragment {
            return ReaderPostListFragment.newInstanceForTag(
                tabs[position],
                ReaderTypes.ReaderPostListType.TAG_FOLLOWED,
                /* isTopLevel = */ true,
                /* isFilterable = */ false,
            )
        }
    }

    companion object {
        private const val INDEX_FRESHLY_PRESSED = 0
        private const val INDEX_RECOMMENDED = 1
        private const val INDEX_LATEST = 2

        private fun createFreshlyPressedTag(): ReaderTag = ReaderTag(
            ReaderTag.TAG_SLUG_FRESHLY_PRESSED,
            ReaderTag.TAG_TITLE_FRESHLY_PRESSED,
            ReaderTag.TAG_TITLE_FRESHLY_PRESSED,
            ReaderTag.FRESHLY_PRESSED_PATH,
            ReaderTagType.DEFAULT,
        )

        private fun createRecommendedTag(): ReaderTag = ReaderTag(
            ReaderTag.TAG_SLUG_RECOMMENDED,
            ReaderTag.TAG_TITLE_RECOMMENDED,
            ReaderTag.TAG_TITLE_RECOMMENDED,
            ReaderTag.DISCOVER_STREAMS_PATH,
            ReaderTagType.DEFAULT,
        )

        private fun createLatestTag(): ReaderTag = ReaderTag(
            ReaderTag.TAG_SLUG_LATEST,
            ReaderTag.TAG_TITLE_LATEST,
            ReaderTag.TAG_TITLE_LATEST,
            ReaderTag.DISCOVER_STREAMS_PATH,
            ReaderTagType.DEFAULT,
        )
    }
}
