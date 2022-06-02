package org.wordpress.android.ui.mysite.tabs

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.wordpress.android.ui.mysite.MySiteViewModel.TabsUiState.TabUiState

class MySiteTabsAdapter(
    parent: Fragment,
    private val tabUiStates: List<TabUiState>
) : FragmentStateAdapter(parent) {
    override fun getItemCount(): Int = tabUiStates.size

    override fun createFragment(position: Int) = MySiteTabFragment.newInstance(tabUiStates[position].tabType)
}
