package org.wordpress.android.ui.mysite.cards.post

import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PostCardBuilderParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import javax.inject.Inject

class PostCardBuilder @Inject constructor() {
    fun build(params: PostCardBuilderParams): List<PostCard> {
        val cards = mutableListOf<PostCard>()
        params.mockedPostsData?.posts?.draft?.map {
            cards.add(
                    PostCard(
                            title = UiStringText(DRAFT_TITLE),
                            postTitle = UiStringText(it.title ?: NO_TITLE)
                    )
            )
        }
        params.mockedPostsData?.posts?.scheduled?.map {
            cards.add(
                    PostCard(
                            title = UiStringText(SCHEDULED_TITLE),
                            postTitle = UiStringText(it.title ?: NO_TITLE)
                    )
            )
        }
        return cards
    }

    companion object {
        const val DRAFT_TITLE = "Draft Post"
        const val SCHEDULED_TITLE = "Scheduled Post"
        const val NO_TITLE = "No title"
    }
}
