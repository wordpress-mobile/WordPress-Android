package org.wordpress.android.ui.mysite.items.listitem

import android.text.TextUtils
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhaseHelper
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.ListItem
import org.wordpress.android.ui.mysite.MySiteViewModel
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.ACTIVITY_LOG
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.ADMIN
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.BACKUP
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.BLAZE
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.DOMAINS
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.PAGES
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.PEOPLE
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.PLAN
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.PLUGINS
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.SCAN
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.SELF_HOSTED_USERS
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
import org.wordpress.android.util.config.SelfHostedUsersFeatureConfig
import org.wordpress.android.util.config.SiteMonitoringFeatureConfig
import java.util.GregorianCalendar
import java.util.TimeZone
import javax.inject.Inject

class SiteListItemBuilder @Inject constructor(
    private val accountStore: AccountStore,
    private val pluginUtilsWrapper: PluginUtilsWrapper,
    private val siteUtilsWrapper: SiteUtilsWrapper,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val themeBrowserUtils: ThemeBrowserUtils,
    private val jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper,
    private val siteMonitoringFeatureConfig: SiteMonitoringFeatureConfig,
    private val selfHostedUsersFeatureConfig: SelfHostedUsersFeatureConfig,
) {
    fun buildActivityLogItemIfAvailable(site: SiteModel, onClick: (ListItemAction) -> Unit): ListItem? {
        val isWpComOrJetpack = siteUtilsWrapper.isAccessedViaWPComRest(
            site
        ) || site.isJetpackConnected
        return if (site.hasCapabilityManageOptions && isWpComOrJetpack && !site.isWpForTeamsSite) {
            ListItem(
                R.drawable.ic_history_white_24dp,
                UiStringRes(R.string.activity_log),
                onClick = ListItemInteraction.create(ACTIVITY_LOG, onClick),
                listItemAction = ACTIVITY_LOG
            )
        } else null
    }

    fun buildBackupItemIfAvailable(onClick: (ListItemAction) -> Unit, isBackupAvailable: Boolean = false): ListItem? {
        return if (isBackupAvailable) {
            ListItem(
                R.drawable.ic_gridicons_cloud_upload_white_24dp,
                UiStringRes(R.string.backup),
                onClick = ListItemInteraction.create(BACKUP, onClick),
                listItemAction = BACKUP
            )
        } else null
    }

    fun buildScanItemIfAvailable(onClick: (ListItemAction) -> Unit, isScanAvailable: Boolean = false): ListItem? {
        return if (isScanAvailable) {
            ListItem(
                R.drawable.ic_baseline_security_white_24dp,
                UiStringRes(R.string.scan),
                onClick = ListItemInteraction.create(SCAN, onClick),
                listItemAction = SCAN
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
            (site.isWPCom || site.isAutomatedTransfer)
        ) {
            ListItem(
                R.drawable.ic_plans_white_24dp,
                UiStringRes(R.string.plan),
                secondaryText = UiStringText(planShortName),
                onClick = ListItemInteraction.create(PLAN, onClick),
                showFocusPoint = showFocusPoint,
                listItemAction = PLAN
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
                showFocusPoint = showFocusPoint,
                listItemAction = PAGES
            )
        } else null
    }

    fun buildAdminItemIfAvailable(site: SiteModel, onClick: (ListItemAction) -> Unit): ListItem? {
        return if (shouldShowWPAdmin(site)) {
            ListItem(
                R.drawable.ic_wordpress_white_24dp,
                UiStringRes(R.string.my_site_btn_wp_admin),
                secondaryIcon = R.drawable.ic_external_white_24dp,
                onClick = ListItemInteraction.create(ADMIN, onClick),
                listItemAction = ADMIN
            )
        } else null
    }

    fun buildPeopleItemIfAvailable(site: SiteModel, onClick: (ListItemAction) -> Unit): ListItem? {
        return if (site.hasCapabilityListUsers) {
            ListItem(
                R.drawable.ic_user_white_24dp,
                UiStringRes(R.string.people),
                onClick = ListItemInteraction.create(PEOPLE, onClick),
                listItemAction = PEOPLE
            )
        } else {
            null
        }
    }

    fun buildSelfHostedUserListItemIfAvailable(site: SiteModel, onClick: (ListItemAction) -> Unit): ListItem? {
        // TODO: Should this excluded JetPack users?
        return if (selfHostedUsersFeatureConfig.isEnabled() && site.selfHostedSiteId > 0) {
            ListItem(
                R.drawable.ic_user_white_24dp,
                UiStringRes(R.string.users),
                onClick = ListItemInteraction.create(SELF_HOSTED_USERS, onClick),
                listItemAction = SELF_HOSTED_USERS
            )
        } else null
    }

    fun buildPluginItemIfAvailable(site: SiteModel, onClick: (ListItemAction) -> Unit): ListItem? {
        return if (pluginUtilsWrapper.isPluginFeatureAvailable(site)) {
            ListItem(
                R.drawable.ic_plugins_white_24dp,
                UiStringRes(R.string.my_site_btn_plugins),
                onClick = ListItemInteraction.create(PLUGINS, onClick),
                listItemAction = PLUGINS
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
                onClick = ListItemInteraction.create(SHARING, onClick),
                listItemAction = SHARING
            )
        } else null
    }

    fun buildDomainsItemIfAvailable(site: SiteModel, onClick: (ListItemAction) -> Unit): ListItem? {
        return if (
            buildConfigWrapper.isJetpackApp && (site.isUsingWpComRestApi && site.hasCapabilityManageOptions)
        ) {
            ListItem(
                R.drawable.ic_domains_white_24dp,
                UiStringRes(R.string.my_site_btn_domains),
                onClick = ListItemInteraction.create(DOMAINS, onClick),
                listItemAction = DOMAINS
            )
        } else null
    }

    fun buildSiteSettingsItemIfAvailable(site: SiteModel, onClick: (ListItemAction) -> Unit): ListItem? {
        return if (site.hasCapabilityManageOptions || !siteUtilsWrapper.isAccessedViaWPComRest(site)) {
            ListItem(
                R.drawable.ic_cog_white_24dp,
                UiStringRes(R.string.my_site_btn_site_settings),
                onClick = ListItemInteraction.create(SITE_SETTINGS, onClick),
                listItemAction = SITE_SETTINGS
            )
        } else null
    }

    @Suppress("ComplexCondition")
    fun buildMeItemIfAvailable(site: SiteModel, onClick: (ListItemAction) -> Unit): ListItem? {
        return if ((!buildConfigWrapper.isJetpackApp &&
                    jetpackFeatureRemovalPhaseHelper.shouldRemoveJetpackFeatures() &&
                    site.hasCapabilityManageOptions) ||
            (!buildConfigWrapper.isJetpackApp &&
                    site.isSelfHostedAdmin)
        ) {
            ListItem(
                R.drawable.ic_user_primary_white_24,
                UiStringRes(R.string.me),
                onClick = ListItemInteraction.create(ListItemAction.ME, onClick),
                disablePrimaryIconTint = true,
                listItemAction = ListItemAction.ME
            )
        } else null
    }

    fun buildBlazeItemIfAvailable(isBlazeEligible: Boolean = false, onClick: (ListItemAction) -> Unit): ListItem? {
        return if (isBlazeEligible) {
            ListItem(
                R.drawable.ic_promote_with_blaze,
                UiStringRes(R.string.blaze_menu_item_label),
                onClick = ListItemInteraction.create(BLAZE, onClick),
                disablePrimaryIconTint = true,
                listItemAction = BLAZE
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
                onClick = ListItemInteraction.create(THEMES, onClick),
                listItemAction = THEMES
            )
        } else null
    }

    @Suppress("ComplexCondition")
    fun buildSiteMonitoringItemIfAvailable(site: SiteModel, onClick: (ListItemAction) -> Unit): MySiteCardAndItem? {
        return if (buildConfigWrapper.isJetpackApp
            && site.isWPComAtomic
            && site.isAdmin
            && siteMonitoringFeatureConfig.isEnabled()
        ) {
            ListItem(
                R.drawable.gb_ic_tool,
                UiStringRes(R.string.site_monitoring),
                onClick = ListItemInteraction.create(ListItemAction.SITE_MONITORING, onClick),
                listItemAction = ListItemAction.SITE_MONITORING
            )
        } else null
    }

    companion object {
        const val HIDE_WP_ADMIN_YEAR = 2015
        const val HIDE_WP_ADMIN_MONTH = 9
        const val HIDE_WP_ADMIN_DAY = 7
    }
}
