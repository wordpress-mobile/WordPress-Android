package org.wordpress.android.fluxc.model.stats

import java.util.Date

data class FollowersModel(
    val totalCount: Int,
    val followers: List<FollowerModel>,
    val hasMore: Boolean
) {
    data class FollowerModel(
        val avatar: String,
        val label: String,
        val url: String?,
        val dateSubscribed: Date
    )
}
