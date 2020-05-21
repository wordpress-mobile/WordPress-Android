package org.wordpress.android.ui.posts.prepublishing

import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId

import org.wordpress.android.ui.posts.PublishPost

interface PrepublishingBottomSheetListener {
    fun onSubmitButtonClicked(postId: LocalId, publishPost: PublishPost)
}
