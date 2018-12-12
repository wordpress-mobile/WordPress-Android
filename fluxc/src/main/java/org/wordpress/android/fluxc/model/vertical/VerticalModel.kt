package org.wordpress.android.fluxc.model.vertical

import com.google.gson.annotations.SerializedName

data class VerticalModel(
    @SerializedName("vertical_name") val name: String,
    @SerializedName("vertical_id") val verticalId: String,
    // TODO: What should this be?
    @SerializedName("TODO") val isNewUserVertical: Boolean
)
