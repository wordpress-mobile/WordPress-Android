package org.wordpress.android.util.analytics

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.Reusable
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.analytics.AnalyticsUtils.AnalyticsCommentActionSource
import org.wordpress.android.util.analytics.AnalyticsUtils.RecommendAppSource
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

    fun trackInviteLinksAction(
        stat: Stat,
        site: SiteModel?,
        properties: Map<String, Any?>?
    ) = AnalyticsUtils.trackInviteLinksAction(stat, site, properties)

    fun trackUserProfileShown(source: String) = AnalyticsUtils.trackUserProfileShown(source)

    fun trackUserProfileSiteShown() = AnalyticsUtils.trackUserProfileSiteShown()

    fun trackBlogPreviewedByUrl(source: String) = AnalyticsUtils.trackBlogPreviewedByUrl(source)

    fun trackLikeListOpened(source: String, listType: String) = AnalyticsUtils.trackLikeListOpened(source, listType)

    fun trackLikeListFetchedMore(
        source: String,
        listType: String,
        nextPage: Int,
        perPage: Int
    ) = AnalyticsUtils.trackLikeListFetchedMore(source, listType, nextPage, perPage)

    /* READER */

    fun trackWithReaderPostDetails(
        stat: Stat,
        post: ReaderPost?,
        properties: Map<String, Any?>
    ) = AnalyticsUtils.trackWithReaderPostDetails(stat, post, properties)

    fun trackFollowCommentsWithReaderPostDetails(
        stat: Stat,
        blogId: Long,
        postId: Long,
        post: ReaderPost?,
        properties: Map<String, Any?>
    ) = AnalyticsUtils.trackFollowCommentsWithReaderPostDetails(stat, blogId, postId, post, properties)

    fun trackWithDeepLinkData(
        stat: Stat,
        action: String,
        host: String,
        data: Uri?
    ) = AnalyticsUtils.trackWithDeepLinkData(stat, action, host, data)

    @Suppress("LongParameterList")
    fun trackWithDeepLinkData(
        stat: Stat,
        action: String,
        host: String,
        source: String,
        url: String,
        sourceInfo: String?
    ) = AnalyticsUtils.trackWithDeepLinkData(
            stat,
            action,
            host,
            source,
            url,
            sourceInfo
    )

    fun trackRailcarRender(
        railcarJson: String
    ) = AnalyticsUtils.trackRailcarRender(railcarJson)

    fun trackWithBlogPostDetails(stat: Stat, blogId: Long, postId: Long) =
            AnalyticsUtils.trackWithBlogPostDetails(stat, blogId, postId)

    fun trackRecommendAppEngaged(source: RecommendAppSource) = AnalyticsUtils.trackRecommendAppEngaged(source)

    fun trackRecommendAppFetchFailed(source: RecommendAppSource, error: String) =
            AnalyticsUtils.trackRecommendAppFetchFailed(source, error)

    fun trackCommentActionWithSiteDetails(
        stat: Stat,
        actionSource: AnalyticsCommentActionSource,
        site: SiteModel
    ) = AnalyticsUtils.trackCommentActionWithSiteDetails(stat, actionSource, site)
}
