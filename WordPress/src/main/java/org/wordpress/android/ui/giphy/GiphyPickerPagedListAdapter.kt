package org.wordpress.android.ui.giphy

import android.arch.paging.PagedListAdapter
import android.support.v7.util.DiffUtil.ItemCallback
import android.view.ViewGroup
import org.wordpress.android.ui.giphy.GiphyMediaViewHolder.ThumbnailViewDimensions
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.viewmodel.giphy.GiphyMediaViewModel

/**
 * An [RecyclerView] adapter to be used with the [PagedList] created by [GiphyPickerViewModel]
 */
class GiphyPickerPagedListAdapter(
    private val imageManager: ImageManager,
    private val thumbnailViewDimensions: ThumbnailViewDimensions,
    private val onMediaViewClickListener: (GiphyMediaViewModel?) -> Unit,
    private val onMediaViewLongClickListener: (GiphyMediaViewModel) -> Unit
) : PagedListAdapter<GiphyMediaViewModel, GiphyMediaViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GiphyMediaViewHolder {
        return GiphyMediaViewHolder.create(
                imageManager = imageManager,
                onClickListener = onMediaViewClickListener,
                onLongClickListener = onMediaViewLongClickListener,
                parent = parent,
                thumbnailViewDimensions = thumbnailViewDimensions
        )
    }

    override fun onBindViewHolder(holder: GiphyMediaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF_CALLBACK = object : ItemCallback<GiphyMediaViewModel>() {
            override fun areItemsTheSame(oldItem: GiphyMediaViewModel, newItem: GiphyMediaViewModel): Boolean {
                return oldItem.id == newItem.id
            }

            /**
             * Always assume that two similar [GiphyMediaViewModel] objects always have the same content.
             *
             * It is probably extremely unlikely that GIFs from Giphy will change while the user is performing
             * a search.
             */
            override fun areContentsTheSame(oldItem: GiphyMediaViewModel, newItem: GiphyMediaViewModel): Boolean {
                return true
            }
        }
    }
}
