package org.wordpress.android.fluxc.network.rest.wpcom.common

class LikeWPComRestResponse {
    inner class LikesWPComRestResponse {
        var likes: List<LikeWPComRestResponse>? = null
    }

    var ID: Long = 0
    var login: String? = null
    var name: String? = null
    var avatar_URL: String? = null
    var primary_blog: String? = null
}
