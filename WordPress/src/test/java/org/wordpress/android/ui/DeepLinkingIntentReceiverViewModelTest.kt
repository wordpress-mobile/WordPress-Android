package org.wordpress.android.ui

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction

class DeepLinkingIntentReceiverViewModelTest : BaseUnitTest() {
    @Mock lateinit var editorLinkHandler: EditorLinkHandler
    @Mock lateinit var accountStore: AccountStore
    @Mock lateinit var deepLinkUriUtils: DeepLinkUriUtils
    @Mock lateinit var serverTrackingHandler: ServerTrackingHandler
    private lateinit var viewModel: DeepLinkingIntentReceiverViewModel

    @InternalCoroutinesApi
    @Before
    fun setUp() {
        viewModel = DeepLinkingIntentReceiverViewModel(
                editorLinkHandler,
                accountStore,
                deepLinkUriUtils,
                serverTrackingHandler,
                TEST_DISPATCHER
        )
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
    fun `create site mbar URL triggers the Site Creation flow when logged in`() {
        val uri = buildUri("public-api.wordpress.com", "mbar", "redirect_to=...")
        val firstRedirect = buildUri("wordpress.com", "wp-login.php", "redirect_to...")
        val secondRedirect = buildUri("wordpress.com", "start")
        whenever(deepLinkUriUtils.getUriFromQueryParameter(uri, "redirect_to")).thenReturn(firstRedirect)
        whenever(deepLinkUriUtils.getUriFromQueryParameter(firstRedirect, "redirect_to")).thenReturn(secondRedirect)
        var navigateAction: NavigateAction? = null
        viewModel.navigateAction.observeForever {
            navigateAction = it?.getContentIfNotHandled()
        }
        val isSignedIn = true
        whenever(accountStore.hasAccessToken()).thenReturn(isSignedIn)

        viewModel.handleEmailUrl(uri)

        assertThat(navigateAction).isEqualTo(NavigateAction.StartCreateSiteFlow)
        verify(serverTrackingHandler).request(uri)
    }

    @Test
    fun `create site mbar URL triggers the sign up flow when not logged in to WPCom account`() {
        val uri = buildUri("public-api.wordpress.com", "mbar", "redirect_to=...")
        val firstRedirect = buildUri("wordpress.com", "wp-login.php", "redirect_to...")
        val secondRedirect = buildUri("wordpress.com", "start")
        whenever(deepLinkUriUtils.getUriFromQueryParameter(uri, "redirect_to")).thenReturn(firstRedirect)
        whenever(deepLinkUriUtils.getUriFromQueryParameter(firstRedirect, "redirect_to")).thenReturn(secondRedirect)
        var navigateAction: NavigateAction? = null
        viewModel.navigateAction.observeForever {
            navigateAction = it?.getContentIfNotHandled()
        }
        val isSignedIn = false
        whenever(accountStore.hasAccessToken()).thenReturn(isSignedIn)

        viewModel.handleEmailUrl(uri)

        assertThat(navigateAction).isEqualTo(NavigateAction.ShowSignInFlow)
        verify(serverTrackingHandler).request(uri)
    }

    @Test
    fun `wp-login mbar URL redirects user to browser with missing second redirect`() {
        val uri = buildUri("public-api.wordpress.com", "mbar", "redirect_to=...")
        val redirect = buildUri("wordpress.com", "wp-login.php")
        whenever(deepLinkUriUtils.getUriFromQueryParameter(uri, "redirect_to")).thenReturn(redirect)
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
    fun `post mbar URL triggers the editor`() {
        val uri = buildUri("public-api.wordpress.com", "mbar", "redirect_to=...")
        val redirect = buildUri("wordpress.com", "post")
        whenever(deepLinkUriUtils.getUriFromQueryParameter(uri, "redirect_to")).thenReturn(redirect)
        val expectedAction = NavigateAction.OpenEditor
        whenever(editorLinkHandler.buildOpenEditorNavigateAction(redirect)).thenReturn(expectedAction)
        var navigateAction: NavigateAction? = null
        viewModel.navigateAction.observeForever {
            navigateAction = it?.getContentIfNotHandled()
        }

        viewModel.handleEmailUrl(uri)

        assertThat(navigateAction).isEqualTo(expectedAction)
        verify(serverTrackingHandler).request(uri)
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
    fun `opens navigate action from editor link handler`() {
        val siteUrl = "site123"
        val uri = buildUri("wordpress.com", "post", siteUrl)
        val expected = NavigateAction.OpenEditor
        whenever(editorLinkHandler.buildOpenEditorNavigateAction(uri)).thenReturn(expected)

        var navigateAction: NavigateAction? = null
        viewModel.navigateAction.observeForever {
            navigateAction = it?.getContentIfNotHandled()
        }

        viewModel.handleOpenEditor(uri)

        assertThat(navigateAction).isEqualTo(expected)
    }
}
