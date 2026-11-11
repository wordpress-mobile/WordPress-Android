package org.wordpress.android.ui.mysite.items.singleactioncard

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import org.wordpress.android.R
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
        singleActionCardText.text = context.getString(singleActionCard.textResource)
        singleActionCardImage.setImageDrawable(
            ContextCompat.getDrawable(context, singleActionCard.imageResource)
        )
        singleActionCardCover.setOnClickListener { singleActionCard.onActionClick() }
        learnMore.visibility = if (singleActionCard.showLearnMore) View.VISIBLE else View.GONE
        bindText(singleActionCard)
        bindImage(singleActionCard)
    }

    /**
     * Every branch here is written both ways round: view holders are recycled, so a card bound with
     * one presentation must not leave the next card wearing it.
     */
    private fun MySiteSingleActionCardItemBinding.bindText(singleActionCard: SingleActionCard) {
        val marginExtraLarge = root.context.resources.getDimensionPixelSize(R.dimen.margin_extra_large)
        val params = singleActionCardText.layoutParams as ConstraintLayout.LayoutParams
        if (singleActionCard.showLearnMore) {
            params.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
            params.bottomMargin = 0
        } else {
            params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            params.bottomMargin = marginExtraLarge
        }
        // Centred cards span the whole card rather than the column beside the icon, which
        // bindImage hides.
        if (singleActionCard.centerText) {
            singleActionCardText.gravity = Gravity.CENTER_HORIZONTAL
            params.startToEnd = ConstraintLayout.LayoutParams.UNSET
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
        } else {
            singleActionCardText.gravity = Gravity.START
            params.startToEnd = singleActionCardImage.id
            params.startToStart = ConstraintLayout.LayoutParams.UNSET
        }
        singleActionCardText.layoutParams = params
    }

    private fun MySiteSingleActionCardItemBinding.bindImage(singleActionCard: SingleActionCard) {
        singleActionCardImage.visibility = if (singleActionCard.centerText) View.GONE else View.VISIBLE
        val marginExtraLarge = root.context.resources.getDimensionPixelSize(R.dimen.margin_extra_large)
        val params = singleActionCardImage.layoutParams as ConstraintLayout.LayoutParams
        if (singleActionCard.centerImageVertically) {
            params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            params.topMargin = 0
            params.bottomMargin = 0
        } else {
            params.topToTop = ConstraintLayout.LayoutParams.UNSET
            params.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
            params.topMargin = marginExtraLarge
            params.bottomMargin = marginExtraLarge
        }
        singleActionCardImage.layoutParams = params
    }
}
