package org.wordpress.android.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.Reusable
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.RemotePreviewLogicHelper.RemotePreviewType
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
    fun openPlayStoreLink(context: Context, packageName: String) {
        var intent: Intent? = context.packageManager.getLaunchIntentForPackage(packageName)

        if (intent == null) {
            intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                setPackage("com.android.vending")
            }
        }
        try {
            context.startActivity(intent)
        } catch (exception: ActivityNotFoundException) {
            // No Activity found to handle Intent
        }
    }

    companion object {
        const val JETPACK_PACKAGE_NAME = "com.jetpack.android"
        const val WORDPRESS_PACKAGE_NAME = "org.wordpress.android"
    }
}
