package org.wordpress.android.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.TaskStackBuilder
import androidx.core.net.toUri
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.accounts.login.applicationpassword.ApplicationPasswordDialogActivity
import org.wordpress.android.ui.accounts.login.applicationpassword.ApplicationPasswordReauthenticateDialogActivity
import org.wordpress.android.ui.accounts.login.applicationpassword.ApplicationPasswordRequiredDialogActivity
import org.wordpress.android.ui.blaze.BlazeFlowSource
import org.wordpress.android.ui.blaze.blazecampaigns.ARG_EXTRA_BLAZE_CAMPAIGN_PAGE
import org.wordpress.android.ui.blaze.blazecampaigns.BlazeCampaignPage
import org.wordpress.android.ui.blaze.blazecampaigns.BlazeCampaignParentActivity
import org.wordpress.android.ui.blaze.blazecampaigns.campaigndetail.CampaignDetailPageSource
import org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting.CampaignListingPageSource
import org.wordpress.android.ui.blaze.blazepromote.ARG_BLAZE_FLOW_SOURCE
import org.wordpress.android.ui.blaze.blazepromote.ARG_BLAZE_SHOULD_SHOW_OVERLAY
import org.wordpress.android.ui.blaze.blazepromote.BlazePromoteParentActivity
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainActivity
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainActivity.Companion.PICKED_DOMAIN_KEY
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainActivity.Companion.PICKED_DOMAIN_PRIVACY
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainActivity.Companion.PICKED_PRODUCT_ID
import org.wordpress.android.ui.main.WPMainActivity
import org.wordpress.android.ui.media.MediaBrowserActivity
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.ui.mysite.menu.MenuActivity
import org.wordpress.android.ui.mysite.personalization.PersonalizationActivity
import org.wordpress.android.ui.newstats.NewStatsActivity
import org.wordpress.android.ui.newstats.NewStatsRouting
import org.wordpress.android.ui.newstats.poststats.PostStatsDetailActivity
import org.wordpress.android.ui.newstats.toNewStatsTarget
import org.wordpress.android.ui.stats.StatsConstants
import org.wordpress.android.ui.stats.StatsTimeframe
import org.wordpress.android.ui.stats.refresh.lists.detail.StatsDetailActivity
import org.wordpress.android.ui.stats.refresh.utils.StatsLaunchedFrom
import org.wordpress.android.ui.sitemonitor.SiteMonitorParentActivity
import org.wordpress.android.ui.sitemonitor.SiteMonitorType
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.analytics.AnalyticsUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityNavigator @Inject constructor(
    private val newStatsRouting: NewStatsRouting
) {
    /**
     * Opens Stats for [site], honouring the New Stats rollout. The old-stats [statsTimeframe] the
     * deep links carry is mapped onto the closest New Stats tab and period.
     *
     * A null [site] falls through to the old path on purpose - it reports STATS_ACCESS_ERROR and
     * toasts, which New Stats has no equivalent for.
     */
    fun openStats(context: Context, site: SiteModel?, statsTimeframe: StatsTimeframe? = null) {
        if (site != null && newStatsRouting.isNewStatsEnabled()) {
            val (tab, period) = statsTimeframe.toNewStatsTarget()
            context.startActivity(
                NewStatsActivity.buildIntent(context, StatsLaunchedFrom.LINK, tab, period, site.id)
            )
        } else {
            ActivityLauncher.viewBlogStatsForTimeframe(context, site, statsTimeframe, StatsLaunchedFrom.LINK)
        }
    }

    /**
     * Same as [openStats], but stacked over the main activity so backing out of Stats lands on My
     * Site rather than leaving the app. Used by the deep links that name a site.
     */
    fun openStatsInNewStack(context: Context, site: SiteModel, statsTimeframe: StatsTimeframe? = null) {
        if (!newStatsRouting.isNewStatsEnabled()) {
            ActivityLauncher.viewStatsInNewStack(context, site, statsTimeframe, StatsLaunchedFrom.LINK)
            return
        }
        val (tab, period) = statsTimeframe.toNewStatsTarget()
        TaskStackBuilder.create(context)
            .addNextIntent(getMainActivityInNewStack(context))
            .addNextIntent(NewStatsActivity.buildIntent(context, StatsLaunchedFrom.LINK, tab, period, site.id))
            .startActivities()
    }

    /**
     * Opens the per-item Stats screen for a single post, honouring the New Stats rollout. Used by
     * the Posts lists, whose overflow menu offers Stats for a published post.
     */
    fun openPostStats(context: Context, site: SiteModel, postId: Long, postTitle: String, postUrl: String?) =
        openItemStats(context, site, postId, postTitle, postUrl, StatsConstants.ITEM_TYPE_POST)

    /**
     * The Pages list's equivalent. The old screen is given ITEM_TYPE_HOME_PAGE for every page,
     * which is what that list has always passed it; New Stats takes no item type and reads the
     * item's kind from the response, so both lists can share one gate.
     */
    fun openPageStats(context: Context, site: SiteModel, pageId: Long, pageTitle: String, pageUrl: String?) =
        openItemStats(context, site, pageId, pageTitle, pageUrl, StatsConstants.ITEM_TYPE_HOME_PAGE)

    /**
     * [itemUrl] and [itemType] reach only the old screen: New Stats takes the item's URL, type and
     * status from the stats/post/<id> response instead, which is what lets it keep the header
     * inert for an item that is no longer public. [itemTitle] is a placeholder there, used until
     * that response arrives.
     *
     * The site travels as a local id rather than a SiteModel because New Stats always renders the
     * currently selected site - see selectNewStatsSiteIfNeeded.
     *
     * Both branches report the open as [StatsLaunchedFrom.POSTS]: that is the tap source the old
     * screen hardcodes for every per-item open, pages included, so the two stay comparable.
     */
    @Suppress("LongParameterList")
    private fun openItemStats(
        context: Context,
        site: SiteModel,
        itemId: Long,
        itemTitle: String,
        itemUrl: String?,
        itemType: String
    ) {
        if (newStatsRouting.isNewStatsEnabled()) {
            PostStatsDetailActivity.start(context, itemId, itemTitle, site.id, StatsLaunchedFrom.POSTS)
        } else {
            StatsDetailActivity.start(context, site, itemId, itemType, itemTitle, itemUrl)
        }
    }

    fun navigateToCampaignListingPage(context: Context, campaignListingPageSource: CampaignListingPageSource) {
        context.startActivity(
            Intent(context, BlazeCampaignParentActivity::class.java).apply {
                putExtra(
                    ARG_EXTRA_BLAZE_CAMPAIGN_PAGE,
                    BlazeCampaignPage.CampaignListingPage(campaignListingPageSource)
                )
            }
        )
    }

    fun navigateToCampaignDetailPage(
        context: Context,
        campaignId: String,
        campaignDetailPageSource: CampaignDetailPageSource
    ) {
        context.startActivity(
            Intent(context, BlazeCampaignParentActivity::class.java).apply {
                putExtra(
                    ARG_EXTRA_BLAZE_CAMPAIGN_PAGE,
                    BlazeCampaignPage.CampaignDetailsPage(campaignId, campaignDetailPageSource)
                )
            }
        )
    }

    fun openPromoteWithBlaze(
        context: Context,
        source: BlazeFlowSource,
        shouldShowOverlay: Boolean = false
    ) {
        val intent = Intent(context, BlazePromoteParentActivity::class.java)
        intent.putExtra(ARG_BLAZE_FLOW_SOURCE, source)
        intent.putExtra(ARG_BLAZE_SHOULD_SHOW_OVERLAY, shouldShowOverlay)
        context.startActivity(intent)
    }

    fun openDomainTransfer(
        context: Context,
        url: String
    ) {
        WPWebViewActivity.openUrlByUsingGlobalWPCOMCredentials(context, url)
    }

    fun openDashboardPersonalization(context: Context) {
        context.startActivity(Intent(context, PersonalizationActivity::class.java))
    }

    fun openUnifiedMySiteMenu(context: Context) {
        context.startActivity(Intent(context, MenuActivity::class.java))
    }

    fun openPurchaseDomain(context: Context, productId: Int, domainName: String, domainSupportsPrivacy: Boolean) {
        context.startActivity(
            Intent(context, PurchaseDomainActivity::class.java)
                .putExtra(PICKED_PRODUCT_ID, productId)
                .putExtra(PICKED_DOMAIN_KEY, domainName)
                .putExtra(PICKED_DOMAIN_PRIVACY, domainSupportsPrivacy)
        )
    }

    fun viewCurrentBlogMedia(context: Context, site: SiteModel?) {
        val intent = Intent(context, MediaBrowserActivity::class.java)
        intent.putExtra(WordPress.SITE, site)
        intent.putExtra(MediaBrowserActivity.ARG_BROWSER_TYPE, MediaBrowserType.BROWSER)
        context.startActivity(intent)
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.OPENED_MEDIA_LIBRARY, site)
    }

    fun openMediaInNewStack(context: Context, site: SiteModel?) {
        if (site == null) {
            ToastUtils.showToast(context, R.string.media_cannot_be_started, ToastUtils.Duration.SHORT)
            return
        }
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.OPENED_MEDIA_LIBRARY, site)
        val taskStackBuilder = TaskStackBuilder.create(context)
        val mainActivityIntent = getMainActivityInNewStack(context)
        val intent = Intent(context, MediaBrowserActivity::class.java)
        intent.putExtra(WordPress.SITE, site)
        taskStackBuilder
            .addNextIntent(mainActivityIntent)
            .addNextIntent(intent)
            .startActivities()
    }

    fun openMediaInNewStack(context: Context) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.OPENED_MEDIA_LIBRARY)
        val intent = getMainActivityInNewStack(context)
        intent.putExtra(WPMainActivity.ARG_OPEN_PAGE, WPMainActivity.ARG_MEDIA)
        context.startActivity(intent)
    }

    fun openMediaPickerInNewStack(context: Context, site: SiteModel?) {
        if (site == null) {
            ToastUtils.showToast(context, R.string.media_cannot_be_started, ToastUtils.Duration.SHORT)
            return
        }
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.OPENED_MEDIA_LIBRARY, site)
        val taskStackBuilder = TaskStackBuilder.create(context)
        val mainActivityIntent = getMainActivityInNewStack(context)
        val intent = Intent(context, MediaBrowserActivity::class.java)
        intent.putExtra(WordPress.SITE, site)
        intent.putExtra(MediaBrowserActivity.ARG_LAUNCH_PHOTO_PICKER, true)
        taskStackBuilder
            .addNextIntent(mainActivityIntent)
            .addNextIntent(intent)
            .startActivities()
    }

    private fun getMainActivityInNewStack(context: Context): Intent {
        val mainActivityIntent = Intent(context, WPMainActivity::class.java)
        mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        return mainActivityIntent
    }

    fun navigateToSiteMonitoring(context: Context, site: SiteModel) {
        val intent = Intent(context, SiteMonitorParentActivity::class.java)
        intent.putExtra(WordPress.SITE, site)
        context.startActivity(intent)
    }

    fun openSiteMonitoringInNewStack(
        context: Context,
        site: SiteModel?,
        siteMonitorType: SiteMonitorType = SiteMonitorType.METRICS
    ) {
        if (site == null) {
            ToastUtils.showToast(context, R.string.site_monitoring_cannot_be_started, ToastUtils.Duration.SHORT)
            return
        }
        val props = mutableMapOf("site_monitoring_type" to siteMonitorType.name as Any)
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.OPENED_SITE_MONITORING, site, props)
        val taskStackBuilder = TaskStackBuilder.create(context)
        val mainActivityIntent = getMainActivityInNewStack(context)
        val intent = Intent(context, SiteMonitorParentActivity::class.java)
        intent.putExtra(WordPress.SITE, site)
        intent.putExtra(SiteMonitorParentActivity.ARG_SITE_MONITOR_TYPE_KEY, siteMonitorType)
        taskStackBuilder
            .addNextIntent(mainActivityIntent)
            .addNextIntent(intent)
            .startActivities()
    }

    fun openMySiteWithMessageInNewStack(
        context: Context,
        message: Int
    ) {
        val taskStackBuilder = TaskStackBuilder.create(context)
        val mainActivityIntent = getMainActivityInNewStack(context)
        val intent = Intent(context, WPMainActivity::class.java)
        intent.putExtra(WPMainActivity.ARG_OPEN_PAGE, WPMainActivity.ARG_MY_SITE)
        intent.putExtra(WPMainActivity.ARG_OPEN_PAGE_MESSAGE, message)
        taskStackBuilder
            .addNextIntent(mainActivityIntent)
            .addNextIntent(intent)
            .startActivities()
    }

    fun openIneligibleForVoiceToContent(
        context: Context,
        url: String
    ) {
        WPWebViewActivity.openUrlByUsingGlobalWPCOMCredentials(context, url)
    }

    @Suppress("TooGenericExceptionCaught")
    fun openApplicationPasswordLogin(activity: Activity, url: String) {
        val intent = getCustomTabsIntent(activity)
        val loginUri = url.toUri()
        try {
            intent.launchUrl(activity, loginUri)
        } catch (e: RuntimeException) {
            when (e) {
                is ActivityNotFoundException,
                is SecurityException -> {
                    AppLog.e(
                        AppLog.T.UTILS,
                        "Error opening login URI in CustomTabsIntent, attempting external browser",
                        e
                    )
                    ActivityLauncher.openUrlExternal(activity, loginUri.toString())
                }

                else -> {
                    throw e
                }
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun openInCustomTab(activity: Activity, url: String) {
        val intent = getCustomTabsIntent(activity)
        // Target the browser directly, otherwise our own deep-link aliases can claim WP.com post URLs
        CustomTabsClient.getPackageName(activity, null)?.let { intent.intent.setPackage(it) }
        try {
            intent.launchUrl(activity, url.toUri())
        } catch (e: RuntimeException) {
            when (e) {
                is ActivityNotFoundException,
                is SecurityException -> {
                    AppLog.e(
                        AppLog.T.UTILS,
                        "Error opening URL in CustomTabsIntent, attempting external browser",
                        e
                    )
                    ActivityLauncher.openUrlExternal(activity, url)
                }
                else -> throw e
            }
        }
    }

    private fun getCustomTabsIntent(activity: Activity): CustomTabsIntent {
        return CustomTabsIntent.Builder()
            .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
            .setStartAnimations(
                activity,
                R.anim.activity_slide_in_from_right,
                R.anim.activity_slide_out_to_left
            )
            .setExitAnimations(
                activity,
                R.anim.activity_slide_in_from_left,
                R.anim.activity_slide_out_to_right
            )
            .setUrlBarHidingEnabled(true)
            .setInstantAppsEnabled(false)
            .setShowTitle(false)
            .build()
    }

    fun navigateToApplicationPasswordReauthentication(activity: Activity, authenticationUrl: String) {
        val intent = Intent(activity, ApplicationPasswordReauthenticateDialogActivity::class.java)
        intent.putExtra(ApplicationPasswordDialogActivity.EXTRA_SITE_URL, authenticationUrl)
        activity.startActivity(intent)
    }

    fun navigateToApplicationPasswordRequired(activity: Activity, site: SiteModel, featureName: String) {
        activity.startActivity(
            ApplicationPasswordRequiredDialogActivity.createIntent(activity, site, featureName)
        )
    }
}
