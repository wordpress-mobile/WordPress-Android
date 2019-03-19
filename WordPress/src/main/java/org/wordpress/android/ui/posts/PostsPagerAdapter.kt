package org.wordpress.android.ui.posts

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.view.ViewGroup
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import java.lang.ref.WeakReference

class PostsPagerAdapter(
    private val pages: List<PostListType>,
    private val site: SiteModel,
    val fm: FragmentManager
) : FragmentStatePagerAdapter(fm) {
    private val listFragments = mutableMapOf<Int, WeakReference<PostListFragment>>()

    var onlyUser: Boolean = false
        set(value) {
            val didChange = field != value
            field = value

            if (didChange) {
                notifyDataSetChanged()
            }
        }

    override fun getCount(): Int = pages.size

    override fun getItemPosition(item: Any): Int {
        if (item is Fragment) {
            listFragments.forEach { set ->
                if (set.value.get() == item) {
                    return set.key
                }
            }

            return POSITION_NONE
        } else {
            return POSITION_UNCHANGED
        }
    }

    override fun getItem(position: Int): PostListFragment =
            PostListFragment.newInstance(site, onlyUser, pages[position])

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

    override fun notifyDataSetChanged() {
        listFragments.clear()

        super.notifyDataSetChanged()
    }
}
