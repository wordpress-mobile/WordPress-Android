package org.wordpress.android.ui.newstats.datasource

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.store.SiteStore
import rs.wordpress.api.kotlin.WpComApiClient
import uniffi.wp_api.StatsTopAuthorsAuthor
import uniffi.wp_api.StatsTopAuthorsPost

/**
 * Covers how the top authors response is mapped, in particular the posts each author carries: the
 * Authors card and its "show all" screen list them under an expanded row, so dropping them (as the
 * first version of this card did) is what CMM-2776 was about.
 */
@ExperimentalCoroutinesApi
class StatsDataSourceTopAuthorsTest : BaseUnitTest() {
    @Mock
    private lateinit var wpComApiClient: WpComApiClient

    @Mock
    private lateinit var siteStore: SiteStore

    private lateinit var dataSource: StatsDataSourceImpl

    @Before
    fun setUp() {
        dataSource = StatsDataSourceImpl(wpComApiClient, siteStore)
    }

    @Test
    fun `given an author with posts, when mapping, then each post is kept in the API's order`() {
        val authors = dataSource.mapTopAuthors(
            listOf(
                author(
                    name = "John Doe",
                    views = 150uL,
                    posts = listOf(
                        post(id = 11uL, title = "Most read", views = 100uL),
                        post(id = 12uL, title = "Less read", views = 50uL)
                    )
                )
            )
        )

        assertThat(authors.single().posts).containsExactly(
            TopAuthorPostItem(id = 11L, title = "Most read", views = 100L),
            TopAuthorPostItem(id = 12L, title = "Less read", views = 50L)
        )
    }

    @Test
    fun `given an author with no posts, when mapping, then its posts are empty`() {
        val authors = dataSource.mapTopAuthors(listOf(author(name = "John Doe", views = 150uL)))

        assertThat(authors.single().name).isEqualTo("John Doe")
        assertThat(authors.single().views).isEqualTo(150L)
        assertThat(authors.single().posts).isEmpty()
    }

    @Test
    fun `given a post the API reports no title for, when mapping, then its title is blank`() {
        val authors = dataSource.mapTopAuthors(
            listOf(
                author(
                    name = "John Doe",
                    views = 10uL,
                    posts = listOf(post(id = 11uL, title = null, views = 10uL))
                )
            )
        )

        assertThat(authors.single().posts.single().title).isEmpty()
    }

    private fun author(
        name: String,
        views: ULong,
        posts: List<StatsTopAuthorsPost> = emptyList()
    ) = StatsTopAuthorsAuthor(
        name = name,
        avatar = null,
        views = views,
        posts = posts,
        followData = null,
        authorId = null,
        otherViews = null
    )

    private fun post(id: ULong, title: String?, views: ULong) = StatsTopAuthorsPost(
        id = id,
        title = title,
        url = null,
        views = views
    )
}
