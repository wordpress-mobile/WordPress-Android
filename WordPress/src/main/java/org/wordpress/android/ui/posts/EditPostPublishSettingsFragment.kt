package org.wordpress.android.ui.posts

import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import org.wordpress.android.WordPress

class EditPostPublishSettingsFragment : PublishSettingsFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity!!.applicationContext as WordPress).component().inject(this)
        viewModel = ViewModelProviders.of(activity!!, viewModelFactory)
                .get(EditPostPublishSettingsViewModel::class.java)
    }

    companion object {
        fun newInstance() = EditPostPublishSettingsFragment()
    }
}
