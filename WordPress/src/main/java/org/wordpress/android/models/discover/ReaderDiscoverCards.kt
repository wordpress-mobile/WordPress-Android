package org.wordpress.android.models.discover

import org.wordpress.android.models.ReaderBlog
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderTagList

data class ReaderDiscoverCards(val cards: List<ReaderDiscoverCard>)

sealed class ReaderDiscoverCard {
    object WelcomeBannerCard : ReaderDiscoverCard()
    data class InterestsYouMayLikeCard(val interests: ReaderTagList) : ReaderDiscoverCard()
    data class ReaderPostCard(val post: ReaderPost) : ReaderDiscoverCard()
    data class ReaderRecommendedBlogsCard(val blogs: List<ReaderBlog>) : ReaderDiscoverCard()
}
