package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.support.design.widget.TabLayout
import android.support.design.widget.TabLayout.OnTabSelectedListener
import android.support.design.widget.TabLayout.Tab
import android.view.ViewGroup
import org.wordpress.android.R
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
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
