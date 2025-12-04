package org.wordpress.android.support.he.model

import org.wordpress.android.support.he.ui.SupportCategory

data class NewTicketFormState(
    val category: SupportCategory? = null,
    val subject: String = "",
    val siteAddress: String = "",
    val message: String = "",
    val includeAppLogs: Boolean = false,
    val attachmentState: AttachmentState = AttachmentState(),
)
