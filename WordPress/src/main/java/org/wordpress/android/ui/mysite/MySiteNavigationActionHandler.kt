package org.wordpress.android.ui.mysite

import android.app.Activity
import android.net.Uri
import androidx.fragment.app.Fragment
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCrop.Options
import com.yalantis.ucrop.UCropActivity
import org.wordpress.android.R
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose.CTA_DOMAIN_CREDIT_REDEMPTION
import org.wordpress.android.ui.main.SitePickerActivity
import org.wordpress.android.ui.mysite.SiteNavigationAction.AddNewSite
import org.wordpress.android.ui.mysite.SiteNavigationAction.AddNewStory
import org.wordpress.android.ui.mysite.SiteNavigationAction.AddNewStoryWithMediaIds
import org.wordpress.android.ui.mysite.SiteNavigationAction.AddNewStoryWithMediaUris
import org.wordpress.android.ui.mysite.SiteNavigationAction.ConnectJetpackForStats
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenActivityLog
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenAdmin
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenBackup
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenComments
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenCropActivity
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenDomainRegistration
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenJetpackSettings
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenMeScreen
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenMedia
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenMediaPicker
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenPages
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenPeople
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenPlan
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenPlugins
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenPosts
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenScan
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenSharing
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenSite
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenSitePicker
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenSiteSettings
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenStats
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenStories
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenThemes
import org.wordpress.android.ui.mysite.SiteNavigationAction.StartWPComLoginForJetpackStats
import org.wordpress.android.ui.photopicker.MediaPickerLauncher
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.getColorFromAttribute
import java.io.File
import javax.inject.Inject

class MySiteNavigationActionHandler
@Inject constructor(private val mediaPickerLauncher: MediaPickerLauncher) {
    fun navigate(activity: Activity, fragment: Fragment, action: SiteNavigationAction) {
        when (action) {
            is OpenMeScreen -> ActivityLauncher.viewMeActivityForResult(activity)
            is OpenSitePicker -> ActivityLauncher.showSitePickerForResult(activity, action.site)
            is OpenSite -> ActivityLauncher.viewCurrentSite(activity, action.site, true)
            is OpenMediaPicker -> mediaPickerLauncher.showSiteIconPicker(fragment, action.site)
            is OpenCropActivity -> startCropActivity(activity, fragment, action.imageUri)
            is OpenActivityLog -> ActivityLauncher.viewActivityLogList(activity, action.site)
            is OpenBackup -> ActivityLauncher.viewBackupList(activity, action.site)
            is OpenScan -> ActivityLauncher.viewScan(activity, action.site)
            is OpenPlan -> ActivityLauncher.viewBlogPlans(activity, action.site)
            is OpenPosts -> ActivityLauncher.viewCurrentBlogPosts(activity, action.site)
            is OpenPages -> ActivityLauncher.viewCurrentBlogPages(activity, action.site)
            is OpenAdmin -> ActivityLauncher.viewBlogAdmin(activity, action.site)
            is OpenPeople -> ActivityLauncher.viewCurrentBlogPeople(activity, action.site)
            is OpenSharing -> ActivityLauncher.viewBlogSharing(activity, action.site)
            is OpenSiteSettings -> ActivityLauncher.viewBlogSettingsForResult(activity, action.site)
            is OpenThemes -> ActivityLauncher.viewCurrentBlogThemes(activity, action.site)
            is OpenPlugins -> ActivityLauncher.viewPluginBrowser(activity, action.site)
            is OpenMedia -> ActivityLauncher.viewCurrentBlogMedia(activity, action.site)
            is OpenComments -> ActivityLauncher.viewCurrentBlogComments(activity, action.site)
            is OpenStats -> ActivityLauncher.viewBlogStats(activity, action.site)
            is ConnectJetpackForStats -> ActivityLauncher.viewConnectJetpackForStats(activity, action.site)
            is StartWPComLoginForJetpackStats -> ActivityLauncher.loginForJetpackStats(fragment)
            is OpenJetpackSettings -> ActivityLauncher.viewJetpackSecuritySettings(activity, action.site)
            is OpenStories -> ActivityLauncher.viewStories(activity, action.site, action.event)
            is AddNewStory ->
                ActivityLauncher.addNewStoryForResult(
                        activity,
                        action.site,
                        action.source
                )
            is AddNewStoryWithMediaIds ->
                ActivityLauncher.addNewStoryWithMediaIdsForResult(
                        activity,
                        action.site,
                        action.source,
                        action.mediaIds.toLongArray()
                )
            is AddNewStoryWithMediaUris ->
                ActivityLauncher.addNewStoryWithMediaUrisForResult(
                        activity,
                        action.site,
                        action.source,
                        action.mediaUris.toTypedArray()
                )
            is OpenDomainRegistration -> ActivityLauncher.viewDomainRegistrationActivityForResult(
                    activity,
                    action.site,
                    CTA_DOMAIN_CREDIT_REDEMPTION
            )
            is AddNewSite -> SitePickerActivity.addSite(activity, action.isSignedInWpCom)
        }
    }

    private fun startCropActivity(activity: Activity, fragment: Fragment, imageUri: UriWrapper) {
        val options = Options()
        options.setShowCropGrid(false)
        options.setStatusBarColor(activity.getColorFromAttribute(android.R.attr.statusBarColor))
        options.setToolbarColor(activity.getColorFromAttribute(R.attr.wpColorAppBar))
        options.setToolbarWidgetColor(activity.getColorFromAttribute(R.attr.colorOnSurface))
        options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.NONE, UCropActivity.NONE)
        options.setHideBottomControls(true)
        UCrop.of(imageUri.uri, Uri.fromFile(File(activity.cacheDir, "cropped_for_site_icon.jpg")))
                .withAspectRatio(1f, 1f)
                .withOptions(options)
                .start(activity, fragment)
    }
}
