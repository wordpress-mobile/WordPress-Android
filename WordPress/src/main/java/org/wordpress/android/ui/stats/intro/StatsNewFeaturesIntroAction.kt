package org.wordpress.android.ui.stats.intro

import org.wordpress.android.fluxc.model.SiteModel

sealed class StatsNewFeaturesIntroAction {
    data class OpenStats(val site: SiteModel) : StatsNewFeaturesIntroAction()
    object DismissDialog : StatsNewFeaturesIntroAction()
}
