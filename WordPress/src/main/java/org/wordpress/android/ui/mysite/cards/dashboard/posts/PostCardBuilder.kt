package org.wordpress.android.ui.mysite.cards.dashboard.posts

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.dashboard.CardModel.PostsCardModel.PostCardModel
import org.wordpress.android.fluxc.store.dashboard.CardsStore.PostCardError
import org.wordpress.android.fluxc.store.dashboard.CardsStore.PostCardErrorType
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard.PostCardWithPostItems
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard.PostCardWithPostItems.PostItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PostCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PostCardBuilderParams.PostItemClickParams
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.LocaleManagerWrapper
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject

class PostCardBuilder @Inject constructor(
    private val localeManagerWrapper: LocaleManagerWrapper,
    private val appLogWrapper: AppLogWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val siteRepository: SelectedSiteRepository
) {
    fun build(params: PostCardBuilderParams): List<PostCard> {
        val error = params.posts?.error
        return if (error != null) {
            buildPostCardWithError(error)
        } else {
            buildPostCardsWithData(params)
        }
    }

    private fun buildPostCardWithError(error: PostCardError): List<PostCard.Error> {
        error.message?.let { appLogWrapper.e(AppLog.T.MY_SITE_DASHBOARD, "Post Card Error: $it") }
        return if (shouldShowError(error)) listOf(createPostErrorCard()) else emptyList()
    }

    private fun buildPostCardsWithData(params: PostCardBuilderParams) =
        mutableListOf<PostCard>().apply {
            val posts = params.posts
            posts?.draft?.takeIf {
                it.isNotEmpty() && shouldShowCard(PostCardType.DRAFT)
            }?.let { add(it.createDraftPostsCard(params)) }
            posts?.scheduled?.takeIf {
                it.isNotEmpty() && shouldShowCard(PostCardType.SCHEDULED)
            }?.let { add(it.createScheduledPostsCard(params)) }
        }.toList()

    private fun shouldShowCard(postCardType: PostCardType) =
        appPrefsWrapper.getShouldHidePostDashboardCard(siteRepository.getSelectedSite()!!.siteId, postCardType.name)
            .not()
    private fun createPostErrorCard() = PostCard.Error(
        title = UiStringRes(R.string.posts)
    )

    private fun List<PostCardModel>.createDraftPostsCard(params: PostCardBuilderParams) =
        PostCardWithPostItems(
            postCardType = PostCardType.DRAFT,
            title = UiStringRes(R.string.my_site_post_card_draft_title),
            postItems = mapToDraftPostItems(params.onPostItemClick),
            moreMenuResId = R.menu.dashboard_card_draft_posts_menu,
            moreMenuOptions = PostCardWithPostItems.MoreMenuOptions(
                onMoreMenuClick = params.moreMenuClickParams.onMoreMenuClick,
                onHideThisMenuItemClick = params.moreMenuClickParams.onHideThisMenuItemClick,
                onViewPostsMenuItemClick = params.moreMenuClickParams.onViewPostsMenuItemClick
            )
        )

    private fun List<PostCardModel>.createScheduledPostsCard(params: PostCardBuilderParams) =
        PostCardWithPostItems(
            postCardType = PostCardType.SCHEDULED,
            title = UiStringRes(R.string.my_site_post_card_scheduled_title),
            postItems = mapToScheduledPostItems(params.onPostItemClick),
            moreMenuResId = R.menu.dashboard_card_scheduled_posts_menu,
            moreMenuOptions = PostCardWithPostItems.MoreMenuOptions(
                onMoreMenuClick = params.moreMenuClickParams.onMoreMenuClick,
                onHideThisMenuItemClick = params.moreMenuClickParams.onHideThisMenuItemClick,
                onViewPostsMenuItemClick = params.moreMenuClickParams.onViewPostsMenuItemClick
            )
        )

    private fun List<PostCardModel>.mapToDraftPostItems(
        onPostItemClick: (params: PostItemClickParams) -> Unit
    ) = map { post ->
        PostItem(
            title = constructPostTitle(post.title),
            excerpt = constructPostContent(post.content),
            featuredImageUrl = post.featuredImage,
            onClick = ListItemInteraction.create(
                PostItemClickParams(PostCardType.DRAFT, post.id),
                onPostItemClick
            )
        )
    }

    private fun constructPostTitle(title: String) =
        if (title.isEmpty()) UiStringRes(R.string.my_site_untitled_post) else UiStringText(title)

    private fun constructPostContent(content: String) =
        content.takeIf { it.isNotEmpty() }?.let { UiStringText(content) }

    private fun List<PostCardModel>.mapToScheduledPostItems(
        onPostItemClick: (params: PostItemClickParams) -> Unit
    ) = map { post ->
        PostItem(
            title = constructPostTitle(post.title),
            excerpt = UiStringText(constructPostDate(post.date)),
            featuredImageUrl = post.featuredImage,
            isTimeIconVisible = true,
            onClick = ListItemInteraction.create(
                PostItemClickParams(PostCardType.SCHEDULED, post.id),
                onPostItemClick
            )
        )
    }

    private fun constructPostDate(date: Date) =
        SimpleDateFormat(MONTH_DAY_FORMAT, localeManagerWrapper.getLocale()).format(date)

    private fun shouldShowError(error: PostCardError) = error.type == PostCardErrorType.GENERIC_ERROR

    companion object {
        private const val MONTH_DAY_FORMAT = "MMM d"
    }
}
