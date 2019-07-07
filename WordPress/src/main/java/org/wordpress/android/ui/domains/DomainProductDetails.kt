package org.wordpress.android.ui.domains

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class DomainProductDetails(
    val productId: Int,
    val domainName: String
) : Parcelable
