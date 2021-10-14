package org.wordpress.android.ui.mysite.cards.post

import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.mysite.cards.post.PostCardBuilder.Companion.DRAFT_TITLE
import org.wordpress.android.ui.mysite.cards.post.PostCardBuilder.Companion.SCHEDULED_TITLE
import org.wordpress.android.ui.mysite.cards.post.mockdata.MockedPostsData
import org.wordpress.android.ui.mysite.cards.post.mockdata.MockedPostsData.Post
import org.wordpress.android.ui.mysite.cards.post.mockdata.MockedPostsData.Posts
import org.wordpress.android.ui.utils.UiString.UiStringText

// This class contains placeholder tests until mock data is removed
@InternalCoroutinesApi
class PostCardBuilderTest : BaseUnitTest() {
    private lateinit var builder: PostCardBuilder

    private val mockedPostsData: MockedPostsData
        get() = MockedPostsData(
                posts = Posts(
                        hasPublishedPosts = true,
                        draft = listOf(Post(id = "1", title = DRAFT_TITLE)),
                        scheduled = listOf(Post(id = "1", title = SCHEDULED_TITLE)),
                ),
        )

    @Before
    fun setUp() {
        builder = PostCardBuilder()
    }

    private fun buildPostCards() = builder.build(mockedPostsData)

    @Test
    fun `when toolbar is built, then card title exists`() {
        val postCards = buildPostCards()
        assertThat(postCards[0].title).isEqualTo(UiStringText(DRAFT_TITLE))
        assertThat(postCards[1].title).isEqualTo(UiStringText(SCHEDULED_TITLE))
    }
}
