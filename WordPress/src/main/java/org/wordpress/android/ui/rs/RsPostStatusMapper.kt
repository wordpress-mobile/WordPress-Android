package org.wordpress.android.ui.rs

import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.post.PostStatus as FluxCPostStatus
import uniffi.wp_api.PostStatus

/**
 * Translates a status FluxC reported into the wordpress-rs status the lists group by, so a change
 * that arrived through FluxC can be located in one of them. Null for a status no rs list shows.
 */
fun FluxCPostStatus.toRsPostStatus(): PostStatus? = when (this) {
    FluxCPostStatus.PUBLISHED -> PostStatus.Publish
    FluxCPostStatus.DRAFT -> PostStatus.Draft
    FluxCPostStatus.PRIVATE -> PostStatus.Private
    FluxCPostStatus.PENDING -> PostStatus.Pending
    FluxCPostStatus.TRASHED -> PostStatus.Trash
    FluxCPostStatus.SCHEDULED -> PostStatus.Future
    FluxCPostStatus.UNKNOWN -> null
}

/** The status label the rs lists and the settings screen show against a post or page. */
@StringRes
internal fun PostStatus?.toLabel(): Int = when (this) {
    is PostStatus.Publish ->
        R.string.post_status_post_published
    is PostStatus.Draft -> R.string.post_status_draft
    is PostStatus.Pending ->
        R.string.post_status_pending_review
    is PostStatus.Private ->
        R.string.post_status_post_private
    is PostStatus.Future ->
        R.string.post_status_post_scheduled
    is PostStatus.Trash ->
        R.string.post_status_post_trashed
    is PostStatus.Any -> 0
    is PostStatus.Custom -> 0
    null -> 0
}
