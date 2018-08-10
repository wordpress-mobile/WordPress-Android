package org.wordpress.android.models.news

/**
 * Data Class for the News Card - card used for announcing new features/updates.
 */
data class NewsItem(
    val title: String,
    val content: String,
    val actionText: String,
    val actionUrl: String,
    val version: Int
)
