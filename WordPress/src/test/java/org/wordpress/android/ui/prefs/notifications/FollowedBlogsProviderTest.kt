package org.wordpress.android.ui.prefs.notifications

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.datasets.ReaderBlogTableWrapper
import org.wordpress.android.fluxc.model.SubscriptionModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderBlog
import org.wordpress.android.ui.prefs.notifications.FollowedBlogsProvider.PreferenceModel
import org.wordpress.android.ui.utils.UrlUtilsWrapper

@RunWith(MockitoJUnitRunner::class)
class FollowedBlogsProviderTest {
    @Mock
    lateinit var accountStore: AccountStore
    @Mock
    lateinit var readerBlogTable: ReaderBlogTableWrapper
    @Mock
    lateinit var urlUtils: UrlUtilsWrapper
    private lateinit var followedBlogsProvider: FollowedBlogsProvider
    private val blogName = "blog name"
    private val url = "http://blog.url.com/feed"
    private val subscriptionUrl = "http://unknown.com"
    private val urlHost = "blog.url.com"
    private val emailFrequency = "daily"

    @Before
    fun setUp() {
        followedBlogsProvider = FollowedBlogsProvider(accountStore, readerBlogTable, urlUtils)
    }

    @Test
    fun `maps followed blog and adds click handler when blog id matches`() {
        val readerBlogId = 12345L

        val blogPost = setupBlogPost(readerBlogId, blogName, url, urlHost)
        whenever(readerBlogTable.getFollowedBlogs()).thenReturn(listOf(blogPost))
        val subscriptionModel = setupSubscriptionModel(
            readerBlogId.toString(),
            shouldNotifyPosts = true,
            shouldEmailPosts = false,
            shouldEmailComments = false,
            emailFrequency = emailFrequency,
            subscriptionUrl = subscriptionUrl,
            subscriptionUrlHost = subscriptionUrl
        )
        whenever(accountStore.subscriptions).thenReturn(listOf(subscriptionModel))

        val result = followedBlogsProvider.getAllFollowedBlogs(null)

        assertThat(result).hasSize(1)
        result.first().assertModel(readerBlogId, blogName, urlHost)
        result.first().assertClickHandler(
            shouldNotifyPosts = true,
            shouldEmailPosts = false,
            shouldEmailComments = false,
            emailFrequency = emailFrequency
        )
    }

    @Test
    fun `maps followed blog and adds click handler when feed id matches`() {
        val readerBlogId = 1L
        val feedId = 10L
        val blogPost = setupBlogPost(readerBlogId, feedId = feedId)
        whenever(readerBlogTable.getFollowedBlogs()).thenReturn(listOf(blogPost))
        val subscriptionModel = setupSubscriptionModel(
            "2",
            shouldNotifyPosts = false,
            shouldEmailPosts = true,
            shouldEmailComments = false,
            feedId = feedId.toString()
        )
        whenever(accountStore.subscriptions).thenReturn(listOf(subscriptionModel))

        val result = followedBlogsProvider.getAllFollowedBlogs(null)

        assertThat(result).hasSize(1)
        result.first().assertModel(readerBlogId, blogName, urlHost)
        result.first().assertClickHandler(
            shouldNotifyPosts = false,
            shouldEmailPosts = true,
            shouldEmailComments = false,
            emailFrequency = emailFrequency
        )
    }

    @Test
    fun `does not add click handler when feed id matches but blog id is missing`() {
        val readerBlogId = 1L
        val feedId = 10L
        val blogPost = setupBlogPost(readerBlogId, feedId = feedId)
        whenever(readerBlogTable.getFollowedBlogs()).thenReturn(listOf(blogPost))
        val subscriptionModel = setupSubscriptionModel(
            "false",
            shouldNotifyPosts = false,
            shouldEmailPosts = true,
            shouldEmailComments = false,
            feedId = feedId.toString()
        )
        whenever(accountStore.subscriptions).thenReturn(listOf(subscriptionModel))

        val result = followedBlogsProvider.getAllFollowedBlogs(null)

        assertThat(result).hasSize(1)
        result.first().assertModel(readerBlogId, blogName, urlHost)
        assertThat(result.first().clickHandler).isNull()
    }

    @Test
    fun `maps followed blog and adds click handler when url host matches`() {
        val readerBlogId = 1L
        val blogPost = setupBlogPost(readerBlogId, blogUrl = url, blogUrlHost = urlHost)
        whenever(readerBlogTable.getFollowedBlogs()).thenReturn(listOf(blogPost))
        val subscriptionModel = setupSubscriptionModel(
            readerBlogId = "2",
            shouldNotifyPosts = false,
            shouldEmailPosts = false,
            shouldEmailComments = true,
            subscriptionUrl = url,
            subscriptionUrlHost = urlHost
        )
        whenever(accountStore.subscriptions).thenReturn(listOf(subscriptionModel))

        val result = followedBlogsProvider.getAllFollowedBlogs(null)

        assertThat(result).hasSize(1)
        result.first().assertModel(readerBlogId, blogName, urlHost)
        result.first().assertClickHandler(
            shouldNotifyPosts = false,
            shouldEmailPosts = false,
            shouldEmailComments = true,
            emailFrequency = emailFrequency
        )
    }

