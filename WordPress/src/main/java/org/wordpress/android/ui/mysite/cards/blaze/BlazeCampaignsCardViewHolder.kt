package org.wordpress.android.ui.mysite.cards.blaze

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.Modifier
import org.wordpress.android.databinding.CampaignsCardBinding
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BlazeCard.BlazeCampaignsCardModel
import org.wordpress.android.ui.mysite.cards.dashboard.CardViewHolder
import org.wordpress.android.util.extensions.viewBinding

class BlazeCampaignsCardViewHolder(parent: ViewGroup) :
    CardViewHolder<CampaignsCardBinding>(parent.viewBinding(CampaignsCardBinding::inflate)) {
    fun bind(cardModel: BlazeCampaignsCardModel) = with(binding) {
        blazeCampaignsCard.setContent {
            AppTheme {
                BlazeCampaignsCard(
                    blazeCampaignCardModel = cardModel, modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                )
            }
        }
    }
}
