package org.wordpress.android.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.OpenEditor
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.OpenEditorForSite
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.OpenInBrowser
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.OpenInEditor
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.StartCreateSiteFlow
import org.wordpress.android.util.UriWrapper
import javax.inject.Inject

class DeepLinkNavigator
@Inject constructor() {
    fun handleNavigationAction(navigateAction: NavigateAction, activity: AppCompatActivity) {
        when (navigateAction) {
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
        data class OpenInBrowser(val uri: UriWrapper) : NavigateAction()
        data class OpenInEditor(val site: SiteModel, val postId: Int) : NavigateAction()
        data class OpenEditorForSite(val site: SiteModel) : NavigateAction()
        object OpenEditor : NavigateAction()
        data class StartCreateSiteFlow(val isSignedToWpCom: Boolean) : NavigateAction()
    }
}
