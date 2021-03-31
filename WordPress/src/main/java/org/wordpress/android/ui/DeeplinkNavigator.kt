package org.wordpress.android.ui

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
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
        }
        activity.finish()
    }

    sealed class NavigateAction {
        data class OpenInWebView(val uri: UriWrapper) : NavigateAction()
        data class StartCreateSiteFlow(val isSignedToWpCom: Boolean) : NavigateAction()
    }
}
