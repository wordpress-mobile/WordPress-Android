package org.wordpress.android.ui.mysite.cards.post

import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard.PostCardCreateFirst
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard.PostCardDraftOrScheduled
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PostCardBuilderParams
import org.wordpress.android.ui.mysite.cards.post.PostCardType.CREATE_FIRST
import org.wordpress.android.ui.mysite.cards.post.PostCardType.DRAFT
import org.wordpress.android.ui.mysite.cards.post.PostCardType.SCHEDULED
import org.wordpress.android.ui.mysite.cards.post.mockdata.MockedPostsData
import org.wordpress.android.ui.mysite.cards.post.mockdata.MockedPostsData.Post
import org.wordpress.android.ui.mysite.cards.post.mockdata.MockedPostsData.Posts
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText

private const val POST_ID = "1"
private const val POST_TITLE = "title"
private const val POST_EXCERPT = "excerpt"
private const val FEATURED_IMAGE_URL = "/image/url"

// This class contains placeholder tests until mock data is removed
@InternalCoroutinesApi
class PostCardBuilderTest : BaseUnitTest() {
    private lateinit var builder: PostCardBuilder
    private val post = Post(id = POST_ID)

    @Before
    fun setUp() {
        builder = PostCardBuilder()
    }

    /* CREATE FIRST POST CARD */

    @Test
    fun `given published post present, when post cards are built, then create first post card exists`() {
        val mockedPostsData = getMockedPostsData(hasPublishedPosts = true)

        val postCards = buildPostCards(mockedPostsData)

        assertThat(postCards.filterCreateFirstPostCard()).isNotNull
    }

    @Test
    fun `given published post not present, when post cards are built, then create first post card not exists`() {
        val mockedPostsData = getMockedPostsData(hasPublishedPosts = false)

        val postCards = buildPostCards(mockedPostsData)

        assertThat(postCards.filterCreateFirstPostCard()).isNull()
    }

    @Test
    fun `when create first post card is built, then it contains correct preset elements`() {
        val mockedPostsData = getMockedPostsData(hasPublishedPosts = true)

        val createFirstPostCard = buildPostCards(mockedPostsData).filterCreateFirstPostCard()

        assertThat(createFirstPostCard).isEqualTo(
                PostCardCreateFirst(
                        postCardType = CREATE_FIRST,
                        title = UiStringRes(R.string.my_site_create_first_post_title),
                        excerpt = UiStringRes(R.string.my_site_create_first_post_excerpt),
                        imageRes = R.drawable.create_first_temp // TODO: ashiagr replace with actual resource
                )
        )
    }

    /* DRAFT POST CARD */

    @Test
    fun `given draft post, when post cards are built, then draft post card exists`() {
        val mockedPostsData = getMockedPostsData(draftPosts = listOf(post))

        val postCards = buildPostCards(mockedPostsData)

        assertThat(postCards.filterDraftPostCard()).isNotNull
    }

    @Test
    fun `given no draft post, when post cards are built, then draft post card not exists`() {
        val mockedPostsData = getMockedPostsData(draftPosts = emptyList())

        val postCards = buildPostCards(mockedPostsData)

        assertThat(postCards.filterDraftPostCard()).isNull()
    }

    /* SCHEDULED POST CARD */

    @Test
    fun `given scheduled post, when post cards are built, then scheduled post card exists`() {
        val mockedPostsData = getMockedPostsData(scheduledPosts = listOf(post))

        val postCards = buildPostCards(mockedPostsData)

        assertThat(postCards.filterScheduledPostCard()).isNotNull
    }

    @Test
    fun `given no scheduled post, when post cards are built, then scheduled post card not exists`() {
        val mockedPostsData = getMockedPostsData(scheduledPosts = emptyList())

        val postCards = buildPostCards(mockedPostsData)

        assertThat(postCards.filterScheduledPostCard()).isNull()
    }

    /* DRAFT OR SCHEDULED POST ITEM - TITLE */

