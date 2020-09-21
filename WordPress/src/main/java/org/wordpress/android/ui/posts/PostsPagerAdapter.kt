package org.wordpress.android.ui.posts

import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import java.lang.ref.WeakReference

class PostsPagerAdapter(
    private val pages: List<PostListType>,
    private val site: SiteModel,
    val fm: FragmentManager
) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    private val listFragments = mutableMapOf<Int, WeakReference<PostListFragment>>()

    override fun getCount(): Int = pages.size

    override fun getItem(position: Int): PostListFragment =
            PostListFragment.newInstance(site, pages[position])

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val fragment = super.instantiateItem(container, position) as PostListFragment
        listFragments[position] = WeakReference(fragment)
        return fragment
    }

    override fun getPageTitle(position: Int): CharSequence? =
            WordPress.getContext().getString(pages[position].titleResId)

    fun getItemAtPosition(position: Int): PostListFragment? {
        return listFragments[position]?.get()
    }
}
