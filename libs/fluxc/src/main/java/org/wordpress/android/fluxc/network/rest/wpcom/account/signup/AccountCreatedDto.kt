package org.wordpress.android.fluxc.network.rest.wpcom.account.signup

import com.google.gson.annotations.SerializedName

data class AccountCreatedDto(
    @SerializedName("success") val success: Boolean,
    @SerializedName("username") val username: String,
    @SerializedName("user_id") val id: String,
    @SerializedName("bearer_token") val token: String,
    )
