package org.wordpress.android.ui.sitecreation.services

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
@SuppressLint("ParcelCreator")
data class NewSiteCreationServiceData(
    val segmentId: Long?,
    val verticalId: String?,
    val siteTitle: String?,
    val siteTagLine: String?,
    val domain: String
) : Parcelable
