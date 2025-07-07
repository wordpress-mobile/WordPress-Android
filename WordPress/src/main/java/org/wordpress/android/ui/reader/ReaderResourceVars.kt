package org.wordpress.android.ui.reader

import android.content.Context
import org.wordpress.android.R
import org.wordpress.android.util.DisplayUtils

/*
* class which holds resource-based variables used when rendering post detail
*/
internal class ReaderResourceVars(context: Context) {
    val marginMediumPx: Int

    val isWideDisplay: Boolean
    val fullSizeImageWidthPx: Int

    val videoWidthPx: Int
    val videoHeightPx: Int

    init {
        val resources = context.resources
        val displayWidthPx = DisplayUtils.getWindowPixelWidth(context)
        val marginLargePx = resources.getDimensionPixelSize(R.dimen.margin_large)
        val detailMarginWidthPx = resources.getDimensionPixelOffset(R.dimen.reader_detail_margin)

        isWideDisplay = DisplayUtils.pxToDp(context, displayWidthPx) >= MIN_WIDE_DISPLAY_WIDTH_DP
        marginMediumPx = resources.getDimensionPixelSize(R.dimen.margin_medium)

        // full-size image width must take margin into account
        fullSizeImageWidthPx = displayWidthPx - (detailMarginWidthPx * 2)

        // 16:9 ratio (YouTube standard)
        videoWidthPx = fullSizeImageWidthPx - (marginLargePx * 2)
        videoHeightPx = (videoWidthPx * RATIO_16_9).toInt()
    }

    companion object {
        private const val MIN_WIDE_DISPLAY_WIDTH_DP = 641
        private const val RATIO_16_9 = 0.5625f
    }
}
