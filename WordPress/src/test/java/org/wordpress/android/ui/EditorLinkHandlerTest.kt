package org.wordpress.android.ui

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction

class EditorLinkHandlerTest : BaseUnitTest() {
    @Mock lateinit var deepLinkUriUtils: DeepLinkUriUtils
    @Mock lateinit var postStore: PostStore
    private lateinit var editorLinkHandler: EditorLinkHandler
    private lateinit var site: SiteModel
    private lateinit var post: PostModel
    private val siteUrl = "site123"
    private val remotePostId = 123L
    private val localPostId = 1

    @Before
    fun setUp() {
        editorLinkHandler = EditorLinkHandler(deepLinkUriUtils, postStore)
        site = SiteModel()
        site.url = siteUrl
        post = PostModel()
        post.setRemotePostId(remotePostId)
        post.setId(localPostId)
    }

    @Test
    fun `handles post URI is true`() {
        val postUri = buildUri("wordpress.com", "post")

        val isEditorUri = editorLinkHandler.isEditorUrl(postUri)

        assertThat(isEditorUri).isTrue()
    }

    @Test
    fun `does not handle post URI with different host`() {
        val postUri = buildUri("wordpress.org", "post")

        val isEditorUri = editorLinkHandler.isEditorUrl(postUri)

        assertThat(isEditorUri).isFalse()
    }

    @Test
    fun `does not handle URI with different path`() {
        val postUri = buildUri("wordpress.com", "stats")

        val isEditorUri = editorLinkHandler.isEditorUrl(postUri)

        assertThat(isEditorUri).isFalse()
    }

    @Test
    fun `opens editor when site not found`() {
        val uri = buildUri(path1 = "post", path2 = siteUrl)

        val navigateAction = editorLinkHandler.buildOpenEditorNavigateAction(uri)

        assertThat(navigateAction).isEqualTo(NavigateAction.OpenEditor)
    }

    @Test
    fun `opens editor for a site site when post missing in URL`() {
        val uri = buildUri(path1 = "post", path2 = siteUrl)
        whenever(deepLinkUriUtils.hostToSite(siteUrl)).thenReturn(site)

        val navigateAction = editorLinkHandler.buildOpenEditorNavigateAction(uri)

        assertThat(navigateAction).isEqualTo(NavigateAction.OpenEditorForSite(site))
    }

    @Test
    fun `opens editor for a post when both site and post exist`() {
        val uri = buildUri(path1 = "post", path2 = siteUrl, path3 = remotePostId.toString())
        whenever(deepLinkUriUtils.hostToSite(siteUrl)).thenReturn(site)
        whenever(postStore.getPostByRemotePostId(remotePostId, site)).thenReturn(post)

        val navigateAction = editorLinkHandler.buildOpenEditorNavigateAction(uri)

        assertThat(navigateAction).isEqualTo(NavigateAction.OpenEditorForPost(site, localPostId))
    }

    @Test
    fun `opens editor for a site site when post not found`() {
        val uri = buildUri(path1 = "post", path2 = siteUrl, path3 = remotePostId.toString())
        whenever(deepLinkUriUtils.hostToSite(siteUrl)).thenReturn(site)
        whenever(postStore.getPostByRemotePostId(remotePostId, site)).thenReturn(null)

        val navigateAction = editorLinkHandler.buildOpenEditorNavigateAction(uri)

        assertThat(navigateAction).isEqualTo(NavigateAction.OpenEditorForSite(site))
    }
}
