package org.wordpress.android.ui.mysite

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.DynamicCardType
import org.wordpress.android.fluxc.model.DynamicCardType.CUSTOMIZE_QUICK_START
import org.wordpress.android.fluxc.model.DynamicCardType.GROW_QUICK_START
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.PUBLISH_POST
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
                CUSTOMIZE_QUICK_START,
                UiStringRes(R.string.set_your_blogging_goals_title),
                getBloggingReminderTaskCards(onBloggingReminderCardClick),
                R.color.orange_40,
                50,
                ListItemInteraction.create(DynamicCardMenuModel(GROW_QUICK_START, true), onQuickStartCardMoreClick)
        )
    }

    private fun getBloggingReminderTaskCards(onBloggingReminderCardClick: () -> Unit): List<BloggingReminderTaskCard> {
        return listOf(
                BloggingReminderTaskCard(
                        quickStartTask = PUBLISH_POST,
                        title = UiStringRes(R.string.set_your_blogging_goals_title),
                        description = UiStringRes(R.string.set_your_blogging_goals_body),
                        illustration = android.R.drawable.ic_menu_my_calendar, // TODO: Replace with calendar vector
                        accentColor = R.color.colorAccent,
                        done = false,
                        onClick = ListItemInteraction.create {
                            onBloggingReminderCardClick
                        }
                )
        )
    }
}
