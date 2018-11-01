package org.wordpress.android.ui.sitecreation

import android.os.Bundle
import android.os.Parcelable
import android.support.annotation.LayoutRes
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.widgets.RecyclerItemDecoration

class NewSiteCreationCategoryFragment : NewSiteCreationBaseFormFragment<NewSiteCreationListener>() {
    private var linearLayoutManager: LinearLayoutManager? = null
    private val keyListState = "list_state"

    @LayoutRes
    override fun getContentLayout(): Int {
        return R.layout.new_site_creation_category_screen
    }

    override fun setupContent(rootView: ViewGroup) {
        // important for accessibility - talkback
        activity!!.setTitle(R.string.site_creation_category_title)
        initRecyclerView(rootView)
    }

    private fun initRecyclerView(rootView: ViewGroup) {
        val recyclerView = rootView.findViewById<RecyclerView>(R.id.recycler_view)
        val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        linearLayoutManager = layoutManager
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.addItemDecoration(
                RecyclerItemDecoration(
                        DisplayUtils.dpToPx(activity, 72),
                        DisplayUtils.dpToPx(activity, 1)
                )
        )
    }

    override fun onHelp() {
        if (mSiteCreationListener != null) {
            mSiteCreationListener.helpCategoryScreen()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity!!.application as WordPress).component().inject(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        linearLayoutManager?.let {
            outState.putParcelable(keyListState, it.onSaveInstanceState())
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.getParcelable<Parcelable>(keyListState)?.let {
            linearLayoutManager?.onRestoreInstanceState(it)
        }
    }

    companion object {
        val TAG = "site_creation_category_fragment_tag"
    }
}
