package org.wordpress.android.ui.main.feedbackform

import javax.inject.Inject

class FeedbackFormUtils @Inject constructor() {
    /**
     * Only videos, photos, PDFs, plain text, mp3s, JSON, and zip are supported
     */
    fun isSupportedAttachmentType(mimeType: String): Boolean {
        return when {
            mimeType.startsWith("image") -> true
            mimeType.startsWith("video") -> true
            mimeType.endsWith("json") -> true
            mimeType.endsWith("pdf") -> true
            mimeType.endsWith("text") -> true
            mimeType.endsWith("zip") -> true
            mimeType == "audio/mpeg" -> true
            else -> false
        }
    }
}
