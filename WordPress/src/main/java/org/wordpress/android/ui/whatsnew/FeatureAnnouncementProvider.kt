package org.wordpress.android.ui.whatsnew

class FeatureAnnouncementProvider {
    fun getAnnouncementAppVersion(): String {
        return "14.7"
    }

    fun getAnnouncementFeatures(): List<FeatureAnnouncementItem> {
        return listOf<FeatureAnnouncementItem>(
                FeatureAnnouncementItem(
                        "Super Publishing",
                        "Publish amazing articles using the power of your mind! Concentrate" +
                                " on what you want to post, and we will do the rest!",
                        ""
                ),
                FeatureAnnouncementItem(
                        "Great cats and superb dogs are right behind you!",
                        "That's right! They are right in the app! They require pets right now." +
                                " Are you going to look for them or what?",
                        ""
                ),
                FeatureAnnouncementItem(
                        "We like long feature announcements that why this one is going to be extra" +
                                " long and span multiple lines",
                        "Here we run out of budget.",
                        ""
                )
        )
    }

    fun getAnnouncementDetailsUrl(): String {
        return "https://wordpress.com/blog/2020/04/20/earth-day-live/"
    }
}
