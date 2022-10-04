package org.wordpress.android.ui.deeplinks

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.LoginForResult
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenEditor
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenEditorForPost
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenEditorForSite
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenHome
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenInBrowser
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenInReader
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenNotifications
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenPages
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenPagesForSite
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenQRCodeAuthFlow
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenReader
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenStats
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenStatsForSite
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenStatsForSiteAndTimeframe
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenStatsForTimeframe
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.ShowSignInFlow
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.StartCreateSiteFlow
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.ViewPostInReader
import org.wordpress.android.ui.sitecreation.misc.SiteCreationSource.DEEP_LINK
import org.wordpress.android.ui.stats.StatsTimeframe
import org.wordpress.android.util.UriWrapper
import javax.inject.Inject

class DeepLinkNavigator
@Inject constructor() {
    @Suppress("ComplexMethod")
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
            is OpenInBrowser -> {
                val browserIntent = Intent(Intent.ACTION_VIEW, navigateAction.uri.uri)
                activity.startActivity(browserIntent)
            }
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
            is OpenStatsForSite -> ActivityLauncher.viewStatsInNewStack(activity, navigateAction.site)
            is OpenStatsForSiteAndTimeframe -> ActivityLauncher.viewStatsInNewStack(
                    activity,
                    navigateAction.site,
                    navigateAction.statsTimeframe
            )
            OpenReader -> ActivityLauncher.viewReaderInNewStack(activity)
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
            is OpenHome -> ActivityLauncher.showHome(activity, navigateAction.isLoggedIn)
        }
        if (navigateAction != LoginForResult) {
            activity.finish()
        }
    }

    sealed class NavigateAction {
        object LoginForResult : NavigateAction()
        data class OpenInBrowser(val uri: UriWrapper) : NavigateAction()
        data class OpenEditorForPost(val site: SiteModel, val postId: Int) : NavigateAction()
        data class OpenEditorForSite(val site: SiteModel) : NavigateAction()
        object OpenReader : NavigateAction()
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
        data class OpenHome(val isLoggedIn: Boolean) : NavigateAction()
    }
}
