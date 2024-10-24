package org.wordpress.android.ui.mysite.cards.blaze

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.Modifier
import org.wordpress.android.databinding.CampaignsCardBinding
import org.wordpress.android.ui.compose.theme.AppThemeM2WithoutBackground
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.BlazeCard.BlazeCampaignsCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.util.extensions.viewBinding

class BlazeCampaignsCardViewHolder(parent: ViewGroup) :
    MySiteCardAndItemViewHolder<CampaignsCardBinding>(parent.viewBinding(CampaignsCardBinding::inflate)) {
    fun bind(cardModel: BlazeCampaignsCardModel) = with(binding) {
        blazeCampaignsCard.setContent {
            AppThemeM2WithoutBackground {
                BlazeCampaignsCard(
                    blazeCampaignCardModel = cardModel, modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                )
            }
        }
    }
}