    @Test
    fun `given post with title, when draft post card is built, then post title exists`() {
        val mockedPostsData = getMockedPostsData(draftPosts = listOf(post.copy(title = POST_TITLE)))

        val postItem = buildPostCards(mockedPostsData).filterDraftPostCard()?.postItems?.first()

        assertThat(postItem?.title).isEqualTo(UiStringText(POST_TITLE))
    }

    @Test
    fun `given post without title, when draft post card is built, then untitled title exists`() {
        val mockedPostsData = getMockedPostsData(draftPosts = listOf(post.copy(title = null)))

        val postItem = buildPostCards(mockedPostsData).filterDraftPostCard()?.postItems?.first()

        assertThat(postItem?.title).isEqualTo(UiStringRes(R.string.my_site_untitled_post))
    }

    /* DRAFT OR SCHEDULED POST ITEM - EXCERPT */

    @Test
    fun `given post with excerpt, when post item is built, then excerpt exists`() {
        val mockedPostsData = getMockedPostsData(draftPosts = listOf(post.copy(excerpt = POST_EXCERPT)))

        val postItem = buildPostCards(mockedPostsData).filterDraftPostCard()?.postItems?.first()

        assertThat(postItem?.excerpt).isEqualTo(UiStringText(POST_EXCERPT))
    }

    @Test
    fun `given post without excerpt, when post item is built, then excerpt not exists`() {
        val mockedPostsData = getMockedPostsData(draftPosts = listOf(post.copy(excerpt = null)))

        val postItem = buildPostCards(mockedPostsData).filterDraftPostCard()?.postItems?.first()

        assertThat(postItem?.excerpt).isNull()
    }

    /* DRAFT OR SCHEDULED POST ITEM - FEATURED IMAGE */

    @Test
    fun `given post with featured image, when post item is built, then featured image visible`() {
        val mockedPostsData = getMockedPostsData(draftPosts = listOf(post.copy(featuredImageUrl = FEATURED_IMAGE_URL)))

        val postItem = buildPostCards(mockedPostsData).filterDraftPostCard()?.postItems?.first()

        assertThat(postItem?.featuredImageUrl).isNotNull
    }

    @Test
    fun `given post without featured image, when post item is built, then featured image not visible`() {
        val mockedPostsData = getMockedPostsData(draftPosts = listOf(post.copy(featuredImageUrl = null)))

        val postItem = buildPostCards(mockedPostsData).filterDraftPostCard()?.postItems?.first()

        assertThat(postItem?.featuredImageUrl).isNull()
    }

    /* DRAFT OR SCHEDULED POST ITEM - TIME ICON */

    @Test
    fun `given draft post, when post item is built, then time icon is not visible`() {
        val mockedPostsData = getMockedPostsData(draftPosts = listOf(post))

        val postCards = buildPostCards(mockedPostsData)

        assertThat((postCards.filterDraftPostCard())?.postItems?.first()?.isTimeIconVisible).isFalse
    }

    @Test
    fun `given scheduled post, when post item is built, then time icon is visible`() {
        val mockedPostsData = getMockedPostsData(scheduledPosts = listOf(post))

        val postCards = buildPostCards(mockedPostsData)

        assertThat((postCards.filterScheduledPostCard())?.postItems?.first()?.isTimeIconVisible).isTrue
    }

    private fun List<PostCard>.filterCreateFirstPostCard() =
            firstOrNull { it.postCardType == CREATE_FIRST } as? PostCardCreateFirst

    private fun List<PostCard>.filterDraftPostCard() =
            firstOrNull { it.postCardType == DRAFT } as? PostCardDraftOrScheduled

    private fun List<PostCard>.filterScheduledPostCard() =
            firstOrNull { it.postCardType == SCHEDULED } as? PostCardDraftOrScheduled

    private fun buildPostCards(mockedData: MockedPostsData) = builder.build(PostCardBuilderParams(mockedData))

    private fun getMockedPostsData(
        hasPublishedPosts: Boolean = false,
        draftPosts: List<Post>? = null,
        scheduledPosts: List<Post>? = null
    ) = MockedPostsData(
            posts = Posts(
                    hasPublishedPosts = hasPublishedPosts,
                    draft = draftPosts,
                    scheduled = scheduledPosts
            )
    )
}
