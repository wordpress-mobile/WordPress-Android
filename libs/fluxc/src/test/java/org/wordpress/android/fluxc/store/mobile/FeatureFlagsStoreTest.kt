package org.wordpress.android.fluxc.store.mobile

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.FeatureFlagsError
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.FeatureFlagsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.FeatureFlagsFetchedPayload
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.FeatureFlagsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.FeatureFlagsRestClient.FeatureFlagsPayload
import org.wordpress.android.fluxc.persistence.FeatureFlagConfigDao
import org.wordpress.android.fluxc.store.mobile.FeatureFlagsStore.FeatureFlagsResult
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(MockitoJUnitRunner::class)
class FeatureFlagsStoreTest {
    @Mock private lateinit var restClient: FeatureFlagsRestClient
    @Mock private lateinit var featureFlagConfigDao: FeatureFlagConfigDao
    private lateinit var store: FeatureFlagsStore

    private val successResponse = mapOf("flag-1" to true, "flag-2" to false)
    private val errorResponse = FeatureFlagsError( type = GENERIC_ERROR)
    private val errorResult = FeatureFlagsError( type = GENERIC_ERROR)


    @Before
    fun setUp() {
        store = FeatureFlagsStore(restClient, featureFlagConfigDao, initCoroutineEngine())
    }

    @Test
    fun `given success, when fetch f-flags is triggered, then result is returned`() = test {
        whenever(restClient.fetchFeatureFlags(any())).thenReturn(
            FeatureFlagsFetchedPayload(successResponse)
        )

        val response = store.fetchFeatureFlags(
            FeatureFlagsPayload(
                buildNumber = BUILD_NUMBER_PARAM,
                deviceId = DEVICE_ID_PARAM,
                identifier = IDENTIFIER_PARAM,
                marketingVersion = MARKETING_VERSION_PARAM,
                platform = PLATFORM_PARAM,
                osVersion = OS_VERSION_PARAM,
            )
        )

        verify(featureFlagConfigDao).insert(successResponse)
        assertNotNull(response.featureFlags)
        assertEquals(FeatureFlagsResult(successResponse), response)
    }

    @Test
    fun `given error, when f-flags is triggered, then error result is returned`() = test {
        whenever(restClient.fetchFeatureFlags(any())).thenReturn(
            FeatureFlagsFetchedPayload(errorResponse)
        )

        val response = store.fetchFeatureFlags(
            FeatureFlagsPayload(
                buildNumber = BUILD_NUMBER_PARAM,
                deviceId = DEVICE_ID_PARAM,
                identifier = IDENTIFIER_PARAM,
                marketingVersion = MARKETING_VERSION_PARAM,
                platform = PLATFORM_PARAM,
                osVersion = OS_VERSION_PARAM,
            )
        )

        verifyNoInteractions(featureFlagConfigDao)
        assertNull(response.featureFlags)
        assertEquals(FeatureFlagsResult(errorResult), response)
    }

    companion object {
        private const val BUILD_NUMBER_PARAM = "build_number_param"
        private const val DEVICE_ID_PARAM = "device_id_param"
        private const val IDENTIFIER_PARAM = "identifier_param"
        private const val MARKETING_VERSION_PARAM = "marketing_version_param"
        private const val PLATFORM_PARAM = "platform_param"
        private const val OS_VERSION_PARAM = "os_version_param"
    }
}
