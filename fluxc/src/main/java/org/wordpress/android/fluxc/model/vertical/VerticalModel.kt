package org.wordpress.android.fluxc.model.vertical

import com.google.gson.annotations.SerializedName

data class VerticalModel(
    @SerializedName("vertical_name") val name: String,
    @SerializedName("vertical_id") val verticalId: String,
    @SerializedName("vertical_slug") val verticalSlug: String,
    @SerializedName("is_user_input_vertical") val isUserInputVertical: Boolean
)
