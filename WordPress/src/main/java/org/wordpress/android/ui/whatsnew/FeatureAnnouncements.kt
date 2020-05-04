package org.wordpress.android.ui.whatsnew

import androidx.annotation.DrawableRes

data class FeatureAnnouncements(val announcements: List<FeatureAnnouncement>)

data class FeatureAnnouncement(
    val version: String,
    val versionCode: Int,
    val detailsUrl: String,
    val features: List<FeatureAnnouncementItem>
)

data class FeatureAnnouncementItem(
    val title: String,
    val subtitle: String,
    val gridiconName: String
)
