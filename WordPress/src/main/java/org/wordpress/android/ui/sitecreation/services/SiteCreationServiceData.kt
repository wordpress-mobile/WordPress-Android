package org.wordpress.android.ui.sitecreation.services

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
@SuppressLint("ParcelCreator")
data class SiteCreationServiceData(
    val segmentId: Long?,
    val siteDesign: String?,
    val domain: String
) : Parcelable
