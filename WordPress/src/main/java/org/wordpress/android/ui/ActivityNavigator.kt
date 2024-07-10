package org.wordpress.android.ui

import android.content.Context
import android.content.Intent
import androidx.core.app.TaskStackBuilder
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
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
import org.wordpress.android.ui.mysite.menu.KEY_QUICK_START_EVENT
import org.wordpress.android.ui.mysite.menu.MenuActivity
import org.wordpress.android.ui.mysite.personalization.PersonalizationActivity
import org.wordpress.android.ui.quickstart.QuickStartEvent
import org.wordpress.android.ui.sitemonitor.SiteMonitorParentActivity
import org.wordpress.android.ui.sitemonitor.SiteMonitorType
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.analytics.AnalyticsUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityNavigator @Inject constructor() {
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

    fun openUnifiedMySiteMenu(context: Context, quickStartEvent: QuickStartEvent? = null) {
        if (quickStartEvent != null) {
            context.startActivity(
                Intent(context, MenuActivity::class.java).apply {
                    putExtra(KEY_QUICK_START_EVENT, quickStartEvent)
                }
            )
            return
        }
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
}
