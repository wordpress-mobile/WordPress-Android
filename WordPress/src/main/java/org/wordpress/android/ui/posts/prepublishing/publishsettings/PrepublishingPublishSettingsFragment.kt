package org.wordpress.android.ui.posts.prepublishing.publishsettings

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.posts.prepublishing.listeners.PrepublishingScreenClosedListener
import org.wordpress.android.ui.posts.PublishSettingsFragment
import org.wordpress.android.ui.posts.PublishSettingsFragmentType.PREPUBLISHING_NUDGES
import org.wordpress.android.ui.posts.PublishSettingsViewModel
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

class PrepublishingPublishSettingsFragment : PublishSettingsFragment() {
    @Inject
    lateinit var uiHelpers: UiHelpers
    private var closeListener: PrepublishingScreenClosedListener? = null

    override fun getContentLayout() = R.layout.prepublishing_published_settings_fragment
    override fun getPublishSettingsFragmentType() = PREPUBLISHING_NUDGES

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().applicationContext as WordPress).component().inject(this)
        viewModel = ViewModelProvider(requireActivity(), viewModelFactory)
            .get(PrepublishingPublishSettingsViewModel::class.java)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        closeListener = parentFragment as PrepublishingScreenClosedListener
    }

    override fun onDetach() {
        super.onDetach()
        closeListener = null
    }

    override fun setupContent(rootView: ViewGroup, viewModel: PublishSettingsViewModel) {
        val backButton = rootView.findViewById<View>(R.id.back_button)
        val toolbarTitle = rootView.findViewById<TextView>(R.id.toolbar_title)

        (viewModel as PrepublishingPublishSettingsViewModel).let {
            backButton.setOnClickListener { viewModel.onBackButtonClicked() }

            viewModel.navigateToHomeScreen.observeEvent(this, {
                closeListener?.onBackClicked()
            })

            viewModel.updateToolbarTitle.observe(this, { uiString ->
                toolbarTitle.text = uiHelpers.getTextOfUiString(
                    requireContext(),
                    uiString
                )
            })
        }
    }

    companion object {
        const val TAG = "prepublishing_publish_settings_fragment_tag"
        fun newInstance() = PrepublishingPublishSettingsFragment()
    }
}
