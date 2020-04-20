package org.wordpress.android.ui.posts.prepublishing

import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import org.wordpress.android.WordPress
import org.wordpress.android.ui.posts.PublishSettingsFragment

class PrepublishingPublishSettingsFragment: PublishSettingsFragment()  {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().applicationContext as WordPress).component().inject(this)
        viewModel = ViewModelProviders.of(requireActivity(), viewModelFactory)
                .get(PrepublishingPublishSettingsViewModel::class.java)
    }

    companion object {
        fun newInstance() = PrepublishingPublishSettingsFragment()
    }
}
