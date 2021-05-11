package org.wordpress.android.ui.deeplinks

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction

class EditorLinkHandlerTest : BaseUnitTest() {
    @Mock lateinit var deepLinkUriUtils: DeepLinkUriUtils
    @Mock lateinit var postStore: PostStore
    private lateinit var editorLinkHandler: EditorLinkHandler
    private lateinit var site: SiteModel
    private lateinit var post: PostModel
    private val siteUrl = "site123"
    private val blogId = "321"
    private val remotePostId = 123L
    private val localPostId = 1
    private lateinit var toasts: MutableList<Int>

    @Before
    fun setUp() {
        editorLinkHandler = EditorLinkHandler(deepLinkUriUtils, postStore)
        site = SiteModel()
        site.url = siteUrl
        post = PostModel()
        post.setRemotePostId(remotePostId)
        post.setId(localPostId)
        toasts = mutableListOf()
        editorLinkHandler.toast.observeForever { it?.getContentIfNotHandled()?.let { toast -> toasts.add(toast) } }
    }

    @Test
    fun `handles post URI`() {
        val postUri = buildUri(host = "wordpress.com", path1 = "post")

        val isEditorUri = editorLinkHandler.isEditorUrl(postUri)

        assertThat(isEditorUri).isTrue()
    }

    @Test
    fun `handles post app link`() {
        val postUri = buildUri(host = "post")

        val isEditorUri = editorLinkHandler.isEditorUrl(postUri)

        assertThat(isEditorUri).isTrue()
    }

    @Test
    fun `does not handle post URI with different host`() {
        val postUri = buildUri(host = "wordpress.org", path1 = "post")

        val isEditorUri = editorLinkHandler.isEditorUrl(postUri)

        assertThat(isEditorUri).isFalse()
    }

    @Test
    fun `does not handle URI with different path`() {
        val postUri = buildUri(host = "wordpress.com", path1 = "stats")

        val isEditorUri = editorLinkHandler.isEditorUrl(postUri)

        assertThat(isEditorUri).isFalse()
    }

    @Test
    fun `deeplink - opens editor and shows toast when site not found`() {
        val uri = buildUri(path1 = "post", path2 = siteUrl)

        val navigateAction = editorLinkHandler.buildOpenEditorNavigateAction(uri)

        assertThat(navigateAction).isEqualTo(NavigateAction.OpenEditor)
        assertThat(toasts.last()).isEqualTo(R.string.blog_not_found)
    }

    @Test
    fun `deeplink - opens editor for a site when post missing in URL`() {
        val uri = buildUri(path1 = "post", path2 = siteUrl)
        whenever(deepLinkUriUtils.hostToSite(siteUrl)).thenReturn(site)

        val navigateAction = editorLinkHandler.buildOpenEditorNavigateAction(uri)

        assertThat(navigateAction).isEqualTo(NavigateAction.OpenEditorForSite(site))
        assertThat(toasts).isEmpty()
    }

    @Test
    fun `deeplink - opens editor for a post when both site and post exist`() {
        val uri = buildUri(path1 = "post", path2 = siteUrl, path3 = remotePostId.toString())
        whenever(deepLinkUriUtils.hostToSite(siteUrl)).thenReturn(site)
        whenever(postStore.getPostByRemotePostId(remotePostId, site)).thenReturn(post)

        val navigateAction = editorLinkHandler.buildOpenEditorNavigateAction(uri)

        assertThat(navigateAction).isEqualTo(NavigateAction.OpenEditorForPost(site, localPostId))
        assertThat(toasts).isEmpty()
    }

    @Test
    fun `deeplink - opens editor for a site and shows toast when post not found`() {
        val uri = buildUri(path1 = "post", path2 = siteUrl, path3 = remotePostId.toString())
        whenever(deepLinkUriUtils.hostToSite(siteUrl)).thenReturn(site)
        whenever(postStore.getPostByRemotePostId(remotePostId, site)).thenReturn(null)

        val navigateAction = editorLinkHandler.buildOpenEditorNavigateAction(uri)

        assertThat(navigateAction).isEqualTo(NavigateAction.OpenEditorForSite(site))
        assertThat(toasts.last()).isEqualTo(R.string.post_not_found)
    }

    @Test
    fun `applink - opens editor and shows toast when site not found`() {
        val uri = buildUri(
                host = "post",
                queryParam1 = "blogId" to blogId
        )

        val navigateAction = editorLinkHandler.buildOpenEditorNavigateAction(uri)

        assertThat(navigateAction).isEqualTo(NavigateAction.OpenEditor)
        assertThat(toasts.last()).isEqualTo(R.string.blog_not_found)
    }

    @Test
    fun `applink - opens editor for a site from ID when post missing in URL`() {
        val uri = buildUri(
                host = "post",
                queryParam1 = "blogId" to blogId
        )
        whenever(deepLinkUriUtils.blogIdToSite(blogId)).thenReturn(site)

        val navigateAction = editorLinkHandler.buildOpenEditorNavigateAction(uri)

        assertThat(navigateAction).isEqualTo(NavigateAction.OpenEditorForSite(site))
        assertThat(toasts).isEmpty()
    }

    @Test
    fun `applink - opens editor for a site from URL`() {
        val uri = buildUri(
                host = "post",
                queryParam1 = "blogId" to siteUrl
        )
        whenever(deepLinkUriUtils.hostToSite(siteUrl)).thenReturn(site)

        val navigateAction = editorLinkHandler.buildOpenEditorNavigateAction(uri)

        assertThat(navigateAction).isEqualTo(NavigateAction.OpenEditorForSite(site))
        assertThat(toasts).isEmpty()
    }

    @Test
    fun `applink - opens editor for a post when both site and post exist`() {
        val uri = buildUri(
                host = "post",
                queryParam1 = "blogId" to blogId,
                queryParam2 = "postId" to remotePostId.toString()
        )
        whenever(deepLinkUriUtils.blogIdToSite(blogId)).thenReturn(site)
        whenever(postStore.getPostByRemotePostId(remotePostId, site)).thenReturn(post)

        val navigateAction = editorLinkHandler.buildOpenEditorNavigateAction(uri)

        assertThat(navigateAction).isEqualTo(NavigateAction.OpenEditorForPost(site, localPostId))
        assertThat(toasts).isEmpty()
    }

    @Test
    fun `applink - opens editor for a site and shows toast when post not found`() {
        val uri = buildUri(
                host = "post",
                queryParam1 = "blogId" to blogId,
                queryParam2 = "postId" to remotePostId.toString()
        )
        whenever(deepLinkUriUtils.blogIdToSite(blogId)).thenReturn(site)
        whenever(postStore.getPostByRemotePostId(remotePostId, site)).thenReturn(null)

        val navigateAction = editorLinkHandler.buildOpenEditorNavigateAction(uri)

        assertThat(navigateAction).isEqualTo(NavigateAction.OpenEditorForSite(site))
        assertThat(toasts.last()).isEqualTo(R.string.post_not_found)
    }
}
