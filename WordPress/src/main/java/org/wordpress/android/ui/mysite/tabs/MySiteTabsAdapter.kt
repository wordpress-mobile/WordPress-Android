package org.wordpress.android.ui.mysite.tabs

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class MySiteTabsAdapter(
    parent: Fragment,
    private val orderedTabTypes: List<MySiteTabType>
) : FragmentStateAdapter(parent) {
    override fun getItemCount(): Int = orderedTabTypes.size

    override fun createFragment(position: Int) = MySiteTabFragment.newInstance(orderedTabTypes[position])
}
