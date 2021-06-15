package org.wordpress.android.ui.sitecreation.theme

import android.content.Context
import android.os.Bundle
import android.view.View
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
    private lateinit var viewModel: HomePagePickerViewModel

    companion object {
        const val DESIGN_PREVIEW_TAG = "DESIGN_PREVIEW_TAG"

        fun newInstance() = DesignPreviewFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity(), viewModelFactory).get(HomePagePickerViewModel::class.java)
        setViewModel(viewModel)
    }

    override fun getChooseButtonText() = R.string.hpp_choose_button

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }
}
