package org.wordpress.android.ui.stats.refresh

import android.content.Context
import android.support.v4.view.PagerAdapter
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import org.wordpress.android.ui.stats.refresh.BlockListItem.TabsItem
import org.wordpress.android.util.image.ImageManager

class BlockTabPagerAdapter(val imageManager: ImageManager, val context: Context, val item: TabsItem) : PagerAdapter() {
    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
//        val layout = LayoutInflater.from(container.context)
//                .inflate(layout.recycler_view, container, false)
        val list = container.getChildAt(position) as RecyclerView
//        val list = layout.findViewById<RecyclerView>(id.recycler_view)
        list.layoutManager = LinearLayoutManager(
                list.context,
                LinearLayoutManager.VERTICAL,
                false
        )
        if (list.adapter == null) {
            list.adapter = BlockListAdapter(imageManager)
        }
        (list.adapter as BlockListAdapter).update(item.tabs[position].items)
        return list
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return context.resources.getString(item.tabs[position].title)
    }

    override fun destroyItem(container: ViewGroup, position: Int, view: Any) {
        container.removeView(view as View)
    }

    override fun getCount(): Int {
        return item.tabs.size
    }
}
