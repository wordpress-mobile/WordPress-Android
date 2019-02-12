package org.wordpress.android.ui.sitecreation.segments

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.sitecreation.segments.SegmentsItemUiState.HeaderUiState
import org.wordpress.android.ui.sitecreation.segments.SegmentsItemUiState.SegmentUiState
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageManager.RequestListener
import org.wordpress.android.util.image.ImageType.ICON

sealed class NewSiteCreationSegmentViewHolder(internal val parent: ViewGroup, @LayoutRes layout: Int) :
        RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    abstract fun onBind(uiState: SegmentsItemUiState)

    class SegmentsItemViewHolder(
        parentView: ViewGroup,
        private val imageManager: ImageManager
    ) : NewSiteCreationSegmentViewHolder(parentView, R.layout.new_site_creation_segment_item) {
        private val container = itemView.findViewById<ViewGroup>(R.id.container)
        private val icon = itemView.findViewById<ImageView>(R.id.icon)
        private val title = itemView.findViewById<TextView>(R.id.title)
        private val subtitle = itemView.findViewById<TextView>(R.id.subtitle)
        private val divider = itemView.findViewById<View>(R.id.divider)

        override fun onBind(uiState: SegmentsItemUiState) {
            uiState as SegmentUiState
            title.text = uiState.title
            subtitle.text = uiState.subtitle
            imageManager.loadWithResultListener(
                    icon,
                    ICON,
                    uiState.iconUrl,
                    null,
                    object : RequestListener<Drawable> {
                        override fun onLoadFailed(e: Exception?) {
                        }

                        override fun onResourceReady(resource: Drawable) {
                            try {
                                icon.setColorFilter(Color.parseColor(uiState.iconColor), PorterDuff.Mode.SRC_IN)
                            } catch (e: IllegalArgumentException) {
                                AppLog.e(
                                        AppLog.T.SITE_CREATION,
                                        "Error parsing segment icon color: ${uiState.iconColor}"
                                )
                            }
                        }
                    }
            )
            requireNotNull(uiState.onItemTapped) { "OnItemTapped is required." }
            container.setOnClickListener {
                uiState.onItemTapped!!.invoke()
            }
            divider.visibility = if (uiState.showDivider) View.VISIBLE else View.GONE
        }
    }

    class SegmentsHeaderViewHolder(
        parentView: ViewGroup
    ) : NewSiteCreationSegmentViewHolder(parentView, R.layout.new_site_creation_header_item) {
        private val title = itemView.findViewById<TextView>(R.id.title)
        private val subtitle = itemView.findViewById<TextView>(R.id.subtitle)

        override fun onBind(uiState: SegmentsItemUiState) {
            uiState as HeaderUiState
            title.text = parent.context.getText(uiState.titleResId)
            subtitle.text = parent.context.getText(uiState.subtitleResId)
        }
    }

    class SegmentsProgressViewHolder(
        parentView: ViewGroup
    ) : NewSiteCreationSegmentViewHolder(parentView, R.layout.new_site_creation_segments_progress) {
        override fun onBind(uiState: SegmentsItemUiState) {}
    }
}
