package org.wordpress.android.ui.posts.prepublishing

import org.wordpress.android.ui.posts.PublishPost

interface PrepublishingBottomSheetListener {
    fun onPublishButtonClicked(publishPost: PublishPost)
}
