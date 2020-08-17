package org.wordpress.android.models.discover

import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderTagList

data class ReaderDiscoverCards(val cards: List<ReaderDiscoverCard>)

sealed class ReaderDiscoverCard {
    data class WelcomeBannerCard(val show: Boolean) : ReaderDiscoverCard()
    data class InterestsYouMayLikeCard(val interests: ReaderTagList) : ReaderDiscoverCard()
    data class ReaderPostCard(val post: ReaderPost) : ReaderDiscoverCard()
}
