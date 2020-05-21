package org.wordpress.android.ui.posts.prepublishing

import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId

interface PrepublishingBottomSheetListener {
    fun onPublishButtonClicked(postId: LocalId)
}
