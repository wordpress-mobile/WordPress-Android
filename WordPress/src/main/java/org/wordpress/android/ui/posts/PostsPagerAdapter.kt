package org.wordpress.android.ui.posts

import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.view.ViewGroup
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import java.lang.ref.WeakReference

class PostsPagerAdapter(
    private val pages: List<PostListType>,
    private val site: SiteModel,
    private val authorFilter: AuthorFilterSelection,
    val fm: FragmentManager
) : FragmentStatePagerAdapter(fm) {
    private val listFragments = mutableMapOf<Int, WeakReference<PostListFragment>>()

    override fun getCount(): Int = pages.size

    override fun getItem(position: Int): PostListFragment =
            PostListFragment.newInstance(site, authorFilter, pages[position])

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
