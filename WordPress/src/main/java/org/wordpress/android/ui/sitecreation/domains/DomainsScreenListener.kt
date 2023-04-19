package org.wordpress.android.ui.sitecreation.domains

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

interface DomainsScreenListener {
    fun onDomainSelected(domain: DomainModel)
}

@Parcelize
data class DomainModel(
    val domainName: String,
    val isFree: Boolean,
    val cost: String,
    val productId: Int,
    val supportsPrivacy: Boolean,
) : Parcelable
