package org.wordpress.android.ui.posts

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemotePreviewLogicHelper @Inject constructor() {
    enum class RemotePreviewType {
        NOT_A_REMOTE_PREVIEW,
        REMOTE_PREVIEW,
        REMOTE_PREVIEW_WITH_REMOTE_AUTO_SAVE
    }
}