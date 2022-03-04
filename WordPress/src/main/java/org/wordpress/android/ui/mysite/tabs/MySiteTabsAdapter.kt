package org.wordpress.android.ui.mysite.tabs

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.wordpress.android.ui.utils.UiString

class MySiteTabsAdapter(
    parent: Fragment,
    private val tabTitles: List<UiString>
) : FragmentStateAdapter(parent) {
    override fun getItemCount(): Int = tabTitles.size

    override fun createFragment(position: Int) = if (position == 0) {
        MySiteTabFragment.newInstance(MySiteTabFragment.MY_SITE_TAB_TYPE_SITE_MENU)
    } else {
        MySiteTabFragment.newInstance(MySiteTabFragment.MY_SITE_TAB_TYPE_DASHBOARD)
    }
}
