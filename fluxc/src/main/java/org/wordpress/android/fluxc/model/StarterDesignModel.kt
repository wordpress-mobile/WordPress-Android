package org.wordpress.android.fluxc.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class StarterDesignModel(
    @SerializedName("blog_id") val blogId: Long?,
    val slug: String?,
    val title: String?,
    @SerializedName("site_url") val siteUrl: String?,
    @SerializedName("demo_url") val demoUrl: String?,
    val theme: String?,
    @SerializedName("segment_id") val segmentId: Long?,
    val screenshot: String?,
    @SerializedName("mobile_screenshot") val mobileScreenshot: String?,
    @SerializedName("tablet_screenshot") val tabletScreenshot: String?
) : Parcelable
