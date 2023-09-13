package org.wordpress.android.ui.mysite.cards.personalize

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import org.wordpress.android.databinding.PersonalizeCardBinding
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.cards.dashboard.CardViewHolder
import org.wordpress.android.util.extensions.viewBinding

class PersonalizeCardViewHolder(parent: ViewGroup) :
    CardViewHolder<PersonalizeCardBinding>(parent.viewBinding(PersonalizeCardBinding::inflate)) {
    fun bind(cardModel: MySiteCardAndItem.Card.PersonalizeCardModel) = with(binding) {
        personalizeCard.setContent {
            AppTheme {
                PersonalizeCard(
                    model = cardModel, modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                )
            }
        }
    }
}
