package org.wordpress.android.ui.mlp

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.layout_picker_preview_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.layoutpicker.LayoutPreviewFragment
import org.wordpress.android.viewmodel.mlp.ModalLayoutPickerViewModel
import javax.inject.Inject

/**
 * Implements the Modal Layout Picker Preview UI
 */
class BlockLayoutPreviewFragment : LayoutPreviewFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: ModalLayoutPickerViewModel

    companion object {
        const val BLOCK_LAYOUT_PREVIEW_TAG = "BLOCK_LAYOUT_PREVIEW_TAG"

        fun newInstance() = BlockLayoutPreviewFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chooseButton.setText(R.string.mlp_create_page)

        viewModel = ViewModelProvider(requireActivity(), viewModelFactory).get(ModalLayoutPickerViewModel::class.java)
        setViewModel(viewModel)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }
}
