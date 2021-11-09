package org.wordpress.android.ui.mysite.cards.post

import org.wordpress.android.R
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard.PostItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PostCardBuilderParams
import org.wordpress.android.ui.mysite.cards.post.mockdata.MockedPostsData.Post
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import javax.inject.Inject

class PostCardBuilder @Inject constructor() {
    fun build(params: PostCardBuilderParams) = mutableListOf<PostCard>().apply {
        val posts = params.mockedPostsData?.posts
        posts?.draft?.takeIf { it.isNotEmpty() }?.let { add(it.createDraftPostsCard()) }
        posts?.scheduled?.takeIf { it.isNotEmpty() }?.let { add(it.createScheduledPostsCard()) }
    }

    private fun List<Post>.createDraftPostsCard() = PostCard(
            title = UiStringRes(R.string.my_site_post_card_draft_title),
            postItems = mapToDraftOrScheduledPostItems()
    )

    private fun List<Post>.createScheduledPostsCard() = PostCard(
            title = UiStringRes(R.string.my_site_post_card_scheduled_title),
            postItems = mapToDraftOrScheduledPostItems()
    )

    private fun List<Post>.mapToDraftOrScheduledPostItems() = map { post ->
        PostItem(
                title = post.title?.let { UiStringText(it) } ?: UiStringRes(R.string.my_site_untitled_post),
                excerpt = post.excerpt?.let { UiStringText(it) },
                featuredImageUrl = post.featuredImageUrl
        )
    }
}
