package org.wordpress.android.imageeditor.preview

import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator.TabConfigurationStrategy

/**
 * Default simplified [RecyclerView.AdapterDataObserver] set on the [ViewPager2] removes all tabs and recreates them
 * every-time the adapter is refreshed, causing flickering. This custom observer watches changes to
 * [RecyclerView.Adapter] set on the [ViewPager2] and creates a new tab on the tabLayout only if it doesn't exist.
 */
class PagerAdapterObserver(
    private val tabLayout: TabLayout,
    private val viewPager: ViewPager2,
    private val tabConfigurationStrategy: TabConfigurationStrategy
) : RecyclerView.AdapterDataObserver() {
    override fun onChanged() {
        updateTabsFromPagerAdapter()
    }

    override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
        updateTabsFromPagerAdapter()
    }

    override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
        updateTabsFromPagerAdapter()
    }

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        updateTabsFromPagerAdapter()
    }

    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
        updateTabsFromPagerAdapter()
    }

    override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
        updateTabsFromPagerAdapter()
    }

    private fun updateTabsFromPagerAdapter() {
        viewPager.adapter?.let {
            val adapterCount = it.itemCount
            for (i in 0 until adapterCount) {
                val existingTab = tabLayout.getTabAt(i)
                val tab = existingTab ?: tabLayout.newTab()
                tabConfigurationStrategy.onConfigureTab(tab, i)
                if (existingTab == null) {
                    tabLayout.addTab(tab, false)
                }
            }
            // Make sure we reflect the currently set ViewPager item
            if (adapterCount > 0) {
                val lastItem = tabLayout.tabCount - 1
                val currItem = Math.min(viewPager.currentItem, lastItem)
                if (currItem != tabLayout.selectedTabPosition) {
                    tabLayout.selectTab(tabLayout.getTabAt(currItem))
                }
            }
        }
    }
}
