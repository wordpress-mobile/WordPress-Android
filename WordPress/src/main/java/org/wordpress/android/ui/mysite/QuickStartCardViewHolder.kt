package org.wordpress.android.ui.mysite

import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.view.ViewGroup
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat.createBlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat.SRC_IN
import kotlinx.android.synthetic.main.quick_start_card.view.*
import org.wordpress.android.R
import org.wordpress.android.ui.mysite.MySiteItem.QuickStartCard

class QuickStartCardViewHolder(
    parent: ViewGroup
) : MySiteItemViewHolder(parent, R.layout.quick_start_card) {
    init {
        itemView.apply {
            quick_start_card_more_button.let { TooltipCompat.setTooltipText(it, it.contentDescription) }
        }
    }

    fun bind(item: QuickStartCard) = itemView.apply {
        ObjectAnimator.ofInt(quick_start_card_progress, "progress", item.progress).setDuration(600).start()

        val progressTrackColor = ContextCompat.getColor(context, item.progressColor.trackColor)
        val progressIndicatorColor = ContextCompat.getColor(context, item.progressColor.indicatorColor)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            quick_start_card_progress.progressBackgroundTintList = ColorStateList.valueOf(progressTrackColor)
            quick_start_card_progress.progressTintList = ColorStateList.valueOf(progressIndicatorColor)
        } else {
            // Workaround for Lollipop
            val progressDrawable = quick_start_card_progress.progressDrawable.mutate() as LayerDrawable
            val backgroundLayer = progressDrawable.findDrawableByLayerId(android.R.id.background)
            val progressLayer = progressDrawable.findDrawableByLayerId(android.R.id.progress)
            backgroundLayer.colorFilter = createBlendModeColorFilterCompat(progressTrackColor, SRC_IN)
            progressLayer.colorFilter = createBlendModeColorFilterCompat(progressIndicatorColor, SRC_IN)
            quick_start_card_progress.progressDrawable = progressDrawable
        }

        quick_start_card_title.text = item.title
        quick_start_card_more_button.setOnClickListener { item.onMoreClick?.click() }
    }
}
