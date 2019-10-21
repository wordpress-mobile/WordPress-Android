package org.wordpress.android.util.analytics

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.analytics.AnalyticsUtils.QuickActionTrackPropertyValue
import javax.inject.Inject

class AnalyticsUtilsWrapper
@Inject constructor() {
    fun trackWithBlogPostDetails(
        stat: Stat,
        siteId: Long,
        postId: Long
    ) = AnalyticsUtils.trackWithBlogPostDetails(stat, siteId, postId)

    fun trackQuickActionTouched(
        type: QuickActionTrackPropertyValue,
        site: SiteModel,
        comment: CommentModel
    ) = AnalyticsUtils.trackQuickActionTouched(type, site, comment)

    fun trackCommentReplyWithDetails(
        isQuickReply: Boolean,
        site: SiteModel,
        comment: CommentModel
    ) = AnalyticsUtils.trackCommentReplyWithDetails(isQuickReply, site, comment)
}
