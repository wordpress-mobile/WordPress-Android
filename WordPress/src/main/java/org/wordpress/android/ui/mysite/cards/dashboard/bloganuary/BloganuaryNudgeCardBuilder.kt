package org.wordpress.android.ui.mysite.cards.dashboard.bloganuary

import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.BloganuaryNudgeCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BloganuaryNudgeCardBuilderParams
import org.wordpress.android.ui.utils.ListItemInteraction
import javax.inject.Inject

class BloganuaryNudgeCardBuilder @Inject constructor() {
    fun build(params: BloganuaryNudgeCardBuilderParams): BloganuaryNudgeCardModel? {
        return if (params.isEligible) {
            BloganuaryNudgeCardModel(
                title = params.title,
                text = params.text,
                onLearnMoreClick = ListItemInteraction.create(params.onLearnMoreClick),
                onMoreMenuClick = ListItemInteraction.create(params.onMoreMenuClick),
                onHideMenuItemClick = ListItemInteraction.create(params.onHideMenuItemClick),
            )
        } else {
            null
        }
    }
}
