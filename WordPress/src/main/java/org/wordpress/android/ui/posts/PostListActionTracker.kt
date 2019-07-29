package org.wordpress.android.ui.posts

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.widgets.PostListButtonType

fun trackPostListAction(site: SiteModel, buttonType: PostListButtonType, postData: PostModel, statsEvent: Stat) {
    val properties = HashMap<String, Any?>()
    if (!postData.isLocalDraft) {
        properties["post_id"] = postData.remotePostId
    }

    properties["action"] = when (buttonType) {
        PostListButtonType.BUTTON_EDIT -> {
            properties[AnalyticsUtils.HAS_GUTENBERG_BLOCKS_KEY] = PostUtils
                    .contentContainsGutenbergBlocks(postData.content)
            "edit"
        }
        PostListButtonType.BUTTON_RETRY -> "retry"
        PostListButtonType.BUTTON_SUBMIT -> "submit"
        PostListButtonType.BUTTON_VIEW -> "view"
        PostListButtonType.BUTTON_PREVIEW -> "preview"
        PostListButtonType.BUTTON_STATS -> "stats"
        PostListButtonType.BUTTON_TRASH -> "trash"
        PostListButtonType.BUTTON_DELETE,
        PostListButtonType.BUTTON_DELETE_PERMANENTLY -> "delete"
        PostListButtonType.BUTTON_PUBLISH -> "publish"
        PostListButtonType.BUTTON_SYNC -> "sync"
        PostListButtonType.BUTTON_MORE -> "more"
        PostListButtonType.BUTTON_MOVE_TO_DRAFT -> "move_to_draft"
    }

    AnalyticsUtils.trackWithSiteDetails(statsEvent, site, properties)
}
