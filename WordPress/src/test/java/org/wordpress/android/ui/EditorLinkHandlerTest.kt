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
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.DeeplinkNavigator.NavigateAction

class EditorLinkHandlerTest : BaseUnitTest() {
    @Mock lateinit var deepLinkUriUtils: DeepLinkUriUtils
    @Mock lateinit var siteStore: SiteStore
    @Mock lateinit var postStore: PostStore
    private lateinit var editorLinkHandler: EditorLinkHandler
    private lateinit var site: SiteModel
    private lateinit var post: PostModel
    private val siteUrl = "site123"
    private val remotePostId = 123L
    private val localPostId = 1

    @Before
    fun setUp() {
        editorLinkHandler = EditorLinkHandler(deepLinkUriUtils, siteStore, postStore)
        site = SiteModel()
        site.url = siteUrl
        post = PostModel()
        post.setRemotePostId(remotePostId)
        post.setId(localPostId)
    }

    @Test
    fun `opens editor when site not found`() {
        val siteUrl = "site123"
        val uri = buildUri(path1 = "post", path2 = siteUrl)
        whenever(siteStore.getSitesByNameOrUrlMatching(siteUrl)).thenReturn(listOf())

        val navigateAction = editorLinkHandler.buildOpenEditorNavigateAction(uri)

        assertThat(navigateAction).isEqualTo(NavigateAction.OpenEditor)
    }

    @Test
    fun `opens editor for a site site when post missing in URL`() {
        val uri = buildUri(path1 = "post", path2 = siteUrl)
        whenever(deepLinkUriUtils.extractHostFromSite(site)).thenReturn(siteUrl)
        whenever(siteStore.getSitesByNameOrUrlMatching(siteUrl)).thenReturn(listOf(site))

        val navigateAction = editorLinkHandler.buildOpenEditorNavigateAction(uri)

        assertThat(navigateAction).isEqualTo(NavigateAction.OpenEditorForSite(site))
    }

    @Test
    fun `opens editor for a post when both site and post exist`() {
        val uri = buildUri(path1 = "post", path2 = siteUrl, path3 = remotePostId.toString())
        whenever(deepLinkUriUtils.extractHostFromSite(site)).thenReturn(siteUrl)
        whenever(siteStore.getSitesByNameOrUrlMatching(siteUrl)).thenReturn(listOf(site))
        whenever(postStore.getPostByRemotePostId(remotePostId, site)).thenReturn(post)

        val navigateAction = editorLinkHandler.buildOpenEditorNavigateAction(uri)

        assertThat(navigateAction).isEqualTo(NavigateAction.OpenInEditor(site, localPostId))
    }

    @Test
    fun `opens editor for a site site when post not found`() {
        val uri = buildUri(path1 = "post", path2 = siteUrl, path3 = remotePostId.toString())
        whenever(deepLinkUriUtils.extractHostFromSite(site)).thenReturn(siteUrl)
        whenever(siteStore.getSitesByNameOrUrlMatching(siteUrl)).thenReturn(listOf(site))
        whenever(postStore.getPostByRemotePostId(remotePostId, site)).thenReturn(null)

        val navigateAction = editorLinkHandler.buildOpenEditorNavigateAction(uri)

        assertThat(navigateAction).isEqualTo(NavigateAction.OpenEditorForSite(site))
    }
}
