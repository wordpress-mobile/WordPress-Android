package org.wordpress.android.ui

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import org.wordpress.android.ui.DeeplinkNavigator.NavigateAction.OpenInActionView
import org.wordpress.android.ui.DeeplinkNavigator.NavigateAction.StartCreateSiteFlow
import javax.inject.Inject

class DeeplinkNavigator
@Inject constructor() {
    fun handleNavigationAction(navigateAction: NavigateAction, activity: AppCompatActivity) {
        when (navigateAction) {
            is OpenInActionView -> {
                val browserIntent = Intent(Intent.ACTION_VIEW, navigateAction.uri)
                activity.startActivity(browserIntent)
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
        data class OpenInActionView(val uri: Uri) : NavigateAction()
        data class StartCreateSiteFlow(val isSignedToWpCom: Boolean) : NavigateAction()
    }
}
