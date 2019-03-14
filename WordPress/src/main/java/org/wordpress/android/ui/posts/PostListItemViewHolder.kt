package org.wordpress.android.ui.posts

import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import org.wordpress.android.viewmodel.posts.PostListItemUiModel
import org.wordpress.android.widgets.WPTextView

class PostListItemViewHolder(
    @LayoutRes layout: Int,
    private val parentView: ViewGroup,
    private val config: PostViewHolderConfig,
    private val imageManager: ImageManager,
    private val uiHelpers: UiHelpers
) : RecyclerView.ViewHolder(
        LayoutInflater.from(parentView.context).inflate(
                layout,
                parentView,
                false
        )
) {
    private val ivImageFeatured: ImageView = parentView.findViewById(R.id.image_featured)
    private val tvTitle: WPTextView = parentView.findViewById(R.id.title)
    private val tvExcerpt: WPTextView = parentView.findViewById(R.id.excerpt)
    private val tvDateAndAuthor: WPTextView = parentView.findViewById(R.id.date_and_author)
    private val tvStatusLabels: WPTextView = parentView.findViewById(R.id.status_labels)

    fun onBind(item: PostListItemUiModel) {
        item.imageUrl?.let { imageManager.load(ivImageFeatured, ImageType.IMAGE, it) }
                ?: imageManager.cancelRequestAndClearImageView(ivImageFeatured)

        tvTitle.text = uiHelpers.getTextOfUiString(parentView.context, item.title)
        tvExcerpt.text = uiHelpers.getTextOfUiString(parentView.context, item.excerpt)
        tvDateAndAuthor.text = uiHelpers.getTextOfUiString(parentView.context, item.dateAndAuthor)
        setTextOrHide(tvStatusLabels, item.statusLabels)

        parentView.setOnClickListener { item.onSelected }
    }

    private fun setTextOrHide(view: WPTextView, text: UiString?) {
        uiHelpers.updateVisibility(view, text != null)
        text?.let {
            view.text = uiHelpers.getTextOfUiString(parentView.context, it)
        }
    }
}
