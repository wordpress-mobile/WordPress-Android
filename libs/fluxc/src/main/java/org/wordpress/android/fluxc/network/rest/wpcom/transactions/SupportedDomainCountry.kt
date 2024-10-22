package org.wordpress.android.fluxc.network.rest.wpcom.transactions

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SupportedDomainCountry(
    val code: String,
    val name: String
) : Parcelable
