package org.wordpress.android.ui.mysite.cards.sotw2023

import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.WpSotw2023NudgeCardModel
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.util.config.WpSotw2023NudgeFeatureConfig
import javax.inject.Inject

class WpSotw2023NudgeCardViewModelSlice @Inject constructor(
    private val featureConfig: WpSotw2023NudgeFeatureConfig,
) {
    fun buildCard() : WpSotw2023NudgeCardModel? = WpSotw2023NudgeCardModel(
        onMoreMenuClick = ListItemInteraction.create(::onMoreMenuClick),
        onHideMenuItemClick = ListItemInteraction.create(::onHideMenuItemClick),
        onCtaClick = ListItemInteraction.create(::onCtaClick),
    ).takeIf { featureConfig.isEnabled() }

    private fun onMoreMenuClick() {
        // TODO analytics
    }

    private fun onHideMenuItemClick() {
        // TODO analytics
        // TODO hide card and refresh
    }

    private fun onCtaClick() {
        // TODO analytics
        // TODO navigation event to open SotW recording URL
    }
}
