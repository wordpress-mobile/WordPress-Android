package org.wordpress.android.ui.mysite.cards.dashboard.posts

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.dashboard.CardModel.PostsCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PostsCardModel.PostCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard.FooterLink
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard.PostCardWithPostItems
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard.PostCardWithPostItems.PostItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard.PostCardWithoutPostItems
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PostCardBuilderParams
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.LocaleManagerWrapper
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject

@Suppress("TooManyFunctions")
class PostCardBuilder @Inject constructor(
    private val localeManagerWrapper: LocaleManagerWrapper
) {
    fun build(params: PostCardBuilderParams): List<PostCard> = mutableListOf<PostCard>().apply {
        val posts = params.posts
        posts?.hasPublished?.takeIf { !posts.hasDraftsOrScheduledPosts() }
                ?.let { hasPublished ->
                    if (hasPublished) {
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

    private fun List<PostCardModel>.createDraftPostsCard(params: PostCardBuilderParams) =
            PostCardWithPostItems(
                    postCardType = PostCardType.DRAFT,
                    title = UiStringRes(R.string.my_site_post_card_draft_title),
                    postItems = mapToDraftPostItems(params.onPostItemClick),
                    footerLink = FooterLink(
                            label = UiStringRes(R.string.my_site_post_card_link_go_to_drafts),
                            onClick = params.onFooterLinkClick
                    )
            )

    private fun List<PostCardModel>.createScheduledPostsCard(params: PostCardBuilderParams) =
            PostCardWithPostItems(
                    postCardType = PostCardType.SCHEDULED,
                    title = UiStringRes(R.string.my_site_post_card_scheduled_title),
                    postItems = mapToScheduledPostItems(params.onPostItemClick),
                    footerLink = FooterLink(
                            label = UiStringRes(R.string.my_site_post_card_link_go_to_scheduled_posts),
                            onClick = params.onFooterLinkClick
                    )
            )

    private fun PostsCardModel.hasDraftsOrScheduledPosts() = draft.isNotEmpty() || scheduled.isNotEmpty()

    private fun List<PostCardModel>.mapToDraftPostItems(onPostItemClick: (postId: Int) -> Unit) = map { post ->
        PostItem(
                title = constructPostTitle(post.title),
                excerpt = constructPostContent(post.content),
                featuredImageUrl = post.featuredImage,
                onClick = ListItemInteraction.create(post.id, onPostItemClick)
        )
    }

    private fun constructPostTitle(title: String) =
            if (title.isEmpty()) UiStringRes(R.string.my_site_untitled_post) else UiStringText(title)

    private fun constructPostContent(content: String) =
            if (content.isEmpty()) UiStringRes(R.string.my_site_no_content_post) else UiStringText(content)

    private fun List<PostCardModel>.mapToScheduledPostItems(onPostItemClick: (postId: Int) -> Unit) = map { post ->
        PostItem(
                title = constructPostTitle(post.title),
                excerpt = UiStringText(constructPostDate(post.date)),
                featuredImageUrl = post.featuredImage,
                isTimeIconVisible = true,
                onClick = ListItemInteraction.create(post.id, onPostItemClick)
        )
    }

    private fun constructPostDate(date: Date) =
            SimpleDateFormat(MONTH_DAY_FORMAT, localeManagerWrapper.getLocale()).format(date)

    companion object {
        private const val MONTH_DAY_FORMAT = "MMM d"
    }
}
