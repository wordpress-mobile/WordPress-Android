package org.wordpress.android.ui

import android.app.Activity
import android.content.Context
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
}
