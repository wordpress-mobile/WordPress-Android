package org.wordpress.android.ui.sitecreation

import android.support.annotation.LayoutRes
import android.view.ViewGroup
import org.wordpress.android.R

class NewSiteCreationSiteInfoFragment : NewSiteCreationBaseFormFragment<NewSiteCreationListener>() {
    @LayoutRes
    override fun getContentLayout(): Int {
        return R.layout.new_site_creation_site_info_screen
    }

    override fun setupContent(rootView: ViewGroup) {
        // TODO: Get the title from the main VM
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
