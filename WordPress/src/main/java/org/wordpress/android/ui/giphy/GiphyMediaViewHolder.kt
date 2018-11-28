package org.wordpress.android.ui.giphy

import android.arch.lifecycle.Observer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ImageView.ScaleType.CENTER_CROP
import kotlinx.android.synthetic.main.media_picker_thumbnail.view.*
import org.wordpress.android.R.layout
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.PHOTO
import org.wordpress.android.viewmodel.giphy.GiphyMediaViewModel

/**
 * Represents a single item in the [GiphyPickerActivity]'s grid (RecyclerView).
 *
 * This is meant to show a single animated gif.
 *
 * This ViewHolder references a readonly [GiphyMediaViewModel]. It should never update the [GiphyMediaViewModel]. That
 * behavior is handled by the [GiphyPickerViewModel]. This is designed this way so that [GiphyPickerViewModel]
 * encapsulates all the logic of managing selected items as well as keeping their selection numbers continuous.
 */
class GiphyMediaViewHolder(
    /**
     * The [ImageManager] to use for loading an image in to the ImageView
     */
    private val imageManager: ImageManager,
    /**
     * A function that is called when the thumbnail is clicked.
     */
    private val onClickListener: (GiphyMediaViewModel) -> Unit,
    itemView: View,
    /**
     * The dimensions used for the ImageView
     */
    thumbnailViewDimensions: ThumbnailViewDimensions
) : LifecycleOwnerViewHolder<GiphyMediaViewModel>(itemView) {

    data class ThumbnailViewDimensions(val width: Int, val height: Int)

    private val thumbnailView: ImageView = itemView.image_thumbnail

    private var mediaViewModel: GiphyMediaViewModel? = null

    init {
        thumbnailView.apply {
            layoutParams.width = thumbnailViewDimensions.width
            layoutParams.height = thumbnailViewDimensions.height

            setOnClickListener { mediaViewModel?.let(onClickListener) }
        }
    }

    /**
     * Update the views to use the given [GiphyMediaViewModel]
     *
     * The [mediaViewModel] is optional because we enable placeholders in the paged list created by
     * [GiphyPickerViewModel]. This causes null values to be bound to [GiphyMediaViewHolder] instances.
     */
    override fun bind(item: GiphyMediaViewModel?) {
        super.bind(item)

        this.mediaViewModel = item

        // Immediately update the selection number and scale the thumbnail when a bind happens
        val isSelected = mediaViewModel?.isSelected?.value ?: false
        updateThumbnailOnSelectionChange(isSelected = isSelected, animated = false)

        // When the [isSelected] property changes later, update the selection number and scale the thumbnail
        mediaViewModel?.isSelected?.observe(this, Observer {
            val selected = it ?: false

            updateThumbnailOnSelectionChange(isSelected = selected, animated = true)
        })

        thumbnailView.contentDescription = mediaViewModel?.title
        imageManager.load(thumbnailView, PHOTO, mediaViewModel?.thumbnailUri.toString(), CENTER_CROP)
    }

    /**
     * Scale the thumbnail depending on the value of [isSelected]
     */
    private fun updateThumbnailOnSelectionChange(isSelected: Boolean, animated: Boolean) {
        val scaleStart = if (isSelected) THUMBNAIL_SCALE_NORMAL else THUMBNAIL_SCALE_SELECTED
        val scaleEnd = if (scaleStart == THUMBNAIL_SCALE_SELECTED) THUMBNAIL_SCALE_NORMAL else THUMBNAIL_SCALE_SELECTED

        with(thumbnailView) {
            if (animated) {
                AniUtils.scale(this, scaleStart, scaleEnd, AniUtils.Duration.SHORT)
            } else {
                scaleX = scaleEnd
                scaleY = scaleEnd
            }
        }
    }

    companion object {
        private const val THUMBNAIL_SCALE_NORMAL: Float = 1.0f
        private const val THUMBNAIL_SCALE_SELECTED: Float = 0.8f

        /**
         * Create the layout and a new instance of [GiphyMediaViewHolder]
         */
        fun create(
            imageManager: ImageManager,
            onClickListener: (GiphyMediaViewModel) -> Unit,
            parent: ViewGroup,
            thumbnailViewDimensions: ThumbnailViewDimensions
        ): GiphyMediaViewHolder {
            // We are intentionally reusing this layout since the UI is very similar.
            val view = LayoutInflater.from(parent.context)
                    .inflate(layout.media_picker_thumbnail, parent, false)
            return GiphyMediaViewHolder(
                    imageManager = imageManager,
                    onClickListener = onClickListener,
                    itemView = view,
                    thumbnailViewDimensions = thumbnailViewDimensions
            )
        }
    }
}

