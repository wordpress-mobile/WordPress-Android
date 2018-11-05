package org.wordpress.android.fluxc.model.stats

data class FollowersModel(
    val totalWpCom: Int,
    val totalEmail: Int,
    val wpComFollowers: List<FollowerModel>,
    val emailFollowers: List<FollowerModel>
) {
    data class FollowerModel(
        val avatar: String,
        val label: String,
        val url: String,
        val dateSubscribed: String
    )
}
