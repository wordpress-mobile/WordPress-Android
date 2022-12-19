package org.wordpress.android.models.recommend

import android.content.Context
import com.wordpress.rest.RestRequest
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.startsWith
import org.mockito.Mock
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.models.recommend.RecommendApiCallsProvider.RecommendAppName.WordPress
import org.wordpress.android.models.recommend.RecommendApiCallsProvider.RecommendCallResult.Failure
import org.wordpress.android.networking.RestClientUtils
import org.wordpress.android.test
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.RestClientProvider
import org.wordpress.android.util.analytics.AnalyticsUtils.RecommendAppSource.ME
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.android.viewmodel.ContextProvider

class RecommendApiCallsProviderTest : BaseUnitTest() {
    @Mock lateinit var contextProvider: ContextProvider
    @Mock lateinit var context: Context
    @Mock lateinit var analyticsUtilsWrapper: AnalyticsUtilsWrapper
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var restClientProvider: RestClientProvider
    @Mock lateinit var restClientUtils: RestClientUtils
    @Mock lateinit var localeManagerWrapper: LocaleManagerWrapper
    @Mock lateinit var jsonObject: JSONObject

    private lateinit var callsProvider: RecommendApiCallsProvider
    private lateinit var listenerCaptor: KArgumentCaptor<RestRequest.Listener>
    private lateinit var errorListenerCaptor: KArgumentCaptor<RestRequest.ErrorListener>

    @Before
    fun setUp() {
        callsProvider = RecommendApiCallsProvider(
                contextProvider,
                analyticsUtilsWrapper,
                networkUtilsWrapper,
                restClientProvider,
                localeManagerWrapper
        )
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(contextProvider.getContext()).thenReturn(context)
        whenever(restClientProvider.getRestClientUtilsV2()).thenReturn(restClientUtils)
        whenever(localeManagerWrapper.getLanguage()).thenReturn("en")
        whenever(jsonObject.optString("name")).thenReturn("wordpress")

        listenerCaptor = argumentCaptor()
        errorListenerCaptor = argumentCaptor()
    }

    @Test
    fun `error is returned when no network`() = test {
        val error = "No Network"
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
        whenever(context.getString(R.string.no_network_message)).thenReturn(error)

        val result = callsProvider.getRecommendTemplate(WordPress.appName, ME)

        assertThat(result).isEqualTo(Failure(error))
    }

    @Test
    fun `error is tracked when no network`() = test {
        val error = "No Network"
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
        whenever(context.getString(R.string.no_network_message)).thenReturn(error)

        callsProvider.getRecommendTemplate(WordPress.appName, ME)

        verify(analyticsUtilsWrapper, times(1)).trackRecommendAppFetchFailed(
                eq(ME),
                eq("getRecommendTemplate > No Network available")
        )
    }

    @Test
    fun `error is tracked on parse error`() = test {
        val error = "Invalid response received"
        whenever(jsonObject.toString()).thenReturn("{name:\"wordpress\",message=[]}")
        whenever(context.getString(R.string.recommend_app_bad_format_response)).thenReturn(error)
        whenever(restClientUtils.get(anyString(), listenerCaptor.capture(), errorListenerCaptor.capture())).doAnswer {
            listenerCaptor.lastValue.onResponse(jsonObject)
            null
        }

        callsProvider.getRecommendTemplate(WordPress.appName, ME)

        verify(analyticsUtilsWrapper, times(1)).trackRecommendAppFetchFailed(
                eq(ME),
                startsWith("getTemplateFromJson > Error parsing server API")
        )
    }

    @Test
    fun `error is tracked on wrong app name`() = test {
        val error = "Invalid response received"
        whenever(jsonObject.optString("name")).thenReturn("jetpack")
        whenever(context.getString(R.string.recommend_app_bad_format_response)).thenReturn(error)
        whenever(restClientUtils.get(anyString(), listenerCaptor.capture(), errorListenerCaptor.capture())).doAnswer {
            listenerCaptor.lastValue.onResponse(jsonObject)
            null
        }

        callsProvider.getRecommendTemplate(WordPress.appName, ME)

        verify(analyticsUtilsWrapper, times(1)).trackRecommendAppFetchFailed(
                eq(ME),
                startsWith("getTemplateFromJson > wrong app name received: ")
        )
    }

    @Test
    fun `error is tracked on null net response`() = test {
        val error = "No response received"
        whenever(context.getString(R.string.recommend_app_null_response)).thenReturn(error)
        whenever(restClientUtils.get(anyString(), listenerCaptor.capture(), errorListenerCaptor.capture())).doAnswer {
            listenerCaptor.lastValue.onResponse(null)
            null
        }

        callsProvider.getRecommendTemplate(WordPress.appName, ME)

        verify(analyticsUtilsWrapper, times(1)).trackRecommendAppFetchFailed(
                eq(ME),
                eq("getTemplateFromJson > null response received")
        )
    }

    @Test
    fun `error is tracked on volley error`() = test {
        val error = "Unknown error fetching recommend app template"
        whenever(context.getString(R.string.recommend_app_generic_get_template_error)).thenReturn(error)
        whenever(restClientUtils.get(anyString(), listenerCaptor.capture(), errorListenerCaptor.capture())).doAnswer {
            errorListenerCaptor.lastValue.onErrorResponse(mock())
            null
        }

        callsProvider.getRecommendTemplate(WordPress.appName, ME)

        verify(analyticsUtilsWrapper, times(1)).trackRecommendAppFetchFailed(
                eq(ME),
                startsWith("getRecommendTemplate > Failed with empty string [volleyError = ")
        )
    }
}
