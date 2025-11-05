package org.wordpress.android.support.he.model

import android.net.Uri

data class AttachmentState(
    val acceptedUris: List<Uri> = emptyList(),
    val rejectedUris: List<Uri> = emptyList(),
    val rejectionReason: RejectionReason? = null
) {
    sealed class RejectionReason {
        data object FileTooLarge : RejectionReason()
        data object TotalSizeExceeded : RejectionReason()
    }
}
