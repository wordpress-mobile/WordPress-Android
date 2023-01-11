package org.wordpress.android.ui.prefs.notifications

import org.wordpress.android.datasets.ReaderBlogTableWrapper
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.prefs.notifications.FollowedBlogsProvider.PreferenceModel.ClickHandler
import org.wordpress.android.ui.utils.UrlUtilsWrapper
import javax.inject.Inject

class FollowedBlogsProvider
@Inject constructor(
    private val accountStore: AccountStore,
    private val readerBlogTable: ReaderBlogTableWrapper,
    private val urlUtils: UrlUtilsWrapper
) {
    fun getAllFollowedBlogs(query: String?): List<PreferenceModel> {
        val subscriptions = accountStore.subscriptions
        if (subscriptions.isEmpty()) {
            return listOf()
        }
        val allFollowedBlogs = if (query != null) {
            readerBlogTable.getFollowedBlogs()
                .filter { it.name.contains(query) || it.url.contains(query) }
        } else {
            readerBlogTable.getFollowedBlogs()
        }
        // Load subscriptions when blogs are not loaded yet
        if (allFollowedBlogs.isEmpty()) {
            return subscriptions.map { subscription ->
                val clickHandler = if (subscription.blogId != null && subscription.blogId != "false") {
                    ClickHandler(
                        subscription.shouldNotifyPosts,
                        subscription.shouldEmailPosts,
                        subscription.emailPostsFrequency,
                        subscription.shouldEmailComments
                    )
                } else {
                    null
                }
                PreferenceModel(
                    getSiteNameOrHostFromSubscription(subscription.blogName, subscription.url),
                    urlUtils.getHost(subscription.url),
                    subscription.blogId.toString(),
                    clickHandler
                )
            }
        }
        return allFollowedBlogs.map { readerBlog ->
            val match = subscriptions.find { subscription ->
                subscription.blogId == readerBlog.blogId.toString() ||
                        subscription.feedId == readerBlog.feedId.toString() ||
                        urlUtils.getHost(subscription.url) == urlUtils.getHost(readerBlog.url)
            }
            // We don't have notification settings for feeds
            val clickHandler = if (match != null && match.blogId != null && match.blogId != "false") {
                ClickHandler(
                    match.shouldNotifyPosts,
                    match.shouldEmailPosts,
                    match.emailPostsFrequency,
                    match.shouldEmailComments
                )
            } else {
                null
            }
            PreferenceModel(
                getSiteNameOrHostFromSubscription(readerBlog.name, readerBlog.url),
                urlUtils.getHost(readerBlog.url),
                readerBlog.blogId.toString(),
                clickHandler
            )
        }
    }

    private fun getSiteNameOrHostFromSubscription(
        blogName: String?,
        blogUrl: String
    ): String {
        var name: String? = blogName

        if (name != null) {
            if (name.trim { it <= ' ' }.isEmpty()) {
                name = urlUtils.getHost(blogUrl)
            }
        } else {
            name = urlUtils.getHost(blogUrl)
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
