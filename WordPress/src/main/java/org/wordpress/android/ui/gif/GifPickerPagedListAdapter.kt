package org.wordpress.android.ui.gif

import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import org.wordpress.android.ui.gif.GifMediaViewHolder.ThumbnailViewDimensions
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.viewmodel.gif.GifMediaViewModel

/**
 * An [RecyclerView] adapter to be used with the [PagedList] created by [GifPickerViewModel]
 */
class GifPickerPagedListAdapter(
    private val imageManager: ImageManager,
    private val thumbnailViewDimensions: ThumbnailViewDimensions,
    private val onMediaViewClickListener: (GifMediaViewModel?) -> Unit,
    private val onMediaViewLongClickListener: (GifMediaViewModel) -> Unit,
    private val isMultiSelectEnabled: Boolean
) : PagedListAdapter<GifMediaViewModel, GifMediaViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GifMediaViewHolder {
        return GifMediaViewHolder.create(
                imageManager = imageManager,
                onClickListener = onMediaViewClickListener,
                onLongClickListener = onMediaViewLongClickListener,
                parent = parent,
                thumbnailViewDimensions = thumbnailViewDimensions,
                isMultiSelectEnabled = isMultiSelectEnabled
        )
    }

    override fun onBindViewHolder(holder: GifMediaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF_CALLBACK = object : ItemCallback<GifMediaViewModel>() {
            override fun areItemsTheSame(oldItem: GifMediaViewModel, newItem: GifMediaViewModel): Boolean {
                return oldItem.id == newItem.id
            }

            /**
             * Always assume that two similar [GifMediaViewModel] objects always have the same content.
             *
             * It is probably extremely unlikely that GIFs will change while the user is performing
             * a search.
             */
            override fun areContentsTheSame(oldItem: GifMediaViewModel, newItem: GifMediaViewModel): Boolean {
                return true
            }
        }
    }
}
