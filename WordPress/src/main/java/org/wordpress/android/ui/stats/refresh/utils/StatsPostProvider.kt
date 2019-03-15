package org.wordpress.android.ui.stats.refresh.utils

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsPostProvider
@Inject constructor() {
    var postId: Long? = null
    var postType: String? = null
    var postTitle: String? = null
    var postUrl: String? = null
    fun init(postId: Long, postType: String, postTitle: String, postUrl: String?) {
        this.postId = postId
        this.postType = postType
        this.postTitle = postTitle
        this.postUrl = postUrl
    }

    fun clear() {
        postId = null
        postType = null
        postTitle = null
        postUrl = null
    }
}
