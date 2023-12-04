package org.wordpress.android.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import dagger.Reusable
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.RemotePreviewLogicHelper.RemotePreviewType
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject

/**
 * Injectable wrapper around ActivityLauncher.
 *
 * ActivityLauncher interface is consisted of static methods, which make the client code difficult to test/mock.
 * Main purpose of this wrapper is to make testing easier.
 *
 */
@Reusable
class ActivityLauncherWrapper @Inject constructor() {
    fun showActionableEmptyView(
        context: Context,
        actionableState: WPWebViewUsageCategory,
        postTitle: String
    ) = ActivityLauncher.showActionableEmptyView(context, actionableState, postTitle)

    fun previewPostOrPageForResult(
        activity: Activity,
        site: SiteModel,
        post: PostImmutableModel,
        remotePreviewType: RemotePreviewType
    ) = ActivityLauncher.previewPostOrPageForResult(activity, site, post, remotePreviewType)

    @Suppress("SwallowedException")
    fun openPlayStoreLink(activity: Activity, packageName: String, utmCampaign: String? = null) {
        var intent: Intent? = activity.packageManager.getLaunchIntentForPackage(packageName)
        val isAppAlreadyInstalled = intent != null

        if (intent == null) {
            intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(getPlayStoreUrl(activity.application.packageName, packageName, utmCampaign))
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                setPackage("com.android.vending")
            }
        }
        try {
            activity.startActivity(intent)
            preventBackNavigation(activity, isAppAlreadyInstalled)
        } catch (e: ActivityNotFoundException) {
            // No Google Play Store installed
            Toast.makeText(
                activity,
                    R.string.install_play_store_to_get_jetpack,
                    Toast.LENGTH_LONG
            ).show()
        }
    }

    fun getPlayStoreUrl(appPackageName: String, destinationPackageName: String, utmCampaign: String? = null): String {
        val referrer = utmCampaign?.let {
            val encoded = UrlUtils.urlEncode("$GOOGLE_STORE_UTM_SOURCE=$appPackageName&$GOOGLE_STORE_UTM_CAMPAIGN=$it")
            "$GOOGLE_STORE_REFERRER=$encoded"
        }
        val storeUrl = "$GOOGLE_STORE_URL_APP_DETAILS?$GOOGLE_STORE_URL_APP_ID=$destinationPackageName"
        return referrer?.let { "$storeUrl&$it" } ?: storeUrl
    }

    private fun preventBackNavigation(activity: Activity, shouldPrevent: Boolean) {
        if (shouldPrevent) {
            activity.finishAffinity()
        }
    }

    companion object {
        const val JETPACK_PACKAGE_NAME = "com.jetpack.android"
        const val GOOGLE_STORE_URL_APP_DETAILS = "https://play.google.com/store/apps/details"
        const val GOOGLE_STORE_URL_APP_ID = "id"
        const val GOOGLE_STORE_REFERRER = "referrer"
        const val GOOGLE_STORE_UTM_SOURCE = "utm_source"
        const val GOOGLE_STORE_UTM_CAMPAIGN = "utm_campaign"
        const val CAMPAIGN_SITE_CREATION = "site_creation"
        const val CAMPAIGN_BOTTOM_SHEET = "bottom_sheet"
        const val CAMPAIGN_STATIC_POSTER = "static_poster"
        const val CAMPAIGN_INDIVIDUAL_PLUGIN = "individual_plugin"
        const val CAMPAIGN_JETPACK_OVERLAY = "jetpack_overlay"
    }
}
