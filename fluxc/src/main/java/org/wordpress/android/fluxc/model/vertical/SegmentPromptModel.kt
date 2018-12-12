package org.wordpress.android.fluxc.model.vertical

import com.google.gson.annotations.SerializedName

data class SegmentPromptModel(
    @SerializedName("site_topic_header") val title: String,
    @SerializedName("site_topic_subheader") val subtitle: String,
    @SerializedName("site_topic_placeholder") val hint: String
)
