package org.wordpress.android.ui.mysite.cards.dashboard.posts

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.dashboard.CardModel.PostsCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PostsCardModel.PostCardModel
import org.wordpress.android.fluxc.store.dashboard.CardsStore.PostCardError
import org.wordpress.android.fluxc.store.dashboard.CardsStore.PostCardErrorType
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard.FooterLink
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard.PostCardWithPostItems
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard.PostCardWithoutPostItems
import org.wordpress.android.ui.mysite.MySiteCardAndItem.DashboardCardType
import org.wordpress.android.ui.mysite.MySiteCardAndItem.DashboardCardType.POST_CARD_ERROR
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PostCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PostCardBuilderParams.PostItemClickParams
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardBuilder.Companion.NOT_SET
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardType.CREATE_FIRST
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.LocaleManagerWrapper
import java.text.SimpleDateFormat
import java.util.Locale

private const val POST_ID = 1
private const val POST_TITLE = "title"
private const val POST_CONTENT = "content"
private const val FEATURED_IMAGE_URL = "featuredImage"
private val POST_DATE = SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2021-12-06 12:34:56")!!

// This class contains placeholder tests until mock data is removed
@ExperimentalCoroutinesApi
class PostCardBuilderTest : BaseUnitTest() {
    @Mock private lateinit var localeManagerWrapper: LocaleManagerWrapper
    @Mock private lateinit var appLogWrapper: AppLogWrapper

    private lateinit var builder: PostCardBuilder
    private val post = PostCardModel(
            id = POST_ID,
            title = POST_TITLE,
            content = POST_CONTENT,
            featuredImage = FEATURED_IMAGE_URL,
            date = POST_DATE
    )

    private val onPostCardFooterLinkClick: (PostCardType) -> Unit = { }
    private val onPostItemClick: (params: PostItemClickParams) -> Unit = { }

    @Before
    fun setUp() {
        builder = PostCardBuilder(localeManagerWrapper, appLogWrapper)
        setUpMocks()
    }

    private fun setUpMocks() {
        whenever(localeManagerWrapper.getLocale()).thenReturn(Locale.US)
    }

    /* POST CARD ERROR */

    @Test
    fun `given post unauth error, when card is built, then posts card not exist`() {
        val posts = PostsCardModel(error = PostCardError(PostCardErrorType.UNAUTHORIZED))

        val postsCard = buildPostsCard(posts)

        assertThat(postsCard).isEmpty()
    }

    @Test
    fun `given post generic error, when card is built, then error card exists`() {
        val posts = PostsCardModel(error = PostCardError(PostCardErrorType.GENERIC_ERROR))

        val postsCard = buildPostsCard(posts)

        assertThat(postsCard.filterPostErrorCard()).isInstanceOf(PostCard.Error::class.java)
    }

    /* CREATE FIRST POST CARD */

    @Test
    fun `given no published post without draft + sched post, when card is built, then create first card exists`() {
        val posts = getPosts(hasPublished = false)

        val postsCard = buildPostsCard(posts)

        assertThat(postsCard.filterCreateFirstPostCard()).isNotNull
    }

    @Test
    fun `given published post exists with draft post, when card is built, then create first card not exists`() {
        val posts = getPosts(hasPublished = true, draftPosts = listOf(post))

        val postsCard = buildPostsCard(posts)

        assertThat(postsCard.filterCreateFirstPostCard()).isNull()
    }

    @Test
    fun `given published post exists with scheduled post, when card is built, then create first card not exists`() {
        val posts = getPosts(hasPublished = true, scheduledPosts = listOf(post))

        val postsCard = buildPostsCard(posts)

        assertThat(postsCard.filterCreateFirstPostCard()).isNull()
    }

    @Test
    fun `given published post present, when card is built, then create first post card not exists`() {
        val posts = getPosts(hasPublished = true)

        val postsCard = buildPostsCard(posts)

        assertThat(postsCard.filterCreateFirstPostCard()).isNull()
    }

    @Test
    fun `given create first post, when card is built, then it contains correct preset elements`() {
        val posts = getPosts(hasPublished = false)

        val postsCard = buildPostsCard(posts).filterCreateFirstPostCard()

        assertThat(postsCard).isEqualTo(
                PostCardWithoutPostItems(
                        postCardType = PostCardType.CREATE_FIRST,
                        title = UiStringRes(R.string.my_site_create_first_post_title),
                        excerpt = UiStringRes(R.string.my_site_create_first_post_excerpt),
                        imageRes = R.drawable.img_write_212dp,
                        footerLink = FooterLink(
                                label = UiStringRes(R.string.my_site_post_card_link_create_post),
                                onClick = onPostCardFooterLinkClick
                        ),
                        onClick = ListItemInteraction.create(
                                PostItemClickParams(postCardType = CREATE_FIRST, postId = NOT_SET),
                                onPostItemClick
                        )
                )
        )
    }

