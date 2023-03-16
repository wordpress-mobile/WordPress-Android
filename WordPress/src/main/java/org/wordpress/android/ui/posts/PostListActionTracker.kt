package org.wordpress.android.ui.posts

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.widgets.PostListButtonType
import org.wordpress.android.widgets.PostListButtonType.BUTTON_CANCEL_PENDING_AUTO_UPLOAD
import org.wordpress.android.widgets.PostListButtonType.BUTTON_COPY
import org.wordpress.android.widgets.PostListButtonType.BUTTON_COPY_URL
import org.wordpress.android.widgets.PostListButtonType.BUTTON_DELETE
import org.wordpress.android.widgets.PostListButtonType.BUTTON_DELETE_PERMANENTLY
import org.wordpress.android.widgets.PostListButtonType.BUTTON_EDIT
import org.wordpress.android.widgets.PostListButtonType.BUTTON_MORE
import org.wordpress.android.widgets.PostListButtonType.BUTTON_MOVE_TO_DRAFT
import org.wordpress.android.widgets.PostListButtonType.BUTTON_PREVIEW
import org.wordpress.android.widgets.PostListButtonType.BUTTON_PUBLISH
import org.wordpress.android.widgets.PostListButtonType.BUTTON_RETRY
import org.wordpress.android.widgets.PostListButtonType.BUTTON_SHOW_MOVE_TRASHED_POST_TO_DRAFT_DIALOG
import org.wordpress.android.widgets.PostListButtonType.BUTTON_STATS
import org.wordpress.android.widgets.PostListButtonType.BUTTON_SUBMIT
import org.wordpress.android.widgets.PostListButtonType.BUTTON_SYNC
import org.wordpress.android.widgets.PostListButtonType.BUTTON_TRASH
import org.wordpress.android.widgets.PostListButtonType.BUTTON_VIEW
import org.wordpress.android.widgets.PostListButtonType.BUTTON_PROMOTE_WITH_BLAZE

fun trackPostListAction(site: SiteModel, buttonType: PostListButtonType, postData: PostModel, statsEvent: Stat) {
    val properties = HashMap<String, Any?>()
    if (!postData.isLocalDraft) {
        properties["post_id"] = postData.remotePostId
    }

    properties["action"] = when (buttonType) {
        BUTTON_EDIT -> {
            properties[AnalyticsUtils.HAS_GUTENBERG_BLOCKS_KEY] = PostUtils
                .contentContainsGutenbergBlocks(postData.content)
            "edit"
        }
        BUTTON_RETRY -> "retry"
        BUTTON_SUBMIT -> "submit"
        BUTTON_VIEW -> "view"
        BUTTON_PREVIEW -> "preview"
        BUTTON_STATS -> "stats"
        BUTTON_TRASH -> "trash"
        BUTTON_COPY -> "copy"
        BUTTON_DELETE,
        BUTTON_DELETE_PERMANENTLY -> "delete"
        BUTTON_PUBLISH -> "publish"
        BUTTON_SYNC -> "sync"
        BUTTON_MORE -> "more"
        BUTTON_MOVE_TO_DRAFT -> "move_to_draft"
        BUTTON_CANCEL_PENDING_AUTO_UPLOAD -> "cancel_pending_auto_upload"
        BUTTON_SHOW_MOVE_TRASHED_POST_TO_DRAFT_DIALOG -> "show_move_trashed_post_to_draft_post_dialog"
        BUTTON_COPY_URL -> "copy_url"
        BUTTON_PROMOTE_WITH_BLAZE -> "promote_with_blaze"
    }

    AnalyticsUtils.trackWithSiteDetails(statsEvent, site, properties)
}
