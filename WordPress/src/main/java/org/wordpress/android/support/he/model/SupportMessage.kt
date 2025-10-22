package org.wordpress.android.support.he.model

import java.util.Date

data class SupportMessage(
    val id: Long,
    val text: String,
    val createdAt: Date,
    val authorName: String,
    val authorIsUser: Boolean
)
