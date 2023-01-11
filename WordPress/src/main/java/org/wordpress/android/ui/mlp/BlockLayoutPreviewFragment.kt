package org.wordpress.android.ui.mlp

import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.ui.layoutpicker.LayoutPreviewFragment
import org.wordpress.android.viewmodel.mlp.ModalLayoutPickerViewModel

/**
 * Implements the Modal Layout Picker Preview UI
 */
class BlockLayoutPreviewFragment : LayoutPreviewFragment() {
    companion object {
        const val BLOCK_LAYOUT_PREVIEW_TAG = "BLOCK_LAYOUT_PREVIEW_TAG"

        fun newInstance() = BlockLayoutPreviewFragment()
    }

    override fun getChooseButtonText() = R.string.mlp_create_page

    override fun getViewModel() =
        ViewModelProvider(requireActivity(), viewModelFactory).get(ModalLayoutPickerViewModel::class.java)
}
