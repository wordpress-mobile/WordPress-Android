package org.wordpress.android.ui.posts

import android.content.Context
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.PostListType.DRAFTS
import org.wordpress.android.ui.posts.PostListType.PUBLISHED
import org.wordpress.android.ui.posts.PostListType.SCHEDULED
import org.wordpress.android.ui.posts.PostListType.TRASHED
import java.lang.ref.WeakReference

class PostsPagerAdapter(private val site: SiteModel, val context: Context, val fm: FragmentManager) :
        FragmentPagerAdapter(fm) {
    companion object {
        val postTypes = listOf(PUBLISHED, DRAFTS, SCHEDULED, TRASHED)
    }

    private val listFragments = mutableMapOf<Int, WeakReference<PostListFragment>>()

    override fun getCount(): Int = postTypes.size

    override fun getItem(position: Int): PostListFragment {
        val fragment = PostListFragment.newInstance(site, postTypes[position], null)
        listFragments[position] = WeakReference(fragment)
        return fragment
    }

    override fun getPageTitle(position: Int): CharSequence? = context.getString(postTypes[position].titleResId)

    fun getItemAtPosition(position: Int): PostListFragment? {
        return listFragments[position]?.get()
    }
}
