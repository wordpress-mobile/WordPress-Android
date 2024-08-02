package org.wordpress.android.ui.main.feedbackform

import javax.inject.Inject

class FeedbackFormUtils @Inject constructor() {
    /**
     * Only images & photos are supported at this point
     */
    fun isSupportedMimeType(mimeType: String): Boolean {
        return when {
            mimeType.startsWith("image") -> true
            mimeType.startsWith("video") -> true
            else -> false
        }
    }
}
