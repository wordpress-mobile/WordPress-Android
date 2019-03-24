package org.wordpress.android.fluxc.model

import android.annotation.SuppressLint
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
@SuppressLint("ParcelCreator")
class DomainContactModel(
    @SerializedName("first_name")
    val firstName: String?,
    @SerializedName("last_name")
    val lastName: String?,
    val organization: String?,
    @SerializedName("address_1")
    val addressLine1: String?,
    @SerializedName("address_2")
    val addressLine2: String?,
    @SerializedName("postal_code")
    val postalCode: String?,
    val city: String?,
    val state: String?,
    @SerializedName("country_code")
    val countryCode: String?,
    val email: String?,
    val phone: String?,
    val fax: String?
) : Parcelable