    /* CREATE NEXT POST CARD */

    @Test
    fun `given published post without draft + sched post, when card is built, then create next card exists`() {
        val posts = getPosts(hasPublished = true)

        val postsCard = buildPostsCard(posts)

        assertThat(postsCard.filterCreateNextPostCard()).isNotNull
    }

    @Test
    fun `given no published post with draft post, when card is built, then create next card not exists`() {
        val posts = getPosts(hasPublished = false, draftPosts = listOf(post))

        val postsCard = buildPostsCard(posts)

        assertThat(postsCard.filterCreateNextPostCard()).isNull()
    }

    @Test
    fun `given no published post with scheduled post, when card is built, then create next card not exists`() {
        val posts = getPosts(hasPublished = false, scheduledPosts = listOf(post))

        val postsCard = buildPostsCard(posts)

        assertThat(postsCard.filterCreateNextPostCard()).isNull()
    }

    @Test
    fun `given published post not present, when card is built, then create next card not exists`() {
        val posts = getPosts(hasPublished = false)

        val postsCard = buildPostsCard(posts)

        assertThat(postsCard.filterCreateNextPostCard()).isNull()
    }

    @Test
    fun `given create next post, when card is built, then it contains correct preset elements`() {
        val posts = getPosts(hasPublished = true)

        val postsCard = buildPostsCard(posts).filterCreateNextPostCard()

        assertThat(postsCard).isEqualTo(
                PostCardWithoutPostItems(
                        postCardType = PostCardType.CREATE_NEXT,
                        title = UiStringRes(R.string.my_site_create_next_post_title),
                        excerpt = UiStringRes(R.string.my_site_create_next_post_excerpt),
                        imageRes = R.drawable.img_write_212dp,
                        footerLink = FooterLink(
                                label = UiStringRes(R.string.my_site_post_card_link_create_post),
                                onClick = onPostCardFooterLinkClick
                        ),
                        ListItemInteraction.create(
                                PostItemClickParams(postCardType = PostCardType.CREATE_NEXT, postId = NOT_SET),
                                onPostItemClick
                        )
                )
        )
    }

    /* DRAFT POST CARD */

    @Test
    fun `given draft post, when card is built, then draft post card exists`() {
        val posts = getPosts(draftPosts = listOf(post))

        val postsCard = buildPostsCard(posts)

        assertThat(postsCard.filterDraftPostCard()).isNotNull
    }

    @Test
    fun `given no draft post, when card is built, then draft post card not exists`() {
        val posts = getPosts(draftPosts = emptyList())

        val postsCard = buildPostsCard(posts)

        assertThat(postsCard.filterDraftPostCard()).isNull()
    }

    @Test
    fun `given draft post, when card is built, then it contains go to drafts link`() {
        val posts = getPosts(draftPosts = listOf(post))

        val postsCard = buildPostsCard(posts).filterDraftPostCard()

        assertThat(postsCard?.footerLink?.label)
                .isEqualTo(UiStringRes(R.string.my_site_post_card_link_go_to_drafts))
    }

    /* SCHEDULED POST CARD */

    @Test
    fun `given scheduled post, when card is built, then scheduled post card exists`() {
        val posts = getPosts(scheduledPosts = listOf(post))

        val postsCard = buildPostsCard(posts)

        assertThat(postsCard.filterScheduledPostCard()).isNotNull
    }

    @Test
    fun `given no scheduled post, when card is built, then scheduled post card not exists`() {
        val posts = getPosts(scheduledPosts = emptyList())

        val postsCard = buildPostsCard(posts)

        assertThat(postsCard.filterScheduledPostCard()).isNull()
    }

    @Test
    fun `given scheduled post, when card is built, then it contains go to scheduled posts link`() {
        val posts = getPosts(scheduledPosts = listOf(post))

        val postsCard = buildPostsCard(posts).filterScheduledPostCard()

        assertThat(postsCard?.footerLink?.label)
                .isEqualTo(UiStringRes(R.string.my_site_post_card_link_go_to_scheduled_posts))
    }

    /* DRAFT OR SCHEDULED POST ITEM - TITLE */

    @Test
    fun `given draft post with title, when card is built, then post title exists`() {
        val posts = getPosts(draftPosts = listOf(post.copy(title = POST_TITLE)))

        val postsCard = buildPostsCard(posts).filterDraftPostCard()?.postItems?.first()

        assertThat(postsCard?.title).isEqualTo(UiStringText(POST_TITLE))
    }

    @Test
    fun `given draft post without title, when card is built, then untitled title exists`() {
        val posts = getPosts(draftPosts = listOf(post.copy(title = "")))

        val postsCard = buildPostsCard(posts).filterDraftPostCard()?.postItems?.first()

        assertThat(postsCard?.title).isEqualTo(UiStringRes(R.string.my_site_untitled_post))
    }

