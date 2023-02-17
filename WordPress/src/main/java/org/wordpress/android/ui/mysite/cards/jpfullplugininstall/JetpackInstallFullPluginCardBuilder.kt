package org.wordpress.android.ui.mysite.cards.jpfullplugininstall

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.JetpackInstallFullPluginCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.JetpackInstallFullPluginCardBuilderParams
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.util.config.JetpackInstallFullPluginFeatureConfig
import org.wordpress.android.util.extensions.activeJetpackConnectionPluginNames
import org.wordpress.android.util.extensions.isJetpackConnectedWithoutFullPlugin
import javax.inject.Inject

class JetpackInstallFullPluginCardBuilder @Inject constructor(
    private val installFullPluginFeatureConfig: JetpackInstallFullPluginFeatureConfig,
    private val appPrefsWrapper: AppPrefsWrapper,
) {
    fun build(
        params: JetpackInstallFullPluginCardBuilderParams
    ): JetpackInstallFullPluginCard? = if (shouldShowCard(params.site)) {
        JetpackInstallFullPluginCard(
            siteName = params.site.name,
            pluginNames = params.site.activeJetpackConnectionPluginNames().orEmpty(),
            onLearnMoreClick = ListItemInteraction.create(params.onLearnMoreClick),
            onHideMenuItemClick = ListItemInteraction.create(params.onHideMenuItemClick),
        )
    } else null

    private fun shouldShowCard(site: SiteModel): Boolean {
        return site.id != 0 &&
                installFullPluginFeatureConfig.isEnabled() &&
                !appPrefsWrapper.getShouldHideJetpackInstallFullPluginCard(site.id) &&
                site.isJetpackConnectedWithoutFullPlugin()

    }
}
