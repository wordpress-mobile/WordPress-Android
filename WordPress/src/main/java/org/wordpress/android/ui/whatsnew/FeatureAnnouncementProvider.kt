package org.wordpress.android.ui.whatsnew

import org.wordpress.android.R
import javax.inject.Inject

class FeatureAnnouncementProvider @Inject constructor() {
    private val localFeatureAnnouncement = FeatureAnnouncement(
            "14.7", "https://wordpress.com/blog/2020/04/20/earth-day-live/", listOf(
            FeatureAnnouncementItem(
                    "Super Publishing",
                    "Publish amazing articles using the power of your mind! Concentrate" +
                            " on what you want to post, and we will do the rest!",
                    R.drawable.ic_posts_white_24dp
            ),
            FeatureAnnouncementItem(
                    "Great cats and superb dogs are right behind you!",
                    "That's right! They are right in the app! They require pets right now." +
                            " Are you going to look for them or what?",
                    R.drawable.ic_plans_white_24dp
            ),
            FeatureAnnouncementItem(
                    "We like long feature announcements that why this one is going to be extra" +
                            " long and span multiple lines",
                    "Here we run out of budget.",
                    R.drawable.ic_themes_white_24dp
            )
    )
    )

    fun getLatestFeatureAnnouncement(): FeatureAnnouncement {
        return localFeatureAnnouncement
    }
}
