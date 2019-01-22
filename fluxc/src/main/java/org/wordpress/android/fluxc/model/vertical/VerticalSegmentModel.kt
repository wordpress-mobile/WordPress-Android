package org.wordpress.android.fluxc.model.vertical

import com.google.gson.annotations.SerializedName

data class VerticalSegmentModel(
    @SerializedName("segment_type_title") val title: String,
    @SerializedName("segment_type_subtitle") val subtitle: String,
    @SerializedName("icon_URL") val iconUrl: String,
    @SerializedName("icon_color") val iconColor: String,
    @SerializedName("id") val segmentId: Long,
    @SerializedName("mobile") val isMobileSegment: Boolean
)
