package org.wordpress.android.ui.newstats.poststats

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.ui.stats.StatsConstants

class PostHeaderActionTest {
    @Test
    fun `given a post, then it opens in the reader`() {
        val action = postHeaderAction(url = URL, type = StatsConstants.ITEM_TYPE_POST, status = PUBLISH)

        assertThat(action).isEqualTo(PostHeaderAction.OpenInReader(URL))
    }

    @Test
    fun `given a page, then it opens in a browser`() {
        val action = postHeaderAction(url = URL, type = "page", status = PUBLISH)

        assertThat(action).isEqualTo(PostHeaderAction.OpenInBrowser(URL))
    }

    /**
     * The case CMM-2580 is about: attachment rows are titled with bare numbers, so opening the
     * item is the only way to tell which picture a row refers to.
     */
    @Test
    fun `given an attachment, then it opens in a browser`() {
        val action = postHeaderAction(url = URL, type = StatsConstants.ITEM_TYPE_ATTACHMENT, status = PUBLISH)

        assertThat(action).isEqualTo(PostHeaderAction.OpenInBrowser(URL))
    }

    /** WordPress.com can return any registered post type, and only "post" reads in the Reader. */
    @Test
    fun `given a custom post type, then it opens in a browser`() {
        val action = postHeaderAction(url = URL, type = "jetpack-portfolio", status = PUBLISH)

        assertThat(action).isEqualTo(PostHeaderAction.OpenInBrowser(URL))
    }

    @Test
    fun `given no url, then the header is inert`() {
        val action = postHeaderAction(url = null, type = StatsConstants.ITEM_TYPE_POST, status = PUBLISH)

        assertThat(action).isEqualTo(PostHeaderAction.None)
    }

    @Test
    fun `given an empty url, then the header is inert`() {
        val action = postHeaderAction(url = "", type = StatsConstants.ITEM_TYPE_POST, status = PUBLISH)

        assertThat(action).isEqualTo(PostHeaderAction.None)
    }

    @Test
    fun `given a blank url, then the header is inert`() {
        val action = postHeaderAction(url = "   ", type = StatsConstants.ITEM_TYPE_POST, status = PUBLISH)

        assertThat(action).isEqualTo(PostHeaderAction.None)
    }

    /** The site's home page comes back with no post object at all, so no field is present. */
    @Test
    fun `given no post, then the header is inert`() {
        val action = postHeaderAction(url = null, type = null, status = null)

        assertThat(action).isEqualTo(PostHeaderAction.None)
    }

    @Test
    fun `given a url but no type, then the header is inert`() {
        val action = postHeaderAction(url = URL, type = null, status = PUBLISH)

        assertThat(action).isEqualTo(PostHeaderAction.None)
    }

    /** Every openable action carries the URL, so a caller always has something to fall back on. */
    @Test
    fun `given an openable item, then the action carries its url`() {
        val post = postHeaderAction(url = URL, type = StatsConstants.ITEM_TYPE_POST, status = PUBLISH)
        val page = postHeaderAction(url = URL, type = "page", status = PUBLISH)

        assertThat(post).isInstanceOf(PostHeaderAction.Open::class.java)
        assertThat((post as PostHeaderAction.Open).url).isEqualTo(URL)
        assertThat(page).isInstanceOf(PostHeaderAction.Open::class.java)
        assertThat((page as PostHeaderAction.Open).url).isEqualTo(URL)
    }

    /**
     * A post can be published, accrue views, and only later be unpublished or trashed. Stats keeps
     * its view history, so the screen still lists it -- but its URL no longer resolves.
     */
    @Test
    fun `given an item that is no longer public, then the header is inert`() {
        listOf("draft", "pending", "future", "trash", "auto-draft").forEach { status ->
            val post = postHeaderAction(url = URL, type = StatsConstants.ITEM_TYPE_POST, status = status)
            val page = postHeaderAction(url = URL, type = "page", status = status)

            assertThat(post).`as`("post with status %s", status).isEqualTo(PostHeaderAction.None)
            assertThat(page).`as`("page with status %s", status).isEqualTo(PostHeaderAction.None)
        }
    }

    /** A private post reads in the Reader, which is signed in as the owner, so it stays openable. */
    @Test
    fun `given a private post, then it still opens in the reader`() {
        val action = postHeaderAction(url = URL, type = StatsConstants.ITEM_TYPE_POST, status = "private")

        assertThat(action).isEqualTo(PostHeaderAction.OpenInReader(URL))
    }

    /**
     * A status registered by a plugin is not one we can classify, so it keeps behaving as it did
     * before the status was consulted at all -- better a link that may 404 than a live item that
     * cannot be opened.
     */
    @Test
    fun `given an unrecognised status, then the item stays openable`() {
        val action = postHeaderAction(url = URL, type = StatsConstants.ITEM_TYPE_POST, status = "wc-completed")

        assertThat(action).isEqualTo(PostHeaderAction.OpenInReader(URL))
    }

    @Test
    fun `given no status, then the item stays openable`() {
        val action = postHeaderAction(url = URL, type = StatsConstants.ITEM_TYPE_POST, status = null)

        assertThat(action).isEqualTo(PostHeaderAction.OpenInReader(URL))
    }

    companion object {
        private const val URL = "https://example.com/hello-world/"
        private const val PUBLISH = "publish"
    }
}
