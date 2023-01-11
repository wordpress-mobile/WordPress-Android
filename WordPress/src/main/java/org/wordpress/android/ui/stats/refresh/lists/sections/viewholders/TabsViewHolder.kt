package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.ViewGroup
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayout.Tab
import org.wordpress.android.R
import org.wordpress.android.R.id
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TabsItem
import org.wordpress.android.util.image.ImageManager

class TabsViewHolder(parent: ViewGroup, val imageManager: ImageManager) : BlockListItemViewHolder(
    parent,
    R.layout.stats_block_tabs_item
) {
    private val tabLayout = itemView.findViewById<TabLayout>(id.tab_layout)

    fun bind(item: TabsItem, tabChanged: Boolean) {
        tabLayout.clearOnTabSelectedListeners()
        if (!tabChanged) {
            tabLayout.removeAllTabs()
            item.tabs.forEach { tabItem ->
                tabLayout.addTab(tabLayout.newTab().setText(tabItem))
            }
        }
        tabLayout.getTabAt(item.selectedTabPosition)?.select()

        tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabReselected(tab: Tab) {
            }

            override fun onTabUnselected(tab: Tab) {
            }

            override fun onTabSelected(tab: Tab) {
                item.onTabSelected(tab.position)
            }
        })
    }
}
