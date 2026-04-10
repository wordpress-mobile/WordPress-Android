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
        val params = singleActionCardImage.layoutParams
            as ConstraintLayout.LayoutParams
        if (singleActionCard.centerImageVertically) {
            params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            params.topMargin = 0
            params.bottomMargin = 0
        } else {
            params.bottomToBottom =
                ConstraintLayout.LayoutParams.UNSET
        }
        singleActionCardImage.layoutParams = params
    }
}
