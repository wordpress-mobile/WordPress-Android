package org.wordpress.android.fluxc.network.rest.wpcom.common

@Suppress("VariableNaming")
class LikeWPComRestResponse {
    inner class LikesWPComRestResponse {
        var likes: List<LikeWPComRestResponse>? = null
    }

    inner class PreferredBlogResponse {
        var id: Long = 0
        var name: String? = null
        var url: String? = null
        var icon: PreferredBlogIcon? = null
    }

    inner class PreferredBlogIcon {
        var ico: String? = null
        var img: String? = null
    }

    var ID: Long = 0
    var login: String? = null
    var name: String? = null
    var avatar_URL: String? = null
    var bio: String? = null
    var site_ID: Long = 0
    var primary_blog: String? = null
    var date_liked: String? = null
    var preferred_blog: PreferredBlogResponse? = null
}
