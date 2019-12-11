package org.wordpress.android.ui.posts

import org.wordpress.android.fluxc.model.PostModel

sealed class PostInfoType {
    object PostNoInfo : PostInfoType()

    data class PostInfo(
        val post: PostModel,
        val hasError: Boolean
    ) : PostInfoType()
}
