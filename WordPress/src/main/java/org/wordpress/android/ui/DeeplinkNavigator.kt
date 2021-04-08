package org.wordpress.android.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.DeeplinkNavigator.NavigateAction.OpenEditor
import org.wordpress.android.ui.DeeplinkNavigator.NavigateAction.OpenEditorForSite
import org.wordpress.android.ui.DeeplinkNavigator.NavigateAction.OpenInBrowser
import org.wordpress.android.ui.DeeplinkNavigator.NavigateAction.OpenInEditor
import org.wordpress.android.ui.DeeplinkNavigator.NavigateAction.OpenInWebView
import org.wordpress.android.ui.DeeplinkNavigator.NavigateAction.StartCreateSiteFlow
import org.wordpress.android.util.UriWrapper
import javax.inject.Inject

class DeeplinkNavigator
@Inject constructor() {
    fun handleNavigationAction(navigateAction: NavigateAction, activity: AppCompatActivity) {
        when (navigateAction) {
            is OpenInWebView -> {
                DeeplinkWebViewActivity.openUrl(activity, navigateAction.uri.toString())
            }
            is StartCreateSiteFlow -> {
                if (navigateAction.isSignedToWpCom) {
                    ActivityLauncher.newBlogForResult(activity)
                } else {
                    ActivityLauncher.addSelfHostedSiteForResult(activity)
                }
            }
            OpenEditor -> ActivityLauncher.openEditorInNewStack(activity)
            is OpenEditorForSite -> ActivityLauncher.openEditorForSiteInNewStack(
                    activity,
                    navigateAction.site
            )
            is OpenInBrowser -> {
                val browserIntent = Intent(Intent.ACTION_VIEW, navigateAction.uri.uri)
                activity.startActivity(browserIntent)
            }
            is OpenInEditor -> ActivityLauncher.openEditorForPostInNewStack(
                    activity,
                    navigateAction.site,
                    navigateAction.postId
            )
        }
        activity.finish()
    }

    sealed class NavigateAction {
        data class OpenInWebView(val uri: UriWrapper) : NavigateAction()
        data class OpenInBrowser(val uri: UriWrapper) : NavigateAction()
        data class OpenInEditor(val site: SiteModel, val postId: Int) : NavigateAction()
        data class OpenEditorForSite(val site: SiteModel) : NavigateAction()
        object OpenEditor : NavigateAction()
        data class StartCreateSiteFlow(val isSignedToWpCom: Boolean) : NavigateAction()
    }
}
