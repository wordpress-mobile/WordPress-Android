package org.wordpress.android.ui.mysite.cards.jpfullplugininstall

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.JetpackInstallFullPluginCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.JetpackInstallFullPluginCardBuilderParams
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.config.JetpackInstallFullPluginFeatureConfig
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
            content = buildContentString(params.site),
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

    private fun buildContentString(site: SiteModel): UiString {
        // TODO 17836-jp-install-card use the site model to extract site name and plugins to build the correct string
        //  it should match the Fullscreen modal screen (mentions plugin if 1 plugin, or generic text if multiple)
        //  for now just use some throwaway logic and non-final strings which will need to be changed
        val jpPluginsCount = site.activeJetpackConnectionPlugins?.split(",")
            ?.filter { it.startsWith("jetpack-") }
            ?.size
            ?: 0

        return if (jpPluginsCount != 1) {
            UiString.UiStringRes(R.string.my_site_jp_install_full_plugin_card_content)
        } else {
            UiString.UiStringResWithParams(
                R.string.jetpack_full_plugin_install_onboarding_description,
                UiString.UiStringText(site.displayName ?: "Unknown"),
                UiString.UiStringText("Plugin Name"),
                UiString.UiStringText("full Jetpack plugin"),
            )
        }
    }
}
