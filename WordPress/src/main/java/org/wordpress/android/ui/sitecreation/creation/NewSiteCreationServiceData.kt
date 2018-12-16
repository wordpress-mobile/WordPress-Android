package org.wordpress.android.ui.sitecreation.creation

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
@SuppressLint("ParcelCreator")
data class NewSiteCreationServiceData(
    val segmentId: Long,
    val verticalId: Long?,
    val siteTitle: String?,
    val siteTagLine: String?,
    val siteSlug: String
) : Parcelable
