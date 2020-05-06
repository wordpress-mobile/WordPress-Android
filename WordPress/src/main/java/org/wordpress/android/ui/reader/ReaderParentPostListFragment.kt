package org.wordpress.android.ui.reader

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.wordpress.android.R

class ReaderParentPostListFragment : Fragment(R.layout.reader_parent_post_list_fragment) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val demoCollectionAdapter = DemoCollectionAdapter(this)
        val viewPager = view.findViewById<ViewPager2>(R.id.view_pager)
        viewPager.adapter = demoCollectionAdapter
        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = "OBJECT ${(position + 1)}"
        }.attach()
    }

    class DemoCollectionAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 4

        override fun createFragment(position: Int): Fragment {
            return ReaderPostListFragment.newInstance(true)
        }
    }
}
