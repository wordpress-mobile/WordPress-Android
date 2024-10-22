package org.wordpress.android.fluxc.model.stats.time

data class ReferrersModel(val otherViews: Int, val totalViews: Int, val groups: List<Group>, val hasMore: Boolean) {
    data class Group(
        val groupId: String?,
        val name: String?,
        val icon: String?,
        val url: String?,
        val total: Int?,
        val referrers: List<Referrer>,
        val markedAsSpam: Boolean = false
    )
    data class Referrer(
        val name: String,
        val views: Int,
        val icon: String?,
        val url: String?,
        val markedAsSpam: Boolean = false
    )
}
