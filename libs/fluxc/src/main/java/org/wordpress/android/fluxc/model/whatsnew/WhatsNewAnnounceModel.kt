package org.wordpress.android.fluxc.model.whatsnew

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
@SuppressLint("ParcelCreator")
data class WhatsNewAnnounceModel(
    val appVersionName: String,
    val announceVersion: Int,
    val detailsUrl: String,
    val features: List<Feature>
) : Parcelable {
    // TODO: implement equals and hashcode?
    @Parcelize
    @SuppressLint("ParcelCreator")
    data class Feature(
        val title: String,
        val subtitle: String,
        val iconUrl: String
    ) : Parcelable
}
