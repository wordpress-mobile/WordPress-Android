package org.wordpress.android.ui.sitecreation

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.v4.app.FragmentActivity
import android.view.ViewGroup
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationSiteInfoViewModel
import javax.inject.Inject

class NewSiteCreationSiteInfoFragment : NewSiteCreationBaseFormFragment<NewSiteCreationListener>() {
    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var nonNullActivity: FragmentActivity
    private lateinit var viewModel: NewSiteCreationSiteInfoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nonNullActivity = requireNotNull(activity)
        (nonNullActivity.application as WordPress).component().inject(this)
    }

    @LayoutRes
    override fun getContentLayout(): Int {
        return R.layout.new_site_creation_site_info_screen
    }

    override fun setupContent(rootView: ViewGroup) {
        // TODO: Get the title from the main VM
        initViewModel()
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(nonNullActivity, viewModelFactory)
                .get(NewSiteCreationSiteInfoViewModel::class.java)
    }

    override fun onHelp() {
        if (mSiteCreationListener != null) {
            mSiteCreationListener.helpCategoryScreen()
        }
    }

    companion object {
        const val TAG = "site_creation_site_info_fragment_tag"
    }
}
