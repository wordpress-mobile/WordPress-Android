package org.wordpress.android.ui.sitecreation.creation

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class NewSiteCreationServiceData(
    val segmentId: Long,
    val verticalId: Long?,
    val siteTitle: String?,
    val siteTagLine: String?,
    val siteSlug: String
) : Parcelable
