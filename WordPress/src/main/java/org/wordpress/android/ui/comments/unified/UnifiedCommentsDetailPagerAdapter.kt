package org.wordpress.android.ui.comments.unified

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import java.lang.ref.WeakReference

class UnifiedCommentsDetailPagerAdapter(
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {
    private val listFragments = mutableMapOf<Int, WeakReference<UnifiedCommentDetailsFragment>>()

    override fun getItemCount(): Int = 3 // TODO return size of the actual collection of comments

    override fun createFragment(position: Int): Fragment {
        val fragment = UnifiedCommentDetailsFragment.newInstance()
        listFragments[position] = WeakReference(fragment)
        return fragment
    }
}
