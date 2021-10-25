package org.wordpress.android.fluxc.network.rest.wpcom.media.wpv2

import com.android.volley.RequestQueue
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.IOException
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.action.UploadAction.UPLOADED_MEDIA
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.media.MediaTestUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.MediaStore.ProgressPayload
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import java.io.File
import java.util.concurrent.CountDownLatch

@RunWith(RobolectricTestRunner::class)
class WPV2MediaRestClientTest {
    private val accessToken: AccessToken = mock()
    private val requestQueue: RequestQueue = mock()
    private val okHttpClient: OkHttpClient = mock()
    private val dispatcher: Dispatcher = mock()
    private val userAgent: UserAgent = mock()
    private val mockedCall: Call = mock()
    private lateinit var countDownLatch: CountDownLatch
    private lateinit var restClient: WPV2MediaRestClient

    private lateinit var dispatchedPayload: ProgressPayload

    @Before
    fun setup() {
        restClient = WPV2MediaRestClient(
                dispatcher = dispatcher,
                appContext = null,
                coroutineEngine = initCoroutineEngine(),
                okHttpClient = okHttpClient,
                requestQueue = requestQueue,
                accessToken = accessToken,
                userAgent = userAgent,
        )
        EventBus.getDefault().register(this)
    }

    @Test
    fun `emit success action when upload finishes`() {
        val file = File("./image.jpg")
        file.createNewFile()
        whenever(okHttpClient.newCall(any())).thenReturn(mockedCall)
        whenever(mockedCall.enqueue(any())).then {
            (it.arguments.first() as Callback).onResponse(
                    mockedCall,
                    mock {
                        on { body } doReturn UnitTestUtils.getStringFromResourceFile(
                                this::class.java,
                                "media/media-upload-wp-api-success.json"
                        ).toResponseBody("application/json".toMediaType())
                        on { isSuccessful } doReturn true
                    }
            )
            countDownLatch.countDown()
        }

        countDownLatch = CountDownLatch(1)
        restClient.uploadMedia(SiteModel(), MediaTestUtils.generateMediaFromPath(0, 0L, "./image.jpg"))

        countDownLatch.await()
        file.delete()

        verify(dispatcher).dispatch(argThat {
            type == UPLOADED_MEDIA && (payload as ProgressPayload).completed
        })
    }

    @Test
    fun `emit failure action when upload fails`() {
        val file = File("./image.jpg")
        file.createNewFile()
        whenever(okHttpClient.newCall(any())).thenReturn(mockedCall)
        whenever(mockedCall.enqueue(any())).then {
            (it.arguments.first() as Callback).onFailure(mock(), IOException())
            countDownLatch.countDown()
        }

        countDownLatch = CountDownLatch(1)
        restClient.uploadMedia(SiteModel(), MediaTestUtils.generateMediaFromPath(0, 0L, "./image.jpg"))

        countDownLatch.await()
        file.delete()

        verify(dispatcher).dispatch(argThat {
            type == UPLOADED_MEDIA && (payload as ProgressPayload).error != null
        })
    }

    @Test
    fun `emit failure action when we can't parse the response`() {
        val file = File("./image.jpg")
        file.createNewFile()
        whenever(okHttpClient.newCall(any())).thenReturn(mockedCall)
        whenever(mockedCall.enqueue(any())).then {
            (it.arguments.first() as Callback).onResponse(
                    mockedCall,
                    mock {
                        on { body } doReturn "".toResponseBody("application/json".toMediaType())
                        on { isSuccessful } doReturn true
                    }
            )
            countDownLatch.countDown()
        }

        countDownLatch = CountDownLatch(1)
        restClient.uploadMedia(SiteModel(), MediaTestUtils.generateMediaFromPath(0, 0L, "./image.jpg"))

        countDownLatch.await()
        file.delete()

        verify(dispatcher).dispatch(argThat {
            type == UPLOADED_MEDIA && (payload as ProgressPayload).error != null
        })
    }

    @Subscribe
    fun onAction(action: Action<*>) {
        if (action.type == UPLOADED_MEDIA) {
            dispatchedPayload = action.payload as ProgressPayload
        }
    }
}
