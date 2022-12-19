package org.wordpress.android.ui.sitecreation.theme

import org.wordpress.android.R.dimen
import org.wordpress.android.ui.layoutpicker.ThumbDimensionProvider
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

class SiteDesignRecommendedDimensionProvider @Inject constructor(private val contextProvider: ContextProvider) :
        ThumbDimensionProvider {
    override val previewWidth: Int
        get() = contextProvider.getContext().resources.getDimensionPixelSize(dimen.hpp_recommended_card_width)

    override val previewHeight: Int
        get() = contextProvider.getContext().resources.getDimensionPixelSize(dimen.hpp_recommended_card_height)

    override val rowHeight: Int
        get() = contextProvider.getContext().resources.getDimensionPixelSize(dimen.hpp_recommended_row_height)

    // For larger images, we must provide a scale of 2.0 to the API due to a limitation in MShots and our endpoint
    // integration (currently, MShots only supports a scale value of 1 or 2).
    override val scale: Double
        get() = if (1f < contextProvider.getContext().resources.displayMetrics.density) 2.0 else 1.0
}
