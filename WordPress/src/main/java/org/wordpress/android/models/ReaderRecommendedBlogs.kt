package org.wordpress.android.models

import com.google.gson.annotations.SerializedName

data class ReaderCardRecommendedBlog(
    @SerializedName("name") val name: String,
    @SerializedName("url") val url: String,
    @SerializedName("blog_id") val blogId: Long,
    @SerializedName("description") val description: String
)
