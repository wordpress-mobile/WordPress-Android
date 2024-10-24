package org.wordpress.android.fluxc.model

import android.annotation.SuppressLint
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
@SuppressLint("ParcelCreator")
data class DomainContactModel(
    @SerializedName("first_name")
    val firstName: String?,
    @SerializedName("last_name")
    val lastName: String?,
    @SerializedName("organization")
    val organization: String?,
    @SerializedName("address_1")
    val addressLine1: String?,
    @SerializedName("address_2")
    val addressLine2: String?,
    @SerializedName("postal_code")
    val postalCode: String?,
    @SerializedName("city")
    val city: String?,
    @SerializedName("state")
    val state: String?,
    @SerializedName("country_code")
    val countryCode: String?,
    @SerializedName("email")
    val email: String?,
    @SerializedName("phone")
    val phone: String?,
    @SerializedName("fax")
    val fax: String?
) : Parcelable
