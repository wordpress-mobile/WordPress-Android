package org.wordpress.android.ui.posts.prepublishing.listeners

import org.wordpress.android.ui.posts.prepublishing.home.PublishPost

interface PrepublishingBottomSheetListener {
    fun onSubmitButtonClicked(publishPost: PublishPost)
}
