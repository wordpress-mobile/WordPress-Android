package org.wordpress.android.ui.mlp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ImageView.ScaleType.FIT_CENTER
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.modal_layout_picker_layouts_card.view.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import org.wordpress.android.util.setVisible
import javax.inject.Inject

/**
 * Renders the Layout cards
 */
class LayoutsAdapter(
    private val context: Context,
    private val selectionListener: LayoutSelectionListener
) : RecyclerView.Adapter<LayoutsAdapter.ViewHolder>() {
    @Inject lateinit var imageManager: ImageManager

    private var layouts: List<LayoutListItem> = listOf()

    init {
        (context.applicationContext as WordPress).component().inject(this)
    }

    fun setData(data: List<LayoutListItem>) {
        val diffResult = DiffUtil.calculateDiff(
                LayoutsDiffCallback(
                        layouts,
                        data
                )
        )
        layouts = data
        diffResult.dispatchUpdatesTo(this)
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
        holder.selected.setVisible(layout.selected)
        holder.preview.contentDescription = if (layout.selected) context.getString(
                R.string.mlp_layout_selected,
                layout.title
        ) else layout.title
        holder.container.setOnClickListener {
            selectionListener.layoutTapped(layout)
        }
        selectionListener.selectedItemData.observe(selectionListener.lifecycleOwner, Observer {
            holder.selected.setVisible(it == layout.slug)
        })
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val container: View = itemView.layout_container
        val preview: ImageView = itemView.preview
        val selected: ImageView = itemView.selected_overlay
    }
}
