package org.wordpress.android.ui.posts.prepublishing

import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.posts.PublishSettingsFragment

class PrepublishingPublishSettingsFragment : PublishSettingsFragment() {
    override fun getContentLayout() = R.layout.prepublishing_published_settings_fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().applicationContext as WordPress).component().inject(this)
        viewModel = ViewModelProviders.of(requireActivity(), viewModelFactory)
                .get(PrepublishingPublishSettingsViewModel::class.java)
    }

    companion object {
        const val TAG = "prepublishing_publish_settings_fragment_tag"
        fun newInstance() = PrepublishingPublishSettingsFragment()
    }
}
