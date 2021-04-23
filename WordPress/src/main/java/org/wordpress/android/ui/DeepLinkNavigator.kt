package org.wordpress.android.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.OpenEditor
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.OpenEditorForPost
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.OpenEditorForSite
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.OpenInBrowser
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.OpenInReader
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.OpenNotifications
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.OpenReader
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.OpenStats
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.OpenStatsForSite
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.OpenStatsForSiteAndTimeframe
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.OpenStatsForTimeframe
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.ShowSignInFlow
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.StartCreateSiteFlow
import org.wordpress.android.ui.stats.StatsTimeframe
import org.wordpress.android.util.UriWrapper
import javax.inject.Inject

class DeepLinkNavigator
@Inject constructor() {
    fun handleNavigationAction(navigateAction: NavigateAction, activity: AppCompatActivity) {
        when (navigateAction) {
            StartCreateSiteFlow -> ActivityLauncher.showMainActivityAndSiteCreationActivity(activity)
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
            OpenNotifications -> ActivityLauncher.viewNotificationsInNewStack(activity)
        }
        activity.finish()
    }

    sealed class NavigateAction {
        data class OpenInBrowser(val uri: UriWrapper) : NavigateAction()
        data class OpenEditorForPost(val site: SiteModel, val postId: Int) : NavigateAction()
        data class OpenEditorForSite(val site: SiteModel) : NavigateAction()
        object OpenReader : NavigateAction()
        data class OpenInReader(val uri: UriWrapper) : NavigateAction()
        object OpenEditor : NavigateAction()
        data class OpenStatsForSiteAndTimeframe(val site: SiteModel, val statsTimeframe: StatsTimeframe) :
                NavigateAction()

        data class OpenStatsForSite(val site: SiteModel) : NavigateAction()
        data class OpenStatsForTimeframe(val statsTimeframe: StatsTimeframe) : NavigateAction()
        object OpenStats : NavigateAction()
        object StartCreateSiteFlow : NavigateAction()
        object ShowSignInFlow : NavigateAction()
        object OpenNotifications : NavigateAction()
    }
}
