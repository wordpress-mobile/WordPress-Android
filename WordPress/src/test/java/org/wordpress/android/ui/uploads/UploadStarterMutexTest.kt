package org.wordpress.android.ui.uploads

import android.content.Context
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.store.PageStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.helpers.ConnectionStatus
import java.util.concurrent.Executors

/**
 * This class tries to reproduce the situation of trying to start the upload service when the device is in the
 * background. Such situation causes the system to throw a IllegalStateException, as it's not allowed to create
 * services from the background, which in turns causes a failure in the coroutine jobs, which should be cancelled
 * without crashing the application.
 *
 * This was created because the situation above was causing a crash in our app:
 * https://a8c.sentry.io/issues/4295575735/?project=573168
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UploadStarterMutexTest : BaseUnitTest() {
    private val bgDispatcher = Executors.newFixedThreadPool(DEFAULT_THREADS).asCoroutineDispatcher()
    private val ioDispatcher = Executors.newFixedThreadPool(IO_THREADS).asCoroutineDispatcher()

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var dispatcher: Dispatcher

    @Mock
    lateinit var postStore: PostStore

    @Mock
    lateinit var pageStore: PageStore

    @Mock
    lateinit var siteStore: SiteStore

    @Mock
    lateinit var uploadActionUseCase: UploadActionUseCase

    @Mock
    lateinit var tracker: AnalyticsTrackerWrapper

    @Mock
    lateinit var uploadServiceFacade: UploadServiceFacade

    @Mock
    lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    private var connectionStatus: MutableLiveData<ConnectionStatus> = MutableLiveData()

    private lateinit var uploadStarter: UploadStarter

    @Before
    fun setUp() {
        uploadStarter = UploadStarter(
            context = context,
            dispatcher = dispatcher,
            postStore = postStore,
            pageStore = pageStore,
            siteStore = siteStore,
            uploadActionUseCase = uploadActionUseCase,
            tracker = tracker,
            bgDispatcher = bgDispatcher,
            ioDispatcher = ioDispatcher,
            uploadServiceFacade = uploadServiceFacade,
            networkUtilsWrapper = networkUtilsWrapper,
            connectionStatus = connectionStatus
        )
    }

    @Test
    fun `should not crash if uploadPost throws an exception`() = test {
        // network is available
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        // return 10 sites for queuing
        whenever(siteStore.sites).thenReturn(List(10) { mock() })
        // each site has 10 posts with local changes
        whenever(postStore.getPostsWithLocalChanges(any())).doAnswer {
            Thread.sleep(10)
            List(10) { fakePostModel(it, false) }
        }
        // each site has 5 pages with local changes
        whenever(pageStore.getPagesWithLocalChanges(any())).doSuspendableAnswer {
            delay(10)
            List(5) {
                fakePostModel(
                    100 + it,
                    true
                )
            }
        }

        whenever(uploadActionUseCase.getAutoUploadAction(any(), any()))
            .thenReturn(UploadActionUseCase.UploadAction.UPLOAD)

        // throw IllegalStateException when uploading post (emulate trying to start background service)
        whenever(uploadServiceFacade.uploadPost(any(), any<PostModel>(), any()))
            .thenThrow(IllegalStateException("FAKE: Not allowed to start service intent"))

        // ACT
        uploadStarter.queueUploadFromAllSites().join()
    }

    companion object {
        // # of threads in Dispatchers.Default is equal to the # of CPU cores, 8 is a common # of cores in modern phones
        // https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-dispatchers/-default.html
        private const val DEFAULT_THREADS = 8

        // default # of threads in Dispatchers.IO is 64 but it can be even larger, let's use 64, which is enough
        // https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-dispatchers/-i-o.html
        private const val IO_THREADS = 64

        private fun fakePostModel(
            id: Int,
            isPage: Boolean,
        ) = PostModel().apply {
            setId(id)
            setIsPage(isPage)
            setTitle("Title $id")
            setStatus("draft")
        }
    }
}
