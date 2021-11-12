package org.wordpress.android.ui.mysite.cards.post

import org.wordpress.android.R
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard.PostCardCreateFirst
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard.PostCardDraftOrScheduled
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard.PostCardDraftOrScheduled.PostItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PostCardBuilderParams
import org.wordpress.android.ui.mysite.cards.post.mockdata.MockedPostsData.Post
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import javax.inject.Inject

class PostCardBuilder @Inject constructor() {
    fun build(params: PostCardBuilderParams): List<PostCard> = mutableListOf<PostCard>().apply {
        val posts = params.mockedPostsData?.posts
        posts?.hasPublishedPosts?.takeIf { it }?.let { add(createFirstPostCard()) }
        posts?.draft?.takeIf { it.isNotEmpty() }?.let { add(it.createDraftPostsCard()) }
        posts?.scheduled?.takeIf { it.isNotEmpty() }?.let { add(it.createScheduledPostsCard()) }
    }

    private fun createFirstPostCard() = PostCardCreateFirst(
            postCardType = PostCardType.CREATE_FIRST,
            title = UiStringRes(R.string.my_site_create_first_post_title),
            excerpt = UiStringRes(R.string.my_site_create_first_post_excerpt),
            imageRes = R.drawable.create_first_temp // TODO: ashiagr replace with actual resource
    )

    private fun List<Post>.createDraftPostsCard() = PostCardDraftOrScheduled(
            postCardType = PostCardType.DRAFT,
            title = UiStringRes(R.string.my_site_post_card_draft_title),
            postItems = mapToDraftOrScheduledPostItems(PostCardType.DRAFT)
    )

    private fun List<Post>.createScheduledPostsCard() = PostCardDraftOrScheduled(
            postCardType = PostCardType.SCHEDULED,
            title = UiStringRes(R.string.my_site_post_card_scheduled_title),
            postItems = mapToDraftOrScheduledPostItems(PostCardType.SCHEDULED)
    )

    private fun List<Post>.mapToDraftOrScheduledPostItems(postCardType: PostCardType) = map { post ->
        val excerpt = if (postCardType == PostCardType.SCHEDULED) {
            "Today at 1:04 PM" // TODO: ashiagr - remove hardcoded text
        } else {
            post.excerpt
        }
        val isTimeIconVisible = postCardType == PostCardType.SCHEDULED && excerpt != null

        PostItem(
                title = post.title?.let { UiStringText(it) } ?: UiStringRes(R.string.my_site_untitled_post),
                excerpt = excerpt?.let { UiStringText(it) },
                featuredImageUrl = post.featuredImageUrl,
                isTimeIconVisible = isTimeIconVisible
        )
    }
}
