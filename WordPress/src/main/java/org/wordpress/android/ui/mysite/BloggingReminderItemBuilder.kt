package org.wordpress.android.ui.mysite

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.DynamicCardType
import org.wordpress.android.fluxc.model.DynamicCardType.GROW_QUICK_START
import org.wordpress.android.ui.mysite.MySiteItem.DynamicCard.BloggingReminderCard
import org.wordpress.android.ui.mysite.MySiteItem.DynamicCard.BloggingReminderCard.BloggingReminderTaskCard
import org.wordpress.android.ui.mysite.QuickStartRepository.QuickStartCategory
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuFragment.DynamicCardMenuModel
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import javax.inject.Inject

class BloggingReminderItemBuilder @Inject constructor() {
    fun build(
        quickStartCategory: QuickStartCategory,
        pinnedDynamicCardType: DynamicCardType?,
        onQuickStartCardMoreClick: (DynamicCardMenuModel) -> Unit,
        onBloggingReminderCardClick: () -> Unit
    ): BloggingReminderCard {
        return BloggingReminderCard(
                GROW_QUICK_START,
                UiStringRes(R.string.blog_not_found),
                getBloggingReminderTaskCards(),
                R.color.orange_40,
                0,
                ListItemInteraction.create(DynamicCardMenuModel(GROW_QUICK_START, true), onQuickStartCardMoreClick)
        )
    }

    fun getBloggingReminderTaskCards(): List<BloggingReminderTaskCard> {
        return emptyList()
    }
}
