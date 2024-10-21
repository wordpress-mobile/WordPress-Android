package org.wordpress.android.ui.mysite.cards.sotw2023

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import org.wordpress.android.databinding.WpSotw20223NudgeCardBinding
import org.wordpress.android.ui.compose.theme.AppThemeM2
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.WpSotw2023NudgeCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.util.extensions.viewBinding

class WpSotw2023NudgeCardViewHolder(parent: ViewGroup) :
    MySiteCardAndItemViewHolder<WpSotw20223NudgeCardBinding>(parent.viewBinding(WpSotw20223NudgeCardBinding::inflate)) {
    fun bind(cardModel: WpSotw2023NudgeCardModel) = with(binding.wpSotw2023NudgeCard) {
        // Dispose of the Composition when the view's LifecycleOwner is destroyed
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
        setContent {
            AppThemeM2 {
                WpSotw2023NudgeCard(
                    model = cardModel,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
