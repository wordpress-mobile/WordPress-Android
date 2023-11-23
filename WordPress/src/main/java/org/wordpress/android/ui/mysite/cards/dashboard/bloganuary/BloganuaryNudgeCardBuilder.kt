package org.wordpress.android.ui.mysite.cards.dashboard.bloganuary

import org.wordpress.android.R
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.BloganuaryNudgeCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BloganuaryNudgeCardBuilderParams
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import javax.inject.Inject

class BloganuaryNudgeCardBuilder @Inject constructor() {
    fun build(params: BloganuaryNudgeCardBuilderParams): BloganuaryNudgeCardModel? {
        return if (params.isEligible) {
            BloganuaryNudgeCardModel(
                title = UiStringRes(R.string.bloganuary_dashboard_nudge_title),
                text = UiStringRes(R.string.bloganuary_dashboard_nudge_text),
                onLearnMoreClick = ListItemInteraction.create(params.onLearnMoreClick),
                onHideMenuItemClick = ListItemInteraction.create(params.onHideMenuItemClick),
            )
        } else {
            null
        }
    }
}
