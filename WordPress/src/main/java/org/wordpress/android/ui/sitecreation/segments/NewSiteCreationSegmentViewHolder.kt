package org.wordpress.android.ui.sitecreation.segments

import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.vertical.VerticalSegmentModel
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.IMAGE

sealed class NewSiteCreationSegmentViewHolder(internal val parent: ViewGroup, @LayoutRes layout: Int) :
        RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    abstract fun onBind(model: VerticalSegmentModel, isLast: Boolean)

    class SegmentViewHolder(
        parentView: ViewGroup,
        private val imageManager: ImageManager,
        private val onItemTapped: (VerticalSegmentModel) -> Unit
    ) : NewSiteCreationSegmentViewHolder(parentView, R.layout.new_site_creation_segment_item) {
        private val container = itemView.findViewById<ViewGroup>(R.id.container)
        private val icon = itemView.findViewById<ImageView>(R.id.icon)
        private val title = itemView.findViewById<TextView>(R.id.title)
        private val subtitle = itemView.findViewById<TextView>(R.id.subtitle)
        private val divider = itemView.findViewById<View>(R.id.divider)

        override fun onBind(model: VerticalSegmentModel, isLast: Boolean) {
            title.text = model.title
            subtitle.text = model.subtitle
            imageManager.load(icon, IMAGE, StringUtils.notNullStr(model.iconUrl))
            container.setOnClickListener { onItemTapped(model) }
            divider.visibility = if (isLast) View.GONE else View.VISIBLE
        }
    }
}
