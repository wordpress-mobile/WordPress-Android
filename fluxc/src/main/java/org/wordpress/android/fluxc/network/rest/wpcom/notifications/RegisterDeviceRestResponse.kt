package org.wordpress.android.fluxc.network.rest.wpcom.notifications

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.network.Response

class RegisterDeviceRestResponse : Response {
    @SerializedName("ID")
    val id: String? = null
}
