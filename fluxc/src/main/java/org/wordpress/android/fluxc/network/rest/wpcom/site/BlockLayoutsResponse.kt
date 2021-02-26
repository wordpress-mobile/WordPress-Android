package org.wordpress.android.fluxc.network.rest.wpcom.site

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.network.Response

data class BlockLayoutsResponse(
    val layouts: List<GutenbergLayout>,
    val categories: List<GutenbergLayoutCategory>
) : Response

@Parcelize
data class GutenbergLayout(
    val slug: String,
    val title: String,
    val preview: String,
    @SerializedName("preview_tablet") val previewTablet: String,
    @SerializedName("preview_mobile") val previewMobile: String,
    val content: String,
    @SerializedName("demo_url") val demoUrl: String,
    val categories: List<GutenbergLayoutCategory>
) : Parcelable

@Parcelize
data class GutenbergLayoutCategory(
    val slug: String,
    val title: String,
    val description: String,
    val emoji: String?
) : Parcelable
