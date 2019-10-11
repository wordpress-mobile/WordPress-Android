package org.wordpress.android.ui.prefs.notifications

import org.wordpress.android.datasets.ReaderBlogTableWrapper
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.prefs.notifications.FollowedBlogsProvider.PreferenceModel.ClickHandler
import org.wordpress.android.ui.utils.UriUtilsWrapper
import javax.inject.Inject

class FollowedBlogsProvider
@Inject constructor(
    private val accountStore: AccountStore,
    private val readerBlogTable: ReaderBlogTableWrapper,
    private val uriUtils: UriUtilsWrapper
) {
    fun getAllFollowedBlogs(query: String?): List<PreferenceModel> {
        val subscriptions = accountStore.subscriptions
        val allFollowedBlogs = if (query != null) {
            readerBlogTable.getFollowedBlogs()
                    .filter { it.name.contains(query) || it.url.contains(query) }
        } else {
            readerBlogTable.getFollowedBlogs()
        }
        val result = allFollowedBlogs.map { readerBlog ->
            val match = subscriptions.find { subscription ->
                subscription.blogId == readerBlog.blogId.toString() ||
                        subscription.feedId == readerBlog.feedId.toString() ||
                        uriUtils.getHost(subscription.url) == uriUtils.getHost(readerBlog.url)
            }
            if (match != null) {
                PreferenceModel(
                        getSiteNameOrHostFromSubscription(readerBlog.name, readerBlog.url),
                        uriUtils.getHost(readerBlog.url),
                        readerBlog.blogId.toString(),
                        ClickHandler(
                                match.shouldNotifyPosts,
                                match.shouldEmailPosts,
                                match.emailPostsFrequency,
                                match.shouldEmailComments
                        )
                )
            } else {
                PreferenceModel(
                        getSiteNameOrHostFromSubscription(readerBlog.name, readerBlog.url),
                        uriUtils.getHost(readerBlog.url),
                        readerBlog.blogId.toString()
                )
            }
        }
        return result
    }

    private fun getSiteNameOrHostFromSubscription(
        blogName: String?,
        blogUrl: String
    ): String {
        var name: String? = blogName

        if (name != null) {
            if (name.trim { it <= ' ' }.isEmpty()) {
                name = uriUtils.getHost(blogUrl)
            }
        } else {
            name = uriUtils.getHost(blogUrl)
        }

        return name
    }

    data class PreferenceModel(
        val title: String,
        val summary: String,
        val blogId: String,
        val clickHandler: ClickHandler? = null
    ) {
        data class ClickHandler(
            val shouldNotifyPosts: Boolean,
            val shouldEmailPosts: Boolean,
            val emailPostFrequency: String?,
            val shouldEmailComments: Boolean
        )
    }
}
