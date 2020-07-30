package org.wordpress.android.ui.mlp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ImageView.ScaleType.FIT_CENTER
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.modal_layout_picker_layouts_card.view.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import javax.inject.Inject

/**
 * Renders the Layout cards
 */
class LayoutsAdapter(
    private val context: Context,
    private val layouts: List<LayoutListItem>
) : RecyclerView.Adapter<LayoutsAdapter.ViewHolder>() {
    @Inject lateinit var imageManager: ImageManager

    init {
        (context.applicationContext as WordPress).component().inject(this)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.modal_layout_picker_layouts_card, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int {
        return layouts.size
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val layout = layouts[position]
        imageManager.load(holder.preview, ImageType.THEME, layout.preview, FIT_CENTER)
        holder.preview.contentDescription = layout.title
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val preview: ImageView = itemView.preview
    }
}
