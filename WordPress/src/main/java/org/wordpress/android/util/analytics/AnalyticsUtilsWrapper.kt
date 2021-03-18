package org.wordpress.android.util.analytics

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.Reusable
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.util.UriWrapper
import javax.inject.Inject

/**
 * Injectable wrapper around AnalyticsUtils.
 *
 * AnalyticsUtils interface is consisted of static methods, which make the client code difficult to test/mock.
 * Main purpose of this wrapper is to make testing easier.
 */
@Reusable
class AnalyticsUtilsWrapper @Inject constructor(
    private val appContext: Context
) {
    fun getMediaProperties(
        isVideo: Boolean,
        mediaURI: UriWrapper?,
        path: String?
    ): MutableMap<String, Any?> = AnalyticsUtils.getMediaProperties(appContext, isVideo, mediaURI?.uri, path)

    fun trackEditorCreatedPost(
        action: String?,
        intent: Intent,
        site: SiteModel,
        post: PostImmutableModel?
    ) = AnalyticsUtils.trackEditorCreatedPost(action, intent, site, post)

    fun trackWithReaderPostDetails(
        stat: AnalyticsTracker.Stat,
        post: ReaderPost?,
        properties: Map<String, Any?>
    ) = AnalyticsUtils.trackWithReaderPostDetails(stat, post, properties)

    fun trackFollowCommentsWithReaderPostDetails(
        stat: AnalyticsTracker.Stat,
        blogId: Long,
        postId: Long,
        post: ReaderPost?,
        properties: Map<String, Any?>
    ) = AnalyticsUtils.trackFollowCommentsWithReaderPostDetails(stat, blogId, postId, post, properties)

    fun trackInviteLinksAction(
        stat: AnalyticsTracker.Stat,
        site: SiteModel?,
        properties: Map<String, Any?>?
    ) = AnalyticsUtils.trackInviteLinksAction(stat, site, properties)

    fun trackWithDeepLinkData(
        stat: AnalyticsTracker.Stat,
        action: String,
        host: String,
        data: Uri?
    ) = AnalyticsUtils.trackWithDeepLinkData(stat, action, host, data)
}
