package org.wordpress.android.ui.giphy

import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ImageView.ScaleType.CENTER_CROP
import kotlinx.android.synthetic.main.media_picker_thumbnail.view.*
import org.wordpress.android.R.layout
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
) : ViewHolder(itemView) {

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
    fun bind(mediaViewModel: GiphyMediaViewModel?) {
        this.mediaViewModel = mediaViewModel

        thumbnailView.contentDescription = mediaViewModel?.title
        imageManager.load(thumbnailView, PHOTO, mediaViewModel?.thumbnailUri.toString(), CENTER_CROP)
    }

    companion object {
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

