package org.wordpress.android.ui.mysite.cards.post.mockdata

import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * This is a temporary mocked data class for site dashboard post-based cards
 */
data class MockedPostsData(
    @SerializedName("posts") val posts: Posts? = null
) {
    data class Posts(
        @SerializedName("has_published_posts") val hasPublishedPosts: Boolean? = null,
        @SerializedName("draft") val draft: List<Post>? = null,
        @SerializedName("scheduled") val scheduled: List<Post>? = null
    )
    data class Post(
        @SerializedName("id") val id: Int? = null,
        @SerializedName("title") val title: String? = null,
        @SerializedName("excerpt") val excerpt: String? = null,
        @SerializedName("modified") val modified: Date? = null,
        @SerializedName("featured_image_url") val featuredImageUrl: String? = null
    )
}
