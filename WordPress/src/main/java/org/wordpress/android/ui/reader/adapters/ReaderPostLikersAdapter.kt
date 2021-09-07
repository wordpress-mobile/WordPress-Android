package org.wordpress.android.ui.reader.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.reader.adapters.TrainOfFacesItem.BloggersLikingTextItem
import org.wordpress.android.ui.reader.adapters.TrainOfFacesItem.FaceItem
import org.wordpress.android.ui.reader.adapters.TrainOfFacesViewType.BLOGGERS_LIKING_TEXT
import org.wordpress.android.ui.reader.adapters.TrainOfFacesViewType.FACE
import org.wordpress.android.ui.reader.viewholders.BloggingLikersTextViewHolder
import org.wordpress.android.ui.reader.viewholders.PostLikerViewHolder
import org.wordpress.android.ui.reader.viewholders.TrainOfFacesViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager

class ReaderPostLikersAdapter(
    private val imageManager: ImageManager,
    private val uiHelpers: UiHelpers
) : Adapter<TrainOfFacesViewHolder<*>>() {
    private var itemsList = listOf<TrainOfFacesItem>()

    fun loadData(items: List<TrainOfFacesItem>) {
        val diffResult = DiffUtil.calculateDiff(
                ReaderPostLikersAdapterDiffCallback(itemsList, items)
        )
        itemsList = items
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrainOfFacesViewHolder<*> {
        return when (viewType) {
            FACE.ordinal -> PostLikerViewHolder(parent, imageManager)
            BLOGGERS_LIKING_TEXT.ordinal -> BloggingLikersTextViewHolder(parent, uiHelpers)
            else -> throw IllegalArgumentException("Illegal view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: TrainOfFacesViewHolder<*>, position: Int) {
        when (val item = itemsList[position]) {
            is FaceItem -> (holder as PostLikerViewHolder).bind(item)
            is BloggersLikingTextItem -> (holder as BloggingLikersTextViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int {
        return itemsList.size
    }

    override fun getItemViewType(position: Int): Int {
        return itemsList[position].type.ordinal
    }
}
