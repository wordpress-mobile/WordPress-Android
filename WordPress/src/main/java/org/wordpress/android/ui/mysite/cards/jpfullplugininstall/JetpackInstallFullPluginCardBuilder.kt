package org.wordpress.android.ui.mysite.cards.jpfullplugininstall

import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.JetpackInstallFullPluginCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.JetpackInstallFullPluginCardBuilderParams
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.util.config.JetpackInstallFullPluginFeatureConfig
import org.wordpress.android.util.extensions.isJetpackConnectedWithoutFullPlugin
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JetpackInstallFullPluginCardBuilder @Inject constructor(
    private val installFullPluginFeatureConfig: JetpackInstallFullPluginFeatureConfig,
) {
    // TODO thomashorta implement show logic (most likely using AppsPrefWrapper
    var shouldShow: Boolean = true

    fun build(
        params: JetpackInstallFullPluginCardBuilderParams
    ): JetpackInstallFullPluginCard? = JetpackInstallFullPluginCard(
        onLearnMoreClick = ListItemInteraction.create(params.onLearnMoreClick),
        onHideMenuItemClick = ListItemInteraction.create(params.onHideMenuItemClick),
    ).takeIf {
        installFullPluginFeatureConfig.isEnabled() && shouldShow && params.site.isJetpackConnectedWithoutFullPlugin()
    }
}
