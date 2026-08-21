package org.wordpress.android.ui.rs

import org.wordpress.android.fluxc.model.post.PostStatus as FluxCPostStatus
import uniffi.wp_api.PostStatus

/**
 * Translates a status FluxC reported into the wordpress-rs status the lists are built around.
 *
 * The rs lists group posts by [PostStatus], so this is what lets a change that arrived through
 * FluxC - an upload, say - be located in one of them. Returns null for a status no rs list can
 * show.
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
