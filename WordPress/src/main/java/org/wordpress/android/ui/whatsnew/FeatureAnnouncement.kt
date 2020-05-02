package org.wordpress.android.ui.whatsnew

import androidx.annotation.DrawableRes

data class FeatureAnnouncement(
    val version: String,
    val versionCode: Int,
    val detailsUrl: String,
    val features: List<FeatureAnnouncementItem>
)

data class FeatureAnnouncementItem(
    val title: String,
    val subtitle: String,
    @DrawableRes val iconResId: Int
)
