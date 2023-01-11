package org.wordpress.android.ui.sitecreation.theme

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.layoutpicker.LayoutPreviewFragment
import org.wordpress.android.util.config.SiteNameFeatureConfig
import javax.inject.Inject

/**
 * Implements the Home Page Picker Design Preview UI
 */
class DesignPreviewFragment : LayoutPreviewFragment() {
    @Inject
    lateinit var siteNameFeatureConfig: SiteNameFeatureConfig

    companion object {
        const val DESIGN_PREVIEW_TAG = "DESIGN_PREVIEW_TAG"

        fun newInstance() = DesignPreviewFragment()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }

    override fun getViewModel() =
        ViewModelProvider(requireActivity(), viewModelFactory).get(HomePagePickerViewModel::class.java)

    override fun getChooseButtonText() = if (siteNameFeatureConfig.isEnabled()) {
        R.string.hpp_choose_and_create_site
    } else {
        R.string.hpp_choose_button
    }
}
