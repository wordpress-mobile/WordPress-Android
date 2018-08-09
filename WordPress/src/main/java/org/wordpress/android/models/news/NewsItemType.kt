package org.wordpress.android.models.news

import org.wordpress.android.R.string

enum class NewsItemType(
    val titleResId: Int,
    val contentResId: Int,
    val actionResId: Int,
    val urlResId: Int,
    val version: Int
) {
    // when announcing a new feature change string resources and increment version number
    LOCAL(
            string.news_card_announcement_title_sample_announcement,
            string.news_card_announcement_content_sample_announcement,
            string.news_card_announcement_action_sample_announcement,
            string.news_card_announcement_action_url_sample_announcement,
            -1
    )
}
