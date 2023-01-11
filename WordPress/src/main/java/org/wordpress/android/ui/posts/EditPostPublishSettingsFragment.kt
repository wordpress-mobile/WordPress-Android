package org.wordpress.android.ui.posts

import android.os.Bundle
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.posts.PublishSettingsFragmentType.EDIT_POST

class EditPostPublishSettingsFragment : PublishSettingsFragment() {
    override fun getContentLayout() = R.layout.edit_post_published_settings_fragment
    override fun getPublishSettingsFragmentType() = EDIT_POST
    override fun setupContent(rootView: ViewGroup, viewModel: PublishSettingsViewModel) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().applicationContext as WordPress).component().inject(this)
        viewModel = ViewModelProvider(requireActivity(), viewModelFactory)
            .get(EditPostPublishSettingsViewModel::class.java)
    }

    companion object {
        fun newInstance() = EditPostPublishSettingsFragment()
    }
}
