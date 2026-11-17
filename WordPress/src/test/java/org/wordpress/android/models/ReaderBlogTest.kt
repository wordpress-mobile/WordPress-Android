package org.wordpress.android.models

import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class ReaderBlogTest {
    @Test
    fun `site subscription is parsed from the site it carries`() {
        val blog = ReaderBlog.fromJson(JSONObject(SITE_SUBSCRIPTION_JSON))

        assertThat(blog.blogId).isEqualTo(252669098L)
        assertThat(blog.feedId).isEqualTo(0L)
        assertThat(blog.name).isEqualTo("Site Title")
        assertThat(blog.description).isEqualTo("Just another site")
        assertThat(blog.url).isEqualTo("https://test45838.wordpress.com")
        assertThat(blog.imageUrl).isEqualTo("https://s2.wp.com/i/webclip.png")
        assertThat(blog.isFollowing).isTrue()
    }

    @Test
    fun `feed subscription is parsed from the feed it carries`() {
        val blog = ReaderBlog.fromJson(JSONObject(FEED_SUBSCRIPTION_JSON))

        assertThat(blog.feedId).isEqualTo(39937882L)
        assertThat(blog.blogId).isEqualTo(39937882L)
        assertThat(blog.name).isEqualTo("Example Feed")
        assertThat(blog.feedUrl).isEqualTo("http://example.com/feed")
        assertThat(blog.isFollowing).isTrue()
    }

    /**
     * read/following/mine returns an error in place of the site or feed of a subscription the user can't
     * access, so the subscription's own fields are used - without them the site is dropped from the list of
     * subscriptions, as it was in CMM-2686
     */
    @Test
    fun `subscription whose feed the API refused is parsed from the subscription`() {
        val blog = ReaderBlog.fromJson(JSONObject(REFUSED_FEED_SUBSCRIPTION_JSON))

        assertThat(blog.feedId).isEqualTo(176978021L)
        assertThat(blog.blogId).isEqualTo(176978021L)
        assertThat(blog.name).isEqualTo("vanilla.wpmt.co")
        assertThat(blog.url).isEqualTo("http://vanilla.wpmt.co")
        assertThat(blog.feedUrl).isEqualTo("http://vanilla.wpmt.co/feed")
        assertThat(blog.imageUrl).isEmpty()
        assertThat(blog.isFollowing).isTrue()
    }

    @Test
    fun `subscription whose site the API refused is parsed from the subscription`() {
        val blog = ReaderBlog.fromJson(JSONObject(REFUSED_SITE_SUBSCRIPTION_JSON))

        assertThat(blog.blogId).isEqualTo(257682375L)
        assertThat(blog.feedId).isEqualTo(0L)
        assertThat(blog.name).isEqualTo("crafdp2")
        assertThat(blog.url).isEqualTo("https://crafdp2.wordpress.com")
        assertThat(blog.imageUrl).isEqualTo("https://crafdp2.wordpress.com/icon.png")
        assertThat(blog.organizationId).isEqualTo(1)
        assertThat(blog.isFollowing).isTrue()
    }

    @Test
    fun `subscription whose feed the API refused is kept in the list of subscriptions`() {
        val json = JSONObject("""{"subscriptions":[$REFUSED_FEED_SUBSCRIPTION_JSON]}""")

        val blogs = ReaderBlogList.fromJson(json)

        assertThat(blogs).hasSize(1)
        assertThat(blogs[0].name).isEqualTo("vanilla.wpmt.co")
    }

    @Test
    fun `site is parsed from a read sites response`() {
        val blog = ReaderBlog.fromJson(JSONObject(READ_SITE_JSON))

        assertThat(blog.blogId).isEqualTo(252388561L)
        assertThat(blog.name).isEqualTo("vanilla.wpmt.co")
        assertThat(blog.url).isEqualTo("https://vanilla.wpmt.co")
        assertThat(blog.isJetpack).isTrue()
        assertThat(blog.isFollowing).isFalse()
    }

    companion object {
        private const val SITE_SUBSCRIPTION_JSON = """
            {
              "ID": "865866758",
              "blog_ID": "252669098",
              "feed_ID": "178254754",
              "URL": "https://test45838.wordpress.com",
              "feed_URL": "http://test45838.wordpress.com",
              "date_subscribed": "2026-11-12T12:01:21+00:00",
              "name": "Site Title",
              "organization_id": 0,
              "unseen_count": 3,
              "site_icon": "https://s2.wp.com/i/webclip.png",
              "meta": {
                "data": {
                  "site": {
                    "ID": 252669098,
                    "name": "Site Title",
                    "description": "Just another site",
                    "URL": "https://test45838.wordpress.com",
                    "jetpack": false,
                    "is_private": false,
                    "is_following": true,
                    "subscribers_count": 5,
                    "icon": { "img": "https://s2.wp.com/i/webclip.png" },
                    "organization_id": 0
                  }
                }
              }
            }
        """

        private const val FEED_SUBSCRIPTION_JSON = """
            {
              "ID": "864817336",
              "blog_ID": 0,
              "feed_ID": "39937882",
              "URL": "http://example.com",
              "feed_URL": "http://example.com/feed",
              "date_subscribed": "2026-10-29T12:05:39+00:00",
              "name": "Example Feed",
              "site_icon": null,
              "meta": {
                "data": {
                  "feed": {
                    "feed_ID": 39937882,
                    "name": "Example Feed",
                    "URL": "http://example.com",
                    "feed_URL": "http://example.com/feed",
                    "subscribers_count": 12
                  }
                }
              }
            }
        """

        private const val REFUSED_FEED_SUBSCRIPTION_JSON = """
            {
              "ID": "866259320",
              "blog_ID": 0,
              "feed_ID": "176978021",
              "URL": "http://vanilla.wpmt.co",
              "feed_URL": "http://vanilla.wpmt.co/feed",
              "date_subscribed": "2026-11-17T16:04:48+00:00",
              "name": "vanilla.wpmt.co",
              "organization_id": 0,
              "unseen_count": 7,
              "site_icon": null,
              "meta": {
                "data": {
                  "feed": {
                    "errors": {
                      "unauthorized": ["User cannot access the feed of this private blog."]
                    },
                    "error_data": { "unauthorized": 403 }
                  }
                }
              }
            }
        """

        private const val REFUSED_SITE_SUBSCRIPTION_JSON = """
            {
              "ID": "864555847",
              "blog_ID": "257682375",
              "feed_ID": "182300651",
              "URL": "https://crafdp2.wordpress.com",
              "feed_URL": "http://crafdp2.wordpress.com",
              "date_subscribed": "2026-10-26T07:58:57+00:00",
              "name": "crafdp2",
              "organization_id": 1,
              "unseen_count": 8,
              "site_icon": "https://crafdp2.wordpress.com/icon.png",
              "meta": {
                "data": {
                  "site": {
                    "errors": { "unauthorized": ["User cannot access this private blog."] },
                    "error_data": { "unauthorized": 403 }
                  }
                }
              }
            }
        """

        private const val READ_SITE_JSON = """
            {
              "ID": 252388561,
              "name": "vanilla.wpmt.co",
              "description": "",
              "URL": "https://vanilla.wpmt.co",
              "jetpack": true,
              "is_private": false,
              "is_following": false,
              "subscribers_count": 1
            }
        """
    }
}
