package org.wordpress.android.fluxc.model.stats

import java.util.Date

data class InsightsLatestPostModel(
    val siteId: Long,
    val postTitle: String,
    val postURL: String,
    val postDate: Date,
    val postId: Long,
    val dayViews: List<Pair<String, Int>>
)
