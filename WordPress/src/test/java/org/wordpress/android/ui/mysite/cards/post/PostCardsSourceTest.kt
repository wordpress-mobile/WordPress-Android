package org.wordpress.android.ui.mysite.cards.post

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.test
import org.wordpress.android.testScope
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.PostsUpdate
import org.wordpress.android.ui.mysite.cards.post.mockdata.MockedDataJsonUtils
import org.wordpress.android.ui.mysite.cards.post.mockdata.MockedPostsData
import org.wordpress.android.ui.mysite.cards.post.mockdata.MockedPostsData.Post
import org.wordpress.android.ui.mysite.cards.post.mockdata.MockedPostsData.Posts

class PostCardsSourceTest : BaseUnitTest() {
    @Mock lateinit var mockedDataJsonUtils: MockedDataJsonUtils
    private lateinit var postCardSource: PostCardsSource
    private lateinit var isRefreshing: MutableList<Boolean>

    @Before
    fun setUp() {
        whenever(mockedDataJsonUtils.getJsonStringFromRawResource(any())).thenReturn("string")
        whenever(mockedDataJsonUtils.getMockedPostsDataFromJsonString(any())).thenReturn(mockedPostsData)
        postCardSource = PostCardsSource(mockedDataJsonUtils)
        isRefreshing = mutableListOf()
    }

    @Test
    fun `when source is requested upon start, then mocked data is loaded`() = test {
        var result: PostsUpdate? = null
        postCardSource.build(testScope(), 1).observeForever {
            it?.let {
                result = it
            }
        }
        assertThat(result?.mockedPostsData).isNotNull
    }

    @Test
    fun `when refresh is invoked, then data is refreshed`() = test {
        val result: MutableList<PostsUpdate?> = mutableListOf()
        postCardSource.build(testScope(), 1).observeForever { it?.let { result.add(it) } }
        postCardSource.refresh.observeForever { isRefreshing.add(it) }

        postCardSource.refresh()

        assertThat(result.first()?.mockedPostsData).isNotNull
        assertThat(result.last()?.mockedPostsData).isNotNull
        assertThat(result.size).isEqualTo(2)
    }

    @Test
    fun `when source is invoked, then refresh is false`() = test {
        postCardSource.refresh.observeForever { isRefreshing.add(it) }

        postCardSource.build(testScope(), 1)

        assertThat(isRefreshing.last()).isFalse
    }

    @Test
    fun `when refresh is invoked, then refresh is true`() = test {
        postCardSource.refresh.observeForever { isRefreshing.add(it) }

        postCardSource.refresh()

        assertThat(isRefreshing.last()).isTrue
    }

    @Test
    fun `when data has been refreshed, then refresh is set to false`() = test {
        val result: MutableList<PostsUpdate?> = mutableListOf()
        postCardSource.build(testScope(), 1).observeForever { it?.let { result.add(it) } }
        postCardSource.refresh.observeForever { isRefreshing.add(it) }

        postCardSource.refresh()

        assertThat(isRefreshing.last()).isFalse
    }

    private val mockedPostsData: MockedPostsData
        get() = MockedPostsData(
                posts = Posts(
                        hasPublishedPosts = true,
                        draft = listOf(Post(id = 1, title = "draft")),
                        scheduled = listOf(Post(id = 1, title = "scheduled"))
                )
        )
}
