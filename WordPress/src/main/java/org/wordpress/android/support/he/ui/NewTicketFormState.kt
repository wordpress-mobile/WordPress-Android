package org.wordpress.android.support.he.ui

data class NewTicketFormState(
    val category: SupportCategory? = null,
    val subject: String = "",
    val siteAddress: String = "",
    val message: String = "",
    val includeAppLogs: Boolean = false,
)
