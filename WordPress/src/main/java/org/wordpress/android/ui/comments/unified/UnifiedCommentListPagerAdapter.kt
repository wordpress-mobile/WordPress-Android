package org.wordpress.android.ui.comments.unified

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import java.lang.ref.WeakReference

class UnifiedCommentListPagerAdapter(
    private val pages: List<CommentFilter>,
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {
    private val listFragments = mutableMapOf<Int, WeakReference<UnifiedCommentListFragment>>()

    fun getItemAtPosition(position: Int): UnifiedCommentListFragment? {
        return listFragments[position]?.get()
    }

    override fun getItemCount(): Int = pages.size

    override fun createFragment(position: Int): Fragment {
        val fragment = UnifiedCommentListFragment.newInstance(pages[position])
        listFragments[position] = WeakReference(fragment)
        return fragment
    }
}
