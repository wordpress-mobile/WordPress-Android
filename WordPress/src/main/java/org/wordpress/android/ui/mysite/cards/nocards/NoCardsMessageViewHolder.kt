package org.wordpress.android.ui.mysite.cards.nocards

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.Modifier
import org.wordpress.android.databinding.NoCardsMessageCardBinding
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.cards.dashboard.CardViewHolder
import org.wordpress.android.util.extensions.viewBinding

class NoCardsMessageViewHolder(parent: ViewGroup) :
    CardViewHolder<NoCardsMessageCardBinding>(parent.viewBinding(NoCardsMessageCardBinding::inflate)) {
    fun bind(cardModel: MySiteCardAndItem.Card.NoCardsMessage) = with(binding) {
        noCardsMessage.setContent {
            AppTheme {
                NoCardsMessage(
                    model = cardModel, modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                )
            }
        }
    }
}
