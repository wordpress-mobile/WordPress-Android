package org.wordpress.android.ui

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import org.wordpress.android.models.FilterCriteria
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T

class FilteredPagerAdapter(
    private val mRecyclerView: RecyclerView,
    private var mFilterCriteria: List<FilterCriteria>
) : PagerAdapter() {
    fun datasetChanged(filterCriteriaOptions: List<FilterCriteria>): Boolean {
        var isChanged = false

        if (mFilterCriteria.size != filterCriteriaOptions.size) {
            isChanged = true
        } else {
            for (index in mFilterCriteria.indices) {
                if (mFilterCriteria[index] != filterCriteriaOptions[index]) {
                    isChanged = true
                    break
                }
            }
        }

        return isChanged
    }

    fun updateFilters(filterCriteriaOptions: List<FilterCriteria>) {
            mFilterCriteria = filterCriteriaOptions
            notifyDataSetChanged()
    }

    fun getFilterAtPosition(position: Int): FilterCriteria {
        return mFilterCriteria[position]
    }

    override fun getItemPosition(`object`: Any): Int {
        mFilterCriteria?.let {
            for (i in mFilterCriteria.indices) {
                val criteria = mFilterCriteria[i]
                if (criteria != null && criteria == `object`) {
                    return i
                }
            }
        }
        return -1
    }

    override fun getCount(): Int {
        return mFilterCriteria.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return mFilterCriteria!![position].label
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        if (mRecyclerView != null && mRecyclerView.parent !== container) {
            container.addView(mRecyclerView)
        } else {
            AppLog.d(
                    T.READER,
                    "FilteredPagerAdapter - Calling instantiateItem at $position without adding the view."
            )
        }

        // Return the page
        return mRecyclerView
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }
}
