package org.wordpress.android.ui.mysite.cards.post

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.test
import org.wordpress.android.testScope
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.PostsUpdate
import org.wordpress.android.ui.mysite.cards.post.mockdata.MockedDataJsonUtils

class PostCardsSourceTest : BaseUnitTest() {
    @Mock lateinit var mockedDataJsonUtils: MockedDataJsonUtils
    private lateinit var postCardSource: PostCardsSource

    @Before
    fun setUp() {
        postCardSource = PostCardsSource(mockedDataJsonUtils)
    }

    @Test
    fun `when source is requested upon start, then mocked data is loaded`() = test {
        var result: PostsUpdate? = null
        postCardSource.buildSource(testScope(), 1).observeForever { it?.let { result = it } }

        assertThat(result?.mockedPostsData).isNotNull
    }
}
