package org.wordpress.android.ui.whatsnew

data class FeatureAnnouncement(
    val version: String,
    val detailsUrl: String,
    val features: List<FeatureAnnouncementItem>
)

data class FeatureAnnouncementItem(
    val announcementTitle: String,
    val announcementSubtitle: String,
    val announcementIconUrl: String
)
