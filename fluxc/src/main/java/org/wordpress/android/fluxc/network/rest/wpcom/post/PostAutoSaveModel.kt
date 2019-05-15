package org.wordpress.android.fluxc.network.rest.wpcom.post

import com.google.gson.annotations.SerializedName

data class PostAutoSaveModel(
    @SerializedName("ID") val revisionId: Long?,
    @SerializedName("post_ID") val postId: Long?,
    @SerializedName("modified") val modified: String?,
    @SerializedName("preview_URL") val previewUrl: String?
)
