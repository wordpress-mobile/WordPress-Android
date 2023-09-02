package org.wordpress.android.ui.mysite.cards.dashboard.domaintransfer

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool
import org.wordpress.android.databinding.DomainTransferCardBinding
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.DomainTransferCardModel
import org.wordpress.android.ui.mysite.cards.dashboard.CardViewHolder
import org.wordpress.android.util.extensions.viewBinding

class DomainTransferCardViewHolder(parent: ViewGroup) :
    CardViewHolder<DomainTransferCardBinding>(parent.viewBinding(DomainTransferCardBinding::inflate)) {
    fun bind(cardModel: DomainTransferCardModel) = with(binding) {
        // Dispose of the Composition when the view's LifecycleOwner is destroyed
        this.domainTransferCard.apply {
            setViewCompositionStrategy(DisposeOnDetachedFromWindowOrReleasedFromPool)
            setContent {
                AppTheme {
                    DomainTransferCard(
                        domainTransferCardModel = cardModel,
                        modifier = Modifier.fillMaxWidth().wrapContentHeight()
                    )
                }
            }
        }
    }
}
