package org.wordpress.android.fluxc.network.common.comments

import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.store.CommentStore.CommentError

data class CommentsApiPayload<T>(
    val response: T? = null
) : Payload<CommentError>() {
    constructor(error: CommentError, response: T? = null) : this(response) {
        this.error = error
    }
}