    /* DRAFT OR SCHEDULED POST ITEM - EXCERPT */

    @Test
    fun `given draft post with excerpt, when card is built, then excerpt exists`() {
        val posts = getPosts(draftPosts = listOf(post.copy(content = POST_CONTENT)))

        val postsCard = buildPostsCard(posts).filterDraftPostCard()?.postItems?.first()

        assertThat(postsCard?.excerpt).isEqualTo(UiStringText(POST_CONTENT))
    }

    @Test
    fun `given draft post without excerpt, when card is built, then excerpt not exists`() {
        val posts = getPosts(draftPosts = listOf(post.copy(content = "")))

        val postsCard = buildPostsCard(posts).filterDraftPostCard()?.postItems?.first()

        assertThat(postsCard?.excerpt).isNull()
    }

    @Test
    fun `given scheduled post, when card is built, then excerpt exists as date`() {
        val posts = getPosts(scheduledPosts = listOf(post.copy(content = "")))

        val postsCard = buildPostsCard(posts).filterScheduledPostCard()?.postItems?.first()

        assertThat(postsCard?.excerpt).isEqualTo(UiStringText("Dec 6"))
    }

    /* DRAFT OR SCHEDULED POST ITEM - FEATURED IMAGE */

    @Test
    fun `given post with featured image, when card is built, then featured image visible`() {
        val posts = getPosts(draftPosts = listOf(post.copy(featuredImage = FEATURED_IMAGE_URL)))

        val postsCard = buildPostsCard(posts).filterDraftPostCard()?.postItems?.first()

        assertThat(postsCard?.featuredImageUrl).isNotNull
    }

    @Test
    fun `given post without featured image, when card is built, then featured image not visible`() {
        val posts = getPosts(draftPosts = listOf(post.copy(featuredImage = null)))

        val postsCard = buildPostsCard(posts).filterDraftPostCard()?.postItems?.first()

        assertThat(postsCard?.featuredImageUrl).isNull()
    }

    /* DRAFT OR SCHEDULED POST ITEM - TIME ICON */

    @Test
    fun `given draft post, when card is built, then time icon is not visible`() {
        val posts = getPosts(draftPosts = listOf(post))

        val postsCard = buildPostsCard(posts)

        assertThat((postsCard.filterDraftPostCard())?.postItems?.first()?.isTimeIconVisible).isFalse
    }

    @Test
    fun `given scheduled post, when card is built, then time icon is visible`() {
        val posts = getPosts(scheduledPosts = listOf(post))

        val postsCard = buildPostsCard(posts)

        assertThat((postsCard.filterScheduledPostCard())?.postItems?.first()?.isTimeIconVisible).isTrue
    }

    private fun List<PostCard>.filterPostErrorCard() = firstOrNull { it.dashboardCardType == POST_CARD_ERROR }

    @Suppress("UNCHECKED_CAST")
    private fun List<PostCard>.filterCreateFirstPostCard() = (
            filter {
                it.dashboardCardType == DashboardCardType.POST_CARD_WITHOUT_POST_ITEMS
            } as? List<PostCardWithoutPostItems>
            )?.firstOrNull { it.postCardType == PostCardType.CREATE_FIRST }

    @Suppress("UNCHECKED_CAST")
    private fun List<PostCard>.filterCreateNextPostCard() = (
            filter {
                it.dashboardCardType == DashboardCardType.POST_CARD_WITHOUT_POST_ITEMS
            } as? List<PostCardWithoutPostItems>
            )?.firstOrNull { it.postCardType == PostCardType.CREATE_NEXT }

    @Suppress("UNCHECKED_CAST")
    private fun List<PostCard>.filterDraftPostCard() = (
            filter {
                it.dashboardCardType == DashboardCardType.POST_CARD_WITH_POST_ITEMS
            } as? List<PostCardWithPostItems>
            )?.firstOrNull { it.postCardType == PostCardType.DRAFT }

    @Suppress("UNCHECKED_CAST")
    private fun List<PostCard>.filterScheduledPostCard() = (
            filter {
                it.dashboardCardType == DashboardCardType.POST_CARD_WITH_POST_ITEMS
            } as? List<PostCardWithPostItems>
            )?.firstOrNull { it.postCardType == PostCardType.SCHEDULED }

    private fun buildPostsCard(posts: PostsCardModel) = builder.build(
            PostCardBuilderParams(
                    posts = posts,
                    onPostItemClick = onPostItemClick,
                    onFooterLinkClick = onPostCardFooterLinkClick
            )
    )

    private fun getPosts(
        hasPublished: Boolean = false,
        draftPosts: List<PostCardModel> = emptyList(),
        scheduledPosts: List<PostCardModel> = emptyList()
    ) = PostsCardModel(
            hasPublished = hasPublished,
            draft = draftPosts,
            scheduled = scheduledPosts
    )
}
