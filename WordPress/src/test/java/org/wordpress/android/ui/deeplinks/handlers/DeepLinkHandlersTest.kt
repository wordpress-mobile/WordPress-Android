package org.wordpress.android.ui.deeplinks.handlers

import androidx.lifecycle.MutableLiveData
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenEditor
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.viewmodel.Event

class DeepLinkHandlersTest : BaseUnitTest() {
    @Mock lateinit var editorLinkHandler: EditorLinkHandler
    @Mock lateinit var statsLinkHandler: StatsLinkHandler
    @Mock lateinit var startLinkHandler: StartLinkHandler
    @Mock lateinit var readerLinkHandler: ReaderLinkHandler
    @Mock lateinit var pagesLinkHandler: PagesLinkHandler
    @Mock lateinit var notificationsLinkHandler: NotificationsLinkHandler
    @Mock lateinit var qrCodeAuthLinkHandler: QRCodeAuthLinkHandler
    @Mock lateinit var homeLinkHandler: HomeLinkHandler
    @Mock lateinit var uri: UriWrapper
    private lateinit var deepLinkHandlers: DeepLinkHandlers
    private lateinit var handlers: List<DeepLinkHandler>

    @Before
    fun setUp() {
        handlers = listOf(
                editorLinkHandler,
                statsLinkHandler,
                startLinkHandler,
                readerLinkHandler,
                pagesLinkHandler,
                notificationsLinkHandler,
                qrCodeAuthLinkHandler,
                homeLinkHandler
        )
        initDeepLinkHandlers()
    }

    private fun initDeepLinkHandlers() {
        deepLinkHandlers = DeepLinkHandlers(
                editorLinkHandler,
                statsLinkHandler,
                startLinkHandler,
                readerLinkHandler,
                pagesLinkHandler,
                notificationsLinkHandler,
                qrCodeAuthLinkHandler,
                homeLinkHandler
        )
    }

    @Test
    fun `passed URI to handler to build navigate action`() {
        for (handler in handlers) {
            val expected = initBuildNavigateAction(handler)

            val navigateAction = deepLinkHandlers.buildNavigateAction(uri)

            assertThat(navigateAction).isEqualTo(expected)
        }
    }

    @Test
    fun `passed URI to handler to strip url`() {
        for (handler in handlers) {
            val expected = initStripUrl(handler)

            val strippedUrl = deepLinkHandlers.stripUrl(uri)

            assertThat(strippedUrl).isEqualTo(expected)
        }
    }

    @Test
    fun `shows toast from handler`() {
        for (handler in handlers) {
            val toast = MutableLiveData<Event<Int>>()
            whenever(handler.toast()).thenReturn(toast)
            initDeepLinkHandlers()
            var toastMessage: Int? = null
            deepLinkHandlers.toast.observeForever { event ->
                event?.getContentIfNotHandled()?.let {
                    toastMessage = it
                }
            }
            assertThat(toastMessage).isNull()
            val expected = 123
            toast.value = Event(expected)
            assertThat(toastMessage).isEqualTo(expected)
        }
    }

    private fun initBuildNavigateAction(deepLinkHandler: DeepLinkHandler): NavigateAction {
        whenever(deepLinkHandler.shouldHandleUrl(uri)).thenReturn(true)
        val expected = OpenEditor
        whenever(deepLinkHandler.buildNavigateAction(uri)).thenReturn(expected)
        return expected
    }

    private fun initStripUrl(deepLinkHandler: DeepLinkHandler): String {
        whenever(deepLinkHandler.shouldHandleUrl(uri)).thenReturn(true)
        val expected = "strippedUrl"
        whenever(deepLinkHandler.stripUrl(uri)).thenReturn(expected)
        return expected
    }
}
