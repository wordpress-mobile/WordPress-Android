package org.wordpress.android.ui.mysite.cards.dashboard.bloganuary

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import org.wordpress.android.databinding.BloganuaryNudgeCardBinding
import org.wordpress.android.ui.compose.theme.AppThemeM2WithoutBackground
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.BloganuaryNudgeCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.util.extensions.viewBinding

class BloganuaryNudgeCardViewHolder(parent: ViewGroup) :
    MySiteCardAndItemViewHolder<BloganuaryNudgeCardBinding>(parent.viewBinding(BloganuaryNudgeCardBinding::inflate)) {
    fun bind(cardModel: BloganuaryNudgeCardModel) = with(binding.bloganuaryNudgeCard) {
        // Dispose of the Composition when the view's LifecycleOwner is destroyed
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
        setContent {
            AppThemeM2WithoutBackground {
                BloganuaryNudgeCard(
                    model = cardModel,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
