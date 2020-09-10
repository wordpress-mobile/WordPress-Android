package org.wordpress.android.ui.reader.discover.viewholders

import android.view.ViewGroup
import kotlinx.android.synthetic.main.reader_recommended_blos_card.*
import org.wordpress.android.R
import org.wordpress.android.ui.reader.discover.ReaderCardUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderRecommendedBlogsCardUiState
import org.wordpress.android.ui.reader.discover.ReaderRecommendedBlogsAdapter
import org.wordpress.android.ui.utils.addItemDivider
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.image.ImageManager

class ReaderRecommendedBlogsCardViewHolder(
    parentView: ViewGroup,
    imageManager: ImageManager
) : ReaderViewHolder(parentView, R.layout.reader_recommended_blos_card) {
    private val recommendedBlogsAdapter = ReaderRecommendedBlogsAdapter(imageManager)

    init {
        if (recommended_blogs.adapter == null) {
            recommended_blogs.adapter = recommendedBlogsAdapter
            parentView.context.getDrawable(R.drawable.discover_list_divider)?.let {
                recommended_blogs.addItemDivider(it)
            } ?: AppLog.w(AppLog.T.READER, "Discover list divider null")
        }
    }

    override fun onBind(uiState: ReaderCardUiState) {
        uiState as ReaderRecommendedBlogsCardUiState
        recommendedBlogsAdapter.submitList(uiState.blogs)
    }
}
