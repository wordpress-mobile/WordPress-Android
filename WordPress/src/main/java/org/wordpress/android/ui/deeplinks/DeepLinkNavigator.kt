package org.wordpress.android.ui.deeplinks

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.ActivityNavigator
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.LoginForResult
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenEditor
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenEditorForPost
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenEditorForSite
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenInBrowser
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenInReader
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenJetpackForDeepLink
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenLoginPrologue
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenMySite
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenNotifications
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenPages
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenPagesForSite
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenQRCodeAuthFlow
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenFeedInReader
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenReader
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenReaderDiscover
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenReaderSearch
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenTagInReader
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenStats
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenStatsForSite
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenStatsForSiteAndTimeframe
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenStatsForTimeframe
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.ShowSignInFlow
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.StartCreateSiteFlow
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.ViewPostInReader
import org.wordpress.android.ui.sitecreation.misc.SiteCreationSource.DEEP_LINK
import org.wordpress.android.ui.sitemonitor.SiteMonitorType
import org.wordpress.android.ui.stats.StatsTimeframe
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.UTILS
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.UriWrapper
import javax.inject.Inject

class DeepLinkNavigator
@Inject constructor(private val activityNavigator: ActivityNavigator) {
    @Suppress("ComplexMethod", "LongMethod")
    fun handleNavigationAction(navigateAction: NavigateAction, activity: AppCompatActivity) {
        when (navigateAction) {
            LoginForResult -> ActivityLauncher.loginForDeeplink(activity)
            StartCreateSiteFlow -> {
                ActivityLauncher.showMainActivityAndSiteCreationActivity(activity, DEEP_LINK)
            }
            ShowSignInFlow -> ActivityLauncher.showSignInForResultWpComOnly(activity)
            OpenEditor -> ActivityLauncher.openEditorInNewStack(activity)
            is OpenEditorForSite -> ActivityLauncher.openEditorForSiteInNewStack(
                activity,
                navigateAction.site
            )
            is OpenInBrowser -> openInBrowser(activity, navigateAction.uri)
            is OpenEditorForPost -> ActivityLauncher.openEditorForPostInNewStack(
                activity,
                navigateAction.site,
                navigateAction.postId
            )
            OpenStats -> ActivityLauncher.viewStatsInNewStack(activity)
            is OpenStatsForTimeframe -> ActivityLauncher.viewStatsForTimeframeInNewStack(
                activity,
                navigateAction.statsTimeframe
            )

            is OpenStatsForSite -> activityNavigator.openStatsInNewStack(activity, navigateAction.site)

            is OpenStatsForSiteAndTimeframe -> activityNavigator.openStatsInNewStack(
                activity,
                navigateAction.site,
                navigateAction.statsTimeframe
            )

            OpenReader -> ActivityLauncher.viewReaderInNewStack(activity)
            OpenReaderDiscover -> ActivityLauncher.viewReaderDiscoverInNewStack(activity)
            OpenReaderSearch -> ActivityLauncher.viewReaderSearchInNewStack(activity)
            is OpenFeedInReader -> ActivityLauncher.viewReaderFeedInNewStack(activity, navigateAction.feedId)
            is OpenTagInReader -> ActivityLauncher.viewReaderTagInNewStack(activity, navigateAction.tagSlug)
            is OpenInReader -> ActivityLauncher.viewPostDeeplinkInNewStack(activity, navigateAction.uri.uri)
            is ViewPostInReader -> ActivityLauncher.viewReaderPostDetailInNewStack(
                activity,
                navigateAction.blogId,
                navigateAction.postId,
                navigateAction.uri.uri
            )
            OpenNotifications -> ActivityLauncher.viewNotificationsInNewStack(activity)
            is OpenPagesForSite -> ActivityLauncher.viewPagesInNewStack(activity, navigateAction.site)
            OpenPages -> ActivityLauncher.viewPagesInNewStack(activity)
            is OpenQRCodeAuthFlow -> ActivityLauncher.startQRCodeAuthFlowInNewStack(activity, navigateAction.uri)
            OpenMySite -> ActivityLauncher.viewMySiteInNewStack(activity)
            OpenLoginPrologue -> ActivityLauncher.showLoginPrologue(activity)
            is OpenJetpackForDeepLink ->
                ActivityLauncher.openJetpackForDeeplink(activity, navigateAction.action, navigateAction.uri)
            is NavigateAction.OpenJetpackStaticPosterView ->
                ActivityLauncher.showJetpackStaticPoster(activity)
            is NavigateAction.OpenMediaForSite -> activityNavigator.openMediaInNewStack(activity, navigateAction.site)
            NavigateAction.OpenMedia -> activityNavigator.openMediaInNewStack(activity)
            is NavigateAction.OpenMediaPickerForSite -> activityNavigator.openMediaPickerInNewStack(
                activity,
                navigateAction.site
            )
            NavigateAction.DomainManagement -> ActivityLauncher.openDomainManagement(activity)
            is NavigateAction.OpenSiteMonitoringForSite -> activityNavigator.openSiteMonitoringInNewStack(
                activity,
                navigateAction.site,
                navigateAction.siteMonitorType
            )
            is NavigateAction.OpenMySiteWithMessage -> activityNavigator.openMySiteWithMessageInNewStack(
                activity,
                navigateAction.message
            )
        }
        if (navigateAction != LoginForResult) {
            activity.finish()
        }
    }

    /**
     * Opens the uri with an external browser. The device may have no app able to handle a web link (no browser
     * installed, browser disabled or restricted by a work profile), so the missing handler is reported to the user
     * instead of crashing the deep link entry point.
     *
     * The receiver activity has no UI of its own, so it is finished when the hand-off fails. Otherwise the user is
     * left on an empty screen after the toast. The toast still shows, since it does not depend on the activity.
     */
    private fun openInBrowser(activity: AppCompatActivity, uri: UriWrapper) {
        @SuppressWarnings("UnsafeImplicitIntentLaunch")
        val browserIntent = Intent(Intent.ACTION_VIEW, uri.uri)
        try {
            activity.startActivity(browserIntent)
        } catch (e: ActivityNotFoundException) {
            ToastUtils.showToast(activity, R.string.cant_open_url, ToastUtils.Duration.LONG)
            AppLog.e(UTILS, "No app available on the device to open the deep link: $uri", e)
            activity.finish()
        }
    }

    sealed class NavigateAction {
        object LoginForResult : NavigateAction()
        data class OpenInBrowser(val uri: UriWrapper) : NavigateAction()
        data class OpenEditorForPost(val site: SiteModel, val postId: Int) : NavigateAction()
        data class OpenEditorForSite(val site: SiteModel) : NavigateAction()
        object OpenReader : NavigateAction()
        object OpenReaderDiscover : NavigateAction()
        object OpenReaderSearch : NavigateAction()
        data class OpenFeedInReader(val feedId: Long) : NavigateAction()
        data class OpenTagInReader(val tagSlug: String) : NavigateAction()
        data class OpenInReader(val uri: UriWrapper) : NavigateAction()
        data class ViewPostInReader(val blogId: Long, val postId: Long, val uri: UriWrapper) : NavigateAction()
        object OpenEditor : NavigateAction()
        data class OpenStatsForSiteAndTimeframe(val site: SiteModel, val statsTimeframe: StatsTimeframe) :
            NavigateAction()

        data class OpenStatsForSite(val site: SiteModel) : NavigateAction()
        data class OpenStatsForTimeframe(val statsTimeframe: StatsTimeframe) : NavigateAction()
        object OpenStats : NavigateAction()
        object StartCreateSiteFlow : NavigateAction()
        object ShowSignInFlow : NavigateAction()
        object OpenNotifications : NavigateAction()
        data class OpenPagesForSite(val site: SiteModel) : NavigateAction()
        object OpenPages : NavigateAction()
        data class OpenQRCodeAuthFlow(val uri: String) : NavigateAction()
        object OpenMySite : NavigateAction()
        object OpenLoginPrologue : NavigateAction()
        data class OpenJetpackForDeepLink(val action: String?, val uri: UriWrapper) : NavigateAction()
        object OpenJetpackStaticPosterView : NavigateAction()
        data class OpenMediaForSite(val site: SiteModel) : NavigateAction()
        object OpenMedia : NavigateAction()
        data class OpenMediaPickerForSite(val site: SiteModel) : NavigateAction()
        object DomainManagement : NavigateAction()
        data class OpenSiteMonitoringForSite(val site: SiteModel?, val siteMonitorType: SiteMonitorType) :
            NavigateAction()
        data class OpenMySiteWithMessage(val message: Int) : NavigateAction()
    }
}
