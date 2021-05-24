package org.wordpress.android.ui.gif

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView.ScaleType.CENTER_CROP
import org.wordpress.android.R
import org.wordpress.android.databinding.MediaPickerThumbnailBinding
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.AniUtils.Duration.MEDIUM
import org.wordpress.android.util.getDistinct
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.PHOTO
import org.wordpress.android.util.redirectContextClickToLongPressListener
import org.wordpress.android.viewmodel.gif.GifMediaViewModel

/**
 * Represents a single item in the [GifPickerActivity]'s grid (RecyclerView).
 *
 * This is meant to show a single animated gif.
 *
 * This ViewHolder references a readonly [GifMediaViewModel]. It should never update the [GifMediaViewModel]. That
 * behavior is handled by the [GifPickerViewModel]. This is designed this way so that [GifPickerViewModel]
 * encapsulates all the logic of managing selected items as well as keeping their selection numbers continuous.
 */
class GifMediaViewHolder(
    /**
     * The [ImageManager] to use for loading an image in to the ImageView
     */
    private val imageManager: ImageManager,
    /**
     * A function that is called when the thumbnail is clicked.
     *
     * If there is no bound [mediaViewModel], this can mean that there was an API error or this is just a placeholder.
     */
    private val onClickListener: (GifMediaViewModel?) -> Unit,
    /**
     * A function that is called when the user performs a long press on the thumbnail
     */
    private val onLongClickListener: (GifMediaViewModel) -> Unit,
    /**
     * The view used for this `ViewHolder`.
     */
    itemView: View,
    /**
     * The dimensions used for the ImageView
     */
    thumbnailViewDimensions: ThumbnailViewDimensions,
    private val isMultiSelectEnabled: Boolean
) : LifecycleOwnerViewHolder<GifMediaViewModel>(itemView) {
    data class ThumbnailViewDimensions(val width: Int, val height: Int)

    private val binding = MediaPickerThumbnailBinding.bind(itemView)

    private var mediaViewModel: GifMediaViewModel? = null

    init {
        binding.imageThumbnail.apply {
            layoutParams.width = thumbnailViewDimensions.width
            layoutParams.height = thumbnailViewDimensions.height

            setOnClickListener { onClickListener(mediaViewModel) }
            setOnLongClickListener {
                val mediaViewModel = mediaViewModel ?: return@setOnLongClickListener false
                onLongClickListener(mediaViewModel)
                true
            }
            redirectContextClickToLongPressListener()
        }
    }

    /**
     * Update the views to use the given [GifMediaViewModel]
     *
     * The [mediaViewModel] is optional because we enable placeholders in the paged list created by
     * [org.wordpress.android.viewmodel.gif.GifPickerViewModel]. This causes null values to be bound to
     * [GifMediaViewHolder] instances.
     */
    override fun bind(item: GifMediaViewModel?) {
        super.bind(item)

        this.mediaViewModel = item

        // Immediately update the selection number and scale the thumbnail when a bind happens
        val isSelected = mediaViewModel?.isSelected?.value ?: false
        updateSelectionIndicatorOnSelectionChange(isSelected = isSelected, animated = false)
        updateThumbnailOnSelectionChange(isSelected = isSelected, animated = false)

        // When the [isSelected] property changes later, update the selection number and scale the thumbnail
        mediaViewModel?.isSelected?.observe(this, {
            val selected = it ?: false

            updateSelectionIndicatorOnSelectionChange(isSelected = selected, animated = true)
            updateThumbnailOnSelectionChange(isSelected = selected, animated = true)
        })

        // Update selection number text and observe later changes
        binding.textSelectionCount.text = mediaViewModel?.selectionNumber?.value?.toString() ?: ""
        mediaViewModel?.selectionNumber?.getDistinct()?.observe(this, {
            binding.textSelectionCount.text = it?.toString() ?: ""
        })

        binding.imageThumbnail.contentDescription = mediaViewModel?.title
        imageManager.load(binding.imageThumbnail, PHOTO, mediaViewModel?.thumbnailUri.toString(), CENTER_CROP)
    }

    private fun updateSelectionIndicatorOnSelectionChange(isSelected: Boolean, animated: Boolean) {
        // The `isSelected` here changes the color of the text. It will be blue when selected.
        binding.textSelectionCount.isSelected = isSelected
        if (!isMultiSelectEnabled) {
            binding.textSelectionCount.visibility = if (isSelected) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        if (animated) {
            if (!isMultiSelectEnabled) {
                if (isSelected) {
                    AniUtils.scaleIn(binding.textSelectionCount, MEDIUM)
                } else {
                    AniUtils.scaleOut(binding.textSelectionCount, MEDIUM)
                }
            } else {
                AniUtils.startAnimation(binding.textSelectionCount, R.anim.pop)
            }
        }
    }

    /**
     * Scale the thumbnail depending on the value of [isSelected]
     */
    private fun updateThumbnailOnSelectionChange(isSelected: Boolean, animated: Boolean) {
        val scaleStart = if (isSelected) THUMBNAIL_SCALE_NORMAL else THUMBNAIL_SCALE_SELECTED
        val scaleEnd = if (scaleStart == THUMBNAIL_SCALE_SELECTED) THUMBNAIL_SCALE_NORMAL else THUMBNAIL_SCALE_SELECTED

        with(binding.imageThumbnail) {
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
         * Create the layout and a new instance of [GifMediaViewHolder]
         */
        fun create(
            imageManager: ImageManager,
            onClickListener: (GifMediaViewModel?) -> Unit,
            onLongClickListener: (GifMediaViewModel) -> Unit,
            parent: ViewGroup,
            thumbnailViewDimensions: ThumbnailViewDimensions,
            isMultiSelectEnabled: Boolean
        ): GifMediaViewHolder {
            // We are intentionally reusing this layout since the UI is very similar.
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.media_picker_thumbnail, parent, false)
            return GifMediaViewHolder(
                    imageManager = imageManager,
                    onClickListener = onClickListener,
                    onLongClickListener = onLongClickListener,
                    itemView = view,
                    thumbnailViewDimensions = thumbnailViewDimensions,
                    isMultiSelectEnabled = isMultiSelectEnabled
            )
        }
    }
}
