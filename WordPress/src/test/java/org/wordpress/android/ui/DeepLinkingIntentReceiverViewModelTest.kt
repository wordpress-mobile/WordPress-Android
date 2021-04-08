package org.wordpress.android.ui

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.DeeplinkNavigator.NavigateAction
import org.wordpress.android.util.UriWrapper

class DeepLinkingIntentReceiverViewModelTest : BaseUnitTest() {
    @Mock lateinit var siteStore: SiteStore
    @Mock lateinit var postStore: PostStore
    @Mock lateinit var accountStore: AccountStore
    @Mock lateinit var deepLinkUriUtils: DeepLinkUriUtils
    private lateinit var viewModel: DeepLinkingIntentReceiverViewModel
    private lateinit var site: SiteModel
    private lateinit var post: PostModel
    private val siteUrl = "site123"
    private val remotePostId = 123L
    private val localPostId = 1

    @InternalCoroutinesApi
    @Before
    fun setUp() {
        viewModel = DeepLinkingIntentReceiverViewModel(
                siteStore,
                postStore,
                accountStore,
                deepLinkUriUtils,
                TEST_DISPATCHER
        )
        site = SiteModel()
        site.url = siteUrl
        post = PostModel()
        post.setRemotePostId(remotePostId)
        post.setId(localPostId)
    }

    @Test
    fun `should handle email mbar mobile URL`() {
        val uri = buildUri("public-api.wordpress.com", "mbar")

        val shouldHandleUri = viewModel.shouldHandleEmailUrl(uri)

        assertThat(shouldHandleUri).isTrue()
    }

    @Test
    fun `should not handle WPcom URL`() {
        val uri = buildUri("wordpress.com", "bar")

        val shouldHandleUri = viewModel.shouldHandleEmailUrl(uri)

        assertThat(shouldHandleUri).isFalse()
    }

    @Test
    fun `should not handle bar non-mobile URL`() {
        val uri = buildUri("public-api.wordpress.com", "bar")

        val shouldHandleUri = viewModel.shouldHandleEmailUrl(uri)

        assertThat(shouldHandleUri).isFalse()
    }

    @Test
    fun `magic login URL opens the URI in the browser without redirect parameter`() {
        val uri = buildUri("public-api.wordpress.com", "mbar")
        var navigateAction: NavigateAction? = null
        viewModel.navigateAction.observeForever {
            navigateAction = it?.getContentIfNotHandled()
        }
        val barUri = buildUri("public-api.wordpress.com", "bar")
        whenever(uri.copy("bar")).thenReturn(barUri)

        viewModel.handleEmailUrl(uri)

        assertThat(navigateAction).isEqualTo(NavigateAction.OpenInBrowser(barUri))
    }

    @Test
    fun `should handle post url`() {
        val uri = buildUri("wordpress.com", "post")

        val shouldHandleUri = viewModel.shouldOpenEditor(uri)

        assertThat(shouldHandleUri).isTrue()
    }

    @Test
    fun `does not handle pages url`() {
        val uri = buildUri("wordpress.com", "pages")

        val shouldHandleUri = viewModel.shouldOpenEditor(uri)

        assertThat(shouldHandleUri).isFalse()
    }

    @Test
    fun `does not handle app link to posts`() {
        val uri = buildUri("pages", "")

        val shouldHandleUri = viewModel.shouldOpenEditor(uri)

        assertThat(shouldHandleUri).isFalse()
    }

    @Test
    fun `opens editor when site is missing from URL`() {
        val uri = buildUri("wordpress.com", "post")

        var navigateAction: NavigateAction? = null
        viewModel.navigateAction.observeForever {
            navigateAction = it?.getContentIfNotHandled()
        }

        viewModel.handleOpenEditor(uri)

        assertThat(navigateAction).isEqualTo(NavigateAction.OpenEditor)
    }

    @Test
    fun `opens editor when site not found`() {
        val siteUrl = "site123"
        val uri = buildUri("wordpress.com", "post", siteUrl)
        whenever(siteStore.getSitesByNameOrUrlMatching(siteUrl)).thenReturn(listOf())

        var navigateAction: NavigateAction? = null
        viewModel.navigateAction.observeForever {
            navigateAction = it?.getContentIfNotHandled()
        }

        viewModel.handleOpenEditor(uri)

        assertThat(navigateAction).isEqualTo(NavigateAction.OpenEditor)
    }

    @Test
    fun `opens editor for a site site when post missing in URL`() {
        val uri = buildUri("wordpress.com", "post", siteUrl)
        whenever(deepLinkUriUtils.extractHostFromSite(site)).thenReturn(siteUrl)
        whenever(siteStore.getSitesByNameOrUrlMatching(siteUrl)).thenReturn(listOf(site))

        var navigateAction: NavigateAction? = null
        viewModel.navigateAction.observeForever {
            navigateAction = it?.getContentIfNotHandled()
        }

        viewModel.handleOpenEditor(uri)

        assertThat(navigateAction).isEqualTo(NavigateAction.OpenEditorForSite(site))
    }

    @Test
    fun `opens editor for a post when both site and post exist`() {
        val uri = buildUri("wordpress.com", "post", siteUrl, remotePostId.toString())
        whenever(deepLinkUriUtils.extractHostFromSite(site)).thenReturn(siteUrl)
        whenever(siteStore.getSitesByNameOrUrlMatching(siteUrl)).thenReturn(listOf(site))
        whenever(postStore.getPostByRemotePostId(remotePostId, site)).thenReturn(post)

        var navigateAction: NavigateAction? = null
        viewModel.navigateAction.observeForever {
            navigateAction = it?.getContentIfNotHandled()
        }

        viewModel.handleOpenEditor(uri)

        assertThat(navigateAction).isEqualTo(NavigateAction.OpenInEditor(site, localPostId))
    }

    @Test
    fun `opens editor for a site site when post not found`() {
        val uri = buildUri("wordpress.com", "post", siteUrl, remotePostId.toString())
        whenever(deepLinkUriUtils.extractHostFromSite(site)).thenReturn(siteUrl)
        whenever(siteStore.getSitesByNameOrUrlMatching(siteUrl)).thenReturn(listOf(site))
        whenever(postStore.getPostByRemotePostId(remotePostId, site)).thenReturn(null)

        var navigateAction: NavigateAction? = null
        viewModel.navigateAction.observeForever {
            navigateAction = it?.getContentIfNotHandled()
        }

        viewModel.handleOpenEditor(uri)

        assertThat(navigateAction).isEqualTo(NavigateAction.OpenEditorForSite(site))
    }

    private fun buildUri(host: String, vararg path: String): UriWrapper {
        val uri = mock<UriWrapper>()
        whenever(uri.host).thenReturn(host)
        whenever(uri.pathSegments).thenReturn(path.toList())
        return uri
    }
}