    @Test
    fun `maps followed blog and does not add click handler when item does not match`() {
        val readerBlogId = 12345L
        val blogPost = setupBlogPost(readerBlogId, blogName, url, urlHost)
        whenever(readerBlogTable.getFollowedBlogs()).thenReturn(listOf(blogPost))
        val subscriptionModel = setupSubscriptionModel(
            "false",
            emailFrequency = emailFrequency,
            subscriptionUrl = subscriptionUrl,
            subscriptionUrlHost = subscriptionUrl
        )
        whenever(accountStore.subscriptions).thenReturn(listOf(subscriptionModel))

        val result = followedBlogsProvider.getAllFollowedBlogs(null)

        assertThat(result).hasSize(1)
        result.first().assertModel(readerBlogId, blogName, urlHost)
        assertThat(result.first().clickHandler).isNull()
    }

    @Test
    fun `returns empty list when subscriptions are not loaded`() {
        whenever(accountStore.subscriptions).thenReturn(listOf())

        val result = followedBlogsProvider.getAllFollowedBlogs(null)

        assertThat(result).isEmpty()
    }

    @Test
    fun `filters out items by query`() {
        val readerBlogId = 12345L
        val query = "query"
        val blogNameWithQuery = "title with $query"
        val blogPostWithQuery = setupBlogPost(readerBlogId, blogNameWithQuery, url, urlHost)
        val blogPostWithoutQuery = setupBlogPost(readerBlogId, "title", url, urlHost)
        whenever(readerBlogTable.getFollowedBlogs()).thenReturn(listOf(blogPostWithQuery, blogPostWithoutQuery))

        val subscriptionModel = setupSubscriptionModel(
            "false",
            emailFrequency = emailFrequency,
            subscriptionUrl = subscriptionUrl,
            subscriptionUrlHost = subscriptionUrl
        )
        whenever(accountStore.subscriptions).thenReturn(listOf(subscriptionModel))

        val result = followedBlogsProvider.getAllFollowedBlogs(query)

        assertThat(result).hasSize(1)
        result.first().assertModel(readerBlogId, blogNameWithQuery, urlHost)
        assertThat(result.first().clickHandler).isNull()
    }

    @Test
    fun `shows subscriptions when reader sites are empty`() {
        val readerBlogId = 12345L
        whenever(readerBlogTable.getFollowedBlogs()).thenReturn(listOf())

        val subscriptionModel = setupSubscriptionModel(
            "$readerBlogId",
            emailFrequency = emailFrequency,
            subscriptionUrl = subscriptionUrl,
            subscriptionUrlHost = subscriptionUrl
        )
        whenever(accountStore.subscriptions).thenReturn(listOf(subscriptionModel))

        val result = followedBlogsProvider.getAllFollowedBlogs(null)

        assertThat(result).hasSize(1)
        result.first().assertModel(readerBlogId, subscriptionUrl, subscriptionUrl)
        result.first().assertClickHandler(
            shouldNotifyPosts = false,
            shouldEmailPosts = false,
            shouldEmailComments = false,
            emailFrequency = emailFrequency
        )
    }

    private fun setupSubscriptionModel(
        readerBlogId: String = "false",
        shouldNotifyPosts: Boolean = false,
        shouldEmailPosts: Boolean = false,
        shouldEmailComments: Boolean = false,
        emailFrequency: String = this.emailFrequency,
        subscriptionUrl: String = this.subscriptionUrl,
        subscriptionUrlHost: String = this.subscriptionUrl,
        feedId: String = "false"
    ): SubscriptionModel {
        val subscriptionModel = SubscriptionModel()
        subscriptionModel.blogId = readerBlogId
        subscriptionModel.shouldNotifyPosts = shouldNotifyPosts
        subscriptionModel.shouldEmailPosts = shouldEmailPosts
        subscriptionModel.emailPostsFrequency = emailFrequency
        subscriptionModel.shouldEmailComments = shouldEmailComments
        subscriptionModel.url = subscriptionUrl
        subscriptionModel.feedId = feedId
        whenever(urlUtils.getHost(subscriptionUrl)).thenReturn(subscriptionUrlHost)
        return subscriptionModel
    }

    private fun setupBlogPost(
        readerBlogId: Long,
        blogName: String = this.blogName,
        blogUrl: String = this.url,
        blogUrlHost: String = this.urlHost,
        feedId: Long? = null
    ): ReaderBlog {
        val readerBlog = ReaderBlog()
        readerBlog.blogId = readerBlogId
        readerBlog.name = blogName
        readerBlog.url = blogUrl
        feedId?.let {
            readerBlog.feedId = it
        }
        whenever(urlUtils.getHost(blogUrl)).thenReturn(blogUrlHost)
        return readerBlog
    }

    private fun PreferenceModel.assertClickHandler(
        shouldNotifyPosts: Boolean,
        shouldEmailPosts: Boolean,
        shouldEmailComments: Boolean,
        emailFrequency: String
    ) {
        assertThat(this.clickHandler!!.emailPostFrequency).isEqualTo(emailFrequency)
        assertThat(this.clickHandler!!.shouldNotifyPosts).isEqualTo(shouldNotifyPosts)
        assertThat(this.clickHandler!!.shouldEmailPosts).isEqualTo(shouldEmailPosts)
        assertThat(this.clickHandler!!.shouldEmailComments).isEqualTo(shouldEmailComments)
    }

    private fun PreferenceModel.assertModel(
        readerBlogId: Long,
        blogName: String,
        urlHost: String
    ) {
        assertThat(this.blogId).isEqualTo(readerBlogId.toString())
        assertThat(this.title).isEqualTo(blogName)
        assertThat(this.summary).isEqualTo(urlHost)
    }
}
