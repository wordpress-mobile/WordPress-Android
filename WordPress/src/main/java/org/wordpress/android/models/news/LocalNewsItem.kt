package org.wordpress.android.models.news

import org.wordpress.android.R.string

/**
 * Hardcoded News item. This is a temporary solution, we'll load data from an API endpoint, as soon as it's implemented.
 *
 * When announcing a new feature, create new string resources and update fields of this object.
 *
 * Don't forget to increment the version number, otherwise some users might not see the new announcement.
 */
object LocalNewsItem {
    val titleResId: Int = string.news_card_announcement_title_sample_announcement
    val contentResId: Int = string.news_card_announcement_content_sample_announcement
    val actionResId: Int = string.news_card_announcement_action_sample_announcement
    val urlResId: Int = string.news_card_announcement_action_url_sample_announcement
    val version: Int = -1
}
