package org.wordpress.android.support.unified.model

data class ConversationReplyFormState(
    val message: String = "",
    val includeAppLogs: Boolean = false,
    val attachmentState: AttachmentState = AttachmentState(),
    val isBottomSheetVisible: Boolean = false,
)
