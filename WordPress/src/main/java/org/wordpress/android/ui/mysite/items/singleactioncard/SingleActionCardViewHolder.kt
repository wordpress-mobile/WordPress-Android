package org.wordpress.android.ui.mysite.items.singleactioncard

import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import org.wordpress.android.databinding.MySiteSingleActionCardItemBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.SingleActionCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.util.extensions.viewBinding

class SingleActionCardViewHolder(
    parent: ViewGroup
) : MySiteCardAndItemViewHolder<MySiteSingleActionCardItemBinding>(
    parent.viewBinding(MySiteSingleActionCardItemBinding::inflate)
) {
    fun bind(singleActionCard: SingleActionCard) = with(binding) {
        val context = root.context
        singleActionCardText.text =
            context.getString(singleActionCard.textResource)
        singleActionCardImage.setImageDrawable(
            ContextCompat.getDrawable(context, singleActionCard.imageResource)
        )
        singleActionCardCover.setOnClickListener {
            singleActionCard.onActionClick()
        }
        learnMore.visibility = if (singleActionCard.showLearnMore) {
            View.VISIBLE
        } else {
            View.GONE
        }
        val textParams = singleActionCardText.layoutParams
            as ConstraintLayout.LayoutParams
        if (!singleActionCard.showLearnMore) {
            textParams.bottomToBottom =
                ConstraintLayout.LayoutParams.PARENT_ID
            textParams.bottomMargin = context.resources
                .getDimensionPixelSize(
                    org.wordpress.android.R.dimen.margin_extra_large
                )
        } else {
            textParams.bottomToBottom =
                ConstraintLayout.LayoutParams.UNSET
            textParams.bottomMargin = 0
        }
        singleActionCardText.layoutParams = textParams
        val marginExtraLarge = context.resources
            .getDimensionPixelSize(
                org.wordpress.android.R.dimen.margin_extra_large
            )
        val params = singleActionCardImage.layoutParams
            as ConstraintLayout.LayoutParams
        if (singleActionCard.centerImageVertically) {
            params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            params.topMargin = 0
            params.bottomMargin = 0
        } else {
            params.topToTop = ConstraintLayout.LayoutParams.UNSET
            params.bottomToBottom =
                ConstraintLayout.LayoutParams.UNSET
            params.topMargin = marginExtraLarge
            params.bottomMargin = marginExtraLarge
        }
        singleActionCardImage.layoutParams = params
    }
}
