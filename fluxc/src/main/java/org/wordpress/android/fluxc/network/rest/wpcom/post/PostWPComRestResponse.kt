package org.wordpress.android.fluxc.network.rest.wpcom.post

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.network.rest.wpcom.post.PostWPComRestResponse.PostMeta.PostData.PostAutoSave
import org.wordpress.android.fluxc.network.rest.wpcom.taxonomy.TermWPComRestResponse

data class PostWPComRestResponse(
    @SerializedName("ID") val remotePostId: Long = 0,
    @SerializedName("site_ID") val remoteSiteId: Long = 0,
    @SerializedName("date") val date: String? = null,
    @SerializedName("modified") val modified: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("URL") val url: String? = null,
    @SerializedName("short_URL") val shortUrl: String? = null,
    @SerializedName("content") val content: String? = null,
    @SerializedName("excerpt") val excerpt: String? = null,
    @SerializedName("slug") val slug: String? = null,
    @SerializedName("guid") val guid: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("sticky") val sticky: Boolean = false,
    @SerializedName("password") val password: String? = null,
    @SerializedName("parent") val parent: PostParent? = null,
    @SerializedName("type") val type: String,
    @SerializedName("featured_image") val featuredImage: String? = null,
    @SerializedName("post_thumbnail") val postThumbnail: PostThumbnail? = null,
    @SerializedName("format") val format: String? = null,
    @SerializedName("geo") val geo: GeoLocation? = null,
    @SerializedName("tags") val tags: Map<String, TermWPComRestResponse>? = null,
    @SerializedName("categories") val categories: Map<String, TermWPComRestResponse>? = null,
    @SerializedName("capabilities") val capabilities: Capabilities? = null,
    @SerializedName("meta") val meta: PostMeta? = null,
    @SerializedName("author") val author: Author? = null
) {
    data class PostsResponse(
        @SerializedName("posts") val posts: List<PostWPComRestResponse>,
        @SerializedName("found") val found: Int
    )

    data class PostThumbnail(
        @SerializedName("ID") val id: Long = 0,
        @SerializedName("URL") val url: String? = null,
        @SerializedName("guid") val guid: String? = null,
        @SerializedName("mime_type") val mimeType: String? = null,
        @SerializedName("width") val width: Int = 0,
        @SerializedName("height") val height: Int = 0
    )

    data class Capabilities(
        @SerializedName("publish_post") val publishPost: Boolean = false,
        @SerializedName("edit_post") val editPost: Boolean = false,
        @SerializedName("delete_post") val deletePost: Boolean = false
    )

    data class PostMeta(@SerializedName("data") val data: PostData? = null) {
        data class PostData(@SerializedName("autosave") val autoSave: PostAutoSave? = null) {
            data class PostAutoSave(
                @SerializedName("ID") var revisionId: Long = 0,
                @SerializedName("modified") var modified: String? = null,
                @SerializedName("preview_URL") var previewUrl: String? = null,
                @SerializedName("title") var title: String? = null,
                @SerializedName("content") var content: String? = null,
                @SerializedName("excerpt") var excerpt: String? = null
            )
        }
    }

    fun getPostAutoSave(): PostAutoSave? {
        return meta?.data?.autoSave
    }

    data class Author(
        @SerializedName("ID") val id: Long = 0,
        @SerializedName("name") val name: String?
    )
}
