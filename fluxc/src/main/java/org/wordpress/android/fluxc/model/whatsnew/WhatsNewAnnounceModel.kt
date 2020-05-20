package org.wordpress.android.fluxc.model.whatsnew

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
@SuppressLint("ParcelCreator")
data class WhatsNewAnnounceModel(
    val appVersionName: String,
    val announcementVersion: Int,
    val minimumAppVersionCode: Int,
    val detailsUrl: String,
    val isLocalized: Boolean,
    val responseLocale: String,
    val features: List<Feature>
) : Parcelable {
    // TODO: implement equals and hashcode?
    @Parcelize
    @SuppressLint("ParcelCreator")
    data class Feature(
        val title: String,
        val subtitle: String,
        val iconBase64: String,
        val iconUrl: String
    ) : Parcelable
}
