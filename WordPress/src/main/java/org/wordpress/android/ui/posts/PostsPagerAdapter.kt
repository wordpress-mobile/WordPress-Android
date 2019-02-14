package org.wordpress.android.ui.posts

import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.view.ViewGroup
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.PostListType.DRAFTS
import org.wordpress.android.ui.posts.PostListType.PUBLISHED
import org.wordpress.android.ui.posts.PostListType.SCHEDULED
import org.wordpress.android.ui.posts.PostListType.TRASHED
import java.lang.ref.WeakReference

class PostsPagerAdapter(private val site: SiteModel, val fm: FragmentManager) :
        FragmentPagerAdapter(fm) {
    companion object {
        val postTypes = listOf(PUBLISHED, DRAFTS, SCHEDULED, TRASHED)
    }

    private val listFragments = mutableMapOf<Int, WeakReference<PostListFragment>>()

    override fun getCount(): Int = postTypes.size

    override fun getItem(position: Int): PostListFragment =
            PostListFragment.newInstance(site, postTypes[position], null)

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val fragment = super.instantiateItem(container, position) as PostListFragment
        listFragments[position] = WeakReference(fragment)
        return fragment
    }

    override fun getPageTitle(position: Int): CharSequence? =
            WordPress.getContext().getString(postTypes[position].titleResId)

    fun getItemAtPosition(position: Int): PostListFragment? {
        return listFragments[position]?.get()
    }
}
