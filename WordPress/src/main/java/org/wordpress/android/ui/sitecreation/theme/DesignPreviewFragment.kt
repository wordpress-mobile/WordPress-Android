package org.wordpress.android.ui.sitecreation.theme

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.layoutpicker.LayoutPreviewFragment
import javax.inject.Inject

/**
 * Implements the Home Page Picker Design Preview UI
 */
class DesignPreviewFragment : LayoutPreviewFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    companion object {
        const val DESIGN_PREVIEW_TAG = "DESIGN_PREVIEW_TAG"

        fun newInstance() = DesignPreviewFragment()
    }

    override fun getViewModel() =
            ViewModelProvider(requireActivity(), viewModelFactory).get(HomePagePickerViewModel::class.java)

    override fun getChooseButtonText() = R.string.hpp_choose_button

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }
}
