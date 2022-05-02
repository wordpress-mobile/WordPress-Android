package org.wordpress.android.ui.reader.discover.viewholders

import android.view.ViewGroup
import org.wordpress.android.R
import org.wordpress.android.databinding.ReaderRecommendedBlogsCardBinding
import org.wordpress.android.ui.reader.discover.ReaderCardUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderRecommendedBlogsCardUiState
import org.wordpress.android.ui.reader.discover.ReaderRecommendedBlogsAdapter
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.addItemDivider
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.extensions.viewBinding

class ReaderRecommendedBlogsCardViewHolder(
    parentView: ViewGroup,
    imageManager: ImageManager,
    uiHelpers: UiHelpers
) : ReaderViewHolder<ReaderRecommendedBlogsCardBinding>(
        parentView.viewBinding(ReaderRecommendedBlogsCardBinding::inflate)
) {
    private val recommendedBlogsAdapter = ReaderRecommendedBlogsAdapter(imageManager, uiHelpers)

    init {
        with(binding) {
            recommendedBlogs.adapter = recommendedBlogsAdapter
            parentView.context.getDrawable(R.drawable.default_list_divider)?.let {
                recommendedBlogs.addItemDivider(it)
            } ?: AppLog.w(AppLog.T.READER, "Discover list divider null")
        }
    }

    override fun onBind(uiState: ReaderCardUiState) {
        uiState as ReaderRecommendedBlogsCardUiState
        recommendedBlogsAdapter.submitList(uiState.blogs)
    }
}
