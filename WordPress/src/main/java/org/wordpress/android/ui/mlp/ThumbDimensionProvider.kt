package org.wordpress.android.ui.mlp

import org.wordpress.android.R
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

class ThumbDimensionProvider @Inject constructor(private val contextProvider: ContextProvider) {
    val previewWidth: Int
        get() = contextProvider.getContext().resources.getDimensionPixelSize(R.dimen.mlp_layout_card_width)

    val scale: Double = 1.0 // Passing 1.0 and the rendered pixels per device in previewWidth
}
