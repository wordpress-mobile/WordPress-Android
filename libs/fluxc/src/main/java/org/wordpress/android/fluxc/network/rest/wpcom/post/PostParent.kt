package org.wordpress.android.fluxc.network.rest.wpcom.post

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.network.rest.JsonObjectOrFalse

data class PostParent(
    @SerializedName("ID") val id: Long = 0,
    @SerializedName("type") val type: String? = null,
    @SerializedName("link") val link: String? = null,
    @SerializedName("title") val title: String? = null
) : JsonObjectOrFalse()

