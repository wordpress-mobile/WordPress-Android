package org.wordpress.android.ui.activitylog

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel
import javax.inject.Inject

class ActivityLogListFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: ActivityLogViewModel

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity?.application as WordPress).component()?.inject(this)

    }
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Use the same view model as the ActivityLogActivity
        viewModel = ViewModelProviders.of(activity!!, viewModelFactory)
                .get<ActivityLogViewModel>(ActivityLogViewModel::class.java)
        setupObservers()
    }

    private fun setupObservers() {
    }
}
