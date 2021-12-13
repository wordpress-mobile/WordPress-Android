package org.wordpress.android.ui.mysite.cards.post

import org.wordpress.android.R
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard.FooterLink
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard.PostCardWithPostItems
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard.PostCardWithPostItems.PostItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard.PostCardWithoutPostItems
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PostCardBuilderParams
import org.wordpress.android.ui.mysite.cards.post.mockdata.MockedPostsData.Post
import org.wordpress.android.ui.mysite.cards.post.mockdata.MockedPostsData.Posts
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import javax.inject.Inject

class PostCardBuilder @Inject constructor() {
    fun build(params: PostCardBuilderParams): List<PostCard> = mutableListOf<PostCard>().apply {
        val posts = params.mockedPostsData?.posts
        posts?.hasPublishedPosts?.takeIf { !posts.hasDraftsOrScheduledPosts() }
                ?.let { hasPublishedPosts ->
                    if (hasPublishedPosts) {
                        add(createNextPostCard(params.onFooterLinkClick))
                    } else {
                        add(createFirstPostCard(params.onFooterLinkClick))
                    }
                }
        posts?.draft?.takeIf { it.isNotEmpty() }?.let { add(it.createDraftPostsCard(params)) }
        posts?.scheduled?.takeIf { it.isNotEmpty() }?.let { add(it.createScheduledPostsCard(params)) }
    }

    private fun createFirstPostCard(onFooterLinkClick: (postCardType: PostCardType) -> Unit) =
            PostCardWithoutPostItems(
                    postCardType = PostCardType.CREATE_FIRST,
                    title = UiStringRes(R.string.my_site_create_first_post_title),
                    excerpt = UiStringRes(R.string.my_site_create_first_post_excerpt),
                    imageRes = R.drawable.img_write_212dp,
                    footerLink = FooterLink(
                            label = UiStringRes(R.string.my_site_post_card_link_create_post),
                            onClick = onFooterLinkClick
                    )
            )

    private fun createNextPostCard(onFooterLinkClick: (postCardType: PostCardType) -> Unit) =
            PostCardWithoutPostItems(
                    postCardType = PostCardType.CREATE_NEXT,
                    title = UiStringRes(R.string.my_site_create_next_post_title),
                    excerpt = UiStringRes(R.string.my_site_create_next_post_excerpt),
                    imageRes = R.drawable.img_write_212dp,
                    footerLink = FooterLink(
                            label = UiStringRes(R.string.my_site_post_card_link_create_post),
                            onClick = onFooterLinkClick
                    )
            )

    private fun List<Post>.createDraftPostsCard(params: PostCardBuilderParams) =
            PostCardWithPostItems(
                    postCardType = PostCardType.DRAFT,
                    title = UiStringRes(R.string.my_site_post_card_draft_title),
                    postItems = mapToDraftPostItems(params.onPostItemClick),
                    footerLink = FooterLink(
                            label = UiStringRes(R.string.my_site_post_card_link_go_to_drafts),
                            onClick = params.onFooterLinkClick
                    )
            )

    private fun List<Post>.createScheduledPostsCard(params: PostCardBuilderParams) =
            PostCardWithPostItems(
                    postCardType = PostCardType.SCHEDULED,
                    title = UiStringRes(R.string.my_site_post_card_scheduled_title),
                    postItems = mapToScheduledPostItems(params.onPostItemClick),
                    footerLink = FooterLink(
                            label = UiStringRes(R.string.my_site_post_card_link_go_to_scheduled_posts),
                            onClick = params.onFooterLinkClick
                    )
            )

    private fun Posts.hasDraftsOrScheduledPosts() =
            this.draft?.isNotEmpty() == true || this.scheduled?.isNotEmpty() == true

    private fun List<Post>.mapToDraftPostItems(onPostItemClick: (postId: Int) -> Unit) = map { post ->
        PostItem(
                title = post.title?.let { UiStringText(it) } ?: UiStringRes(R.string.my_site_untitled_post),
                excerpt = post.excerpt?.let { UiStringText(it) },
                featuredImageUrl = post.featuredImageUrl,
                onClick = { post.id?.let { onPostItemClick.invoke(it) } }
        )
    }

    private fun List<Post>.mapToScheduledPostItems(onPostItemClick: (postId: Int) -> Unit) = map { post ->
        PostItem(
                title = post.title?.let { UiStringText(it) } ?: UiStringRes(R.string.my_site_untitled_post),
                excerpt = UiStringText("Today at 1:04 PM"), // TODO: ashiagr - remove hardcoded text
                featuredImageUrl = post.featuredImageUrl,
                isTimeIconVisible = true,
                onClick = { post.id?.let { onPostItemClick.invoke(it) } }
        )
    }
}
