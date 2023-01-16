package org.wordpress.android.ui.mlp

import org.wordpress.android.R.dimen
import org.wordpress.android.ui.layoutpicker.ThumbDimensionProvider
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

class ModalLayoutPickerDimensionProvider @Inject constructor(private val contextProvider: ContextProvider) :
    ThumbDimensionProvider {
    override val previewWidth: Int
        get() = contextProvider.getContext().resources.getDimensionPixelSize(dimen.mlp_layout_card_width)

    override val previewHeight: Int
        get() = contextProvider.getContext().resources.getDimensionPixelSize(dimen.mlp_layout_card_height)

    override val rowHeight: Int
        get() = contextProvider.getContext().resources.getDimensionPixelSize(dimen.mlp_layouts_row_height)
}
