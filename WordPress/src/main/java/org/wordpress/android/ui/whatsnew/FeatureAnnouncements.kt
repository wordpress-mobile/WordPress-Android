package org.wordpress.android.ui.whatsnew

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
    val iconUrl: String
)
