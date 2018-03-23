package org.wordpress.android.ui.activitylog

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel

class ActivityLogListFragment : Fragment() {
    companion object {
        val TAG = ActivityLogListFragment::class.java.name

        fun newInstance(site: SiteModel): ActivityLogListFragment {
            val fragment = ActivityLogListFragment()
            val bundle = Bundle()
            bundle.putSerializable(WordPress.SITE, site)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }
}
