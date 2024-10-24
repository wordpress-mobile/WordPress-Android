package org.wordpress.android.fluxc.network.rest.wpcom.theme

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.network.Response

data class StarterDesignsResponse(
    val designs: List<StarterDesign>,
    val categories: List<StarterDesignCategory>
) : Response

@Parcelize
data class StarterDesign(
    val slug: String,
    val title: String,
    @SerializedName("segment_id") val segmentId: Long?,
    val categories: List<StarterDesignCategory>,
    @SerializedName("demo_url") val demoUrl: String,
    val theme: String?,
    val group: List<String>,
    val preview: String,
    @SerializedName("preview_tablet") val previewTablet: String,
    @SerializedName("preview_mobile") val previewMobile: String
) : Parcelable

@Parcelize
data class StarterDesignCategory(
    val slug: String,
    val title: String,
    val description: String,
    val emoji: String?
) : Parcelable
