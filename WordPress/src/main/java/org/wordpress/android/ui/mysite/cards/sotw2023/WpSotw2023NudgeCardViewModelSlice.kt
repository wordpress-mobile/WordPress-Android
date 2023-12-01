package org.wordpress.android.ui.mysite.cards.sotw2023

import org.wordpress.android.R
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.WpSotw2023NudgeCardModel
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.config.WpSotw2023NudgeFeatureConfig
import javax.inject.Inject

class WpSotw2023NudgeCardViewModelSlice @Inject constructor(
    private val featureConfig: WpSotw2023NudgeFeatureConfig,
) {
    fun buildCard(): WpSotw2023NudgeCardModel? = WpSotw2023NudgeCardModel(
        title = UiStringRes(R.string.wp_sotw_2023_dashboard_nudge_title),
        text = UiStringRes(R.string.wp_sotw_2023_dashboard_nudge_text),
        ctaText = UiStringRes(R.string.wp_sotw_2023_dashboard_nudge_cta),
        onHideMenuItemClick = ListItemInteraction.create(::onHideMenuItemClick),
        onCtaClick = ListItemInteraction.create(::onCtaClick),
    ).takeIf { featureConfig.isEnabled() }

    private fun onHideMenuItemClick() {
        // TODO thomashortadev analytics
        // TODO thomashortadev hide card and refresh
    }

    private fun onCtaClick() {
        // TODO thomashortadev analytics
        // TODO thomashortadev navigation event to open SotW recording URL
    }
}
