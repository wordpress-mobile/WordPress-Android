package org.wordpress.android.models.news

import org.wordpress.android.R

/**
 * Hardcoded News item. This is a temporary solution, we'll load data from an API endpoint, as soon as it's implemented.
 *
 * When announcing a new feature, create new string resources and update fields of this object.
 *
 * Don't forget to increment the version number, otherwise some users might not see the new announcement.
 */
object LocalNewsItem {
    const val titleResId: Int = R.string.news_card_announcement_title_sample_announcement
    const val contentResId: Int = R.string.news_card_announcement_content_sample_announcement
    const val actionResId: Int = R.string.news_card_announcement_action_sample_announcement
    const val urlResId: Int = R.string.news_card_announcement_action_url_sample_announcement
    const val version: Int = -1
}
