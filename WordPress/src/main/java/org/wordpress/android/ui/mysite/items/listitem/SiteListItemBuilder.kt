package org.wordpress.android.ui.mysite.items.listitem

import android.text.TextUtils
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.ListItem
import org.wordpress.android.ui.mysite.MySiteViewModel
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.ACTIVITY_LOG
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.ADMIN
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.BACKUP
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.DOMAINS
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.JETPACK_SETTINGS
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.PAGES
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.PEOPLE
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.PLAN
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.PLUGINS
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.SCAN
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.SHARING
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.SITE_SETTINGS
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.THEMES
import org.wordpress.android.ui.plugins.PluginUtilsWrapper
import org.wordpress.android.ui.themes.ThemeBrowserUtils
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.SiteUtilsWrapper
import org.wordpress.android.util.config.SiteDomainsFeatureConfig
import java.util.GregorianCalendar
import java.util.TimeZone
import javax.inject.Inject

class SiteListItemBuilder @Inject constructor(
    private val accountStore: AccountStore,
    private val pluginUtilsWrapper: PluginUtilsWrapper,
    private val siteUtilsWrapper: SiteUtilsWrapper,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val themeBrowserUtils: ThemeBrowserUtils,
    private val siteDomainsFeatureConfig: SiteDomainsFeatureConfig
) {
    fun buildActivityLogItemIfAvailable(site: SiteModel, onClick: (ListItemAction) -> Unit): ListItem? {
        val isWpComOrJetpack = siteUtilsWrapper.isAccessedViaWPComRest(
                site
        ) || site.isJetpackConnected
        return if (site.hasCapabilityManageOptions && isWpComOrJetpack && !site.isWpForTeamsSite) {
            ListItem(
                    R.drawable.ic_gridicons_clipboard_white_24dp,
                    UiStringRes(R.string.activity_log),
                    onClick = ListItemInteraction.create(ACTIVITY_LOG, onClick)
            )
        } else null
    }

    fun buildBackupItemIfAvailable(onClick: (ListItemAction) -> Unit, isBackupAvailable: Boolean = false): ListItem? {
        return if (isBackupAvailable) {
            ListItem(
                    R.drawable.ic_gridicons_cloud_upload_white_24dp,
                    UiStringRes(R.string.backup),
                    onClick = ListItemInteraction.create(BACKUP, onClick)
            )
        } else null
    }

    fun buildScanItemIfAvailable(onClick: (ListItemAction) -> Unit, isScanAvailable: Boolean = false): ListItem? {
        return if (isScanAvailable) {
            ListItem(
                    R.drawable.ic_baseline_security_white_24dp,
                    UiStringRes(R.string.scan),
                    onClick = ListItemInteraction.create(SCAN, onClick)
            )
        } else null
    }

    fun buildJetpackItemIfAvailable(site: SiteModel, onClick: (ListItemAction) -> Unit): ListItem? {
        val jetpackSettingsVisible = site.isJetpackConnected && // jetpack is installed and connected
                !site.isWPComAtomic &&
                siteUtilsWrapper.isAccessedViaWPComRest(site) && // is using .com login
                site.hasCapabilityManageOptions // has permissions to manage the site
        return if (jetpackSettingsVisible) {
            ListItem(
                    R.drawable.ic_cog_white_24dp,
                    UiStringRes(R.string.my_site_btn_jetpack_settings),
                    onClick = ListItemInteraction.create(JETPACK_SETTINGS, onClick)
            )
        } else null
    }

    @Suppress("ComplexCondition")
    fun buildPlanItemIfAvailable(
        site: SiteModel,
        showFocusPoint: Boolean,
        onClick: (ListItemAction) -> Unit
    ): ListItem? {
        val planShortName = site.planShortName
        return if (!TextUtils.isEmpty(planShortName) &&
                site.hasCapabilityManageOptions &&
                !site.isWpForTeamsSite &&
                (site.isWPCom || site.isAutomatedTransfer)) {
            ListItem(
                    R.drawable.ic_plans_white_24dp,
                    UiStringRes(R.string.plan),
                    secondaryText = UiStringText(planShortName),
                    onClick = ListItemInteraction.create(PLAN, onClick),
                    showFocusPoint = showFocusPoint
            )
        } else null
    }

    fun buildPagesItemIfAvailable(
        site: SiteModel,
        onClick: (ListItemAction) -> Unit,
        showFocusPoint: Boolean
    ): ListItem? {
        return if (site.isSelfHostedAdmin || site.hasCapabilityEditPages) {
            ListItem(
                    R.drawable.ic_pages_white_24dp,
                    UiStringRes(R.string.my_site_btn_site_pages),
                    onClick = ListItemInteraction.create(PAGES, onClick),
                    showFocusPoint = showFocusPoint
            )
        } else null
    }

    fun buildAdminItemIfAvailable(site: SiteModel, onClick: (ListItemAction) -> Unit): ListItem? {
        return if (shouldShowWPAdmin(site)) {
            ListItem(
                    R.drawable.ic_wordpress_white_24dp,
                    UiStringRes(R.string.my_site_btn_view_admin),
                    secondaryIcon = R.drawable.ic_external_white_24dp,
                    onClick = ListItemInteraction.create(ADMIN, onClick)
            )
        } else null
    }

    fun buildPeopleItemIfAvailable(site: SiteModel, onClick: (ListItemAction) -> Unit): ListItem? {
        return if (site.hasCapabilityListUsers) {
            ListItem(
                    R.drawable.ic_user_white_24dp,
                    UiStringRes(R.string.people),
                    onClick = ListItemInteraction.create(PEOPLE, onClick)
            )
        } else null
    }

    fun buildPluginItemIfAvailable(site: SiteModel, onClick: (ListItemAction) -> Unit): ListItem? {
        return if (pluginUtilsWrapper.isPluginFeatureAvailable(site)) {
            ListItem(
                    R.drawable.ic_plugins_white_24dp,
                    UiStringRes(R.string.my_site_btn_plugins),
                    onClick = ListItemInteraction.create(PLUGINS, onClick)
            )
        } else null
    }

    fun buildShareItemIfAvailable(
        site: SiteModel,
        onClick: (ListItemAction) -> Unit,
        showFocusPoint: Boolean = false
    ): ListItem? {
        return if (site.supportsSharing()) {
            ListItem(
                    R.drawable.ic_share_white_24dp,
                    UiStringRes(R.string.my_site_btn_sharing),
                    showFocusPoint = showFocusPoint,
                    onClick = ListItemInteraction.create(SHARING, onClick)
            )
        } else null
    }

    fun buildDomainsItemIfAvailable(site: SiteModel, onClick: (ListItemAction) -> Unit): ListItem? {
        return if (
                buildConfigWrapper.isJetpackApp &&
                siteDomainsFeatureConfig.isEnabled() &&
                site.hasCapabilityManageOptions
        ) {
            ListItem(
                    R.drawable.ic_domains_white_24dp,
                    UiStringRes(R.string.my_site_btn_domains),
                    onClick = ListItemInteraction.create(DOMAINS, onClick)
            )
        } else null
    }

    fun buildSiteSettingsItemIfAvailable(site: SiteModel, onClick: (ListItemAction) -> Unit): ListItem? {
        return if (site.hasCapabilityManageOptions || !siteUtilsWrapper.isAccessedViaWPComRest(site)) {
            ListItem(
                    R.drawable.ic_cog_white_24dp,
                    UiStringRes(R.string.my_site_btn_site_settings),
                    onClick = ListItemInteraction.create(SITE_SETTINGS, onClick)
            )
        } else null
    }

    private fun shouldShowWPAdmin(site: SiteModel): Boolean {
        return if (!site.isWPCom) {
            true
        } else {
            val dateCreated = DateTimeUtils.dateFromIso8601(
                    accountStore.account
                            .date
            )
            val calendar = GregorianCalendar(HIDE_WP_ADMIN_YEAR, HIDE_WP_ADMIN_MONTH, HIDE_WP_ADMIN_DAY)
            calendar.timeZone = TimeZone.getTimeZone(MySiteViewModel.HIDE_WP_ADMIN_GMT_TIME_ZONE)
            dateCreated == null || dateCreated.before(calendar.time)
        }
    }

    fun buildThemesItemIfAvailable(site: SiteModel, onClick: (ListItemAction) -> Unit): MySiteCardAndItem? {
        return if (themeBrowserUtils.isAccessible(site)) {
            ListItem(
                    R.drawable.ic_themes_white_24dp,
                    UiStringRes(R.string.themes),
                    onClick = ListItemInteraction.create(THEMES, onClick)
            )
        } else null
    }

    companion object {
        const val HIDE_WP_ADMIN_YEAR = 2015
        const val HIDE_WP_ADMIN_MONTH = 9
        const val HIDE_WP_ADMIN_DAY = 7
    }
}
