package org.wordpress.android.e2e.pages

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import org.wordpress.android.R
import org.wordpress.android.support.WPSupportUtils

object PostsListPage {
    @JvmStatic
    fun tapPostWithName(name: String?) {
        WPSupportUtils.scrollToAndClickOnTextInRecyclerView(name, recyclerView)
    }

    @JvmStatic
    fun scrollToTop() {
        WPSupportUtils.scrollToTopOfRecyclerView(recyclerView)
    }

    // Workaround for cases when recyclerview id missing
    private val recyclerView: RecyclerView?
        get() {
            val pager = WPSupportUtils.getCurrentActivity().findViewById<ViewPager>(R.id.postPager)
            var recyclerView = pager.getChildAt(pager.currentItem)
                .findViewById<View>(R.id.recycler_view) as RecyclerView
            return recyclerView
        }

    @JvmStatic
    fun goToDrafts() {
        WPSupportUtils.selectItemWithTitleInTabLayout(
            WPSupportUtils.getTranslatedString(R.string.post_list_tab_drafts),
            R.id.tabLayout
        )
    }
}
