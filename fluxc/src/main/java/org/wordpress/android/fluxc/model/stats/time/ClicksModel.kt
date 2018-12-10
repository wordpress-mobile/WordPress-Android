package org.wordpress.android.fluxc.model.stats.time

data class ClicksModel(val otherClicks: Int, val totalClicks: Int, val groups: List<Group>, val hasMore: Boolean) {
    data class Group(
        val groupId: String?,
        val name: String?,
        val icon: String?,
        val url: String?,
        val views: Int?,
        val clicks: List<Click>
    )
    data class Click(val name: String, val views: Int, val icon: String?, val url: String?)
}
