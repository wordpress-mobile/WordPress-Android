package org.wordpress.android.fluxc.store.blaze

import junit.framework.TestCase.assertEquals
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeStatusError
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeStatusErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeStatusFetchedPayload
import org.wordpress.android.fluxc.persistence.blaze.BlazeStatusDao
import org.wordpress.android.fluxc.persistence.blaze.BlazeStatusDao.BlazeStatus
import org.wordpress.android.fluxc.store.blaze.BlazeStore.BlazeStatusResult
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@RunWith(MockitoJUnitRunner::class)
class BlazeStoreTest {
    @Mock private lateinit var restClient: BlazeRestClient
    @Mock private lateinit var dao: BlazeStatusDao
    @Mock private lateinit var siteModel: SiteModel
    private lateinit var store: BlazeStore

    private val successResponse = mapOf("approved" to true)
    private val statusResult = BlazeStatus(SITE_ID,true)
    private val errorResponse = BlazeStatusError( type = GENERIC_ERROR)
    private val errorResult = BlazeStatusError( type = GENERIC_ERROR)

    @Before
    fun setUp() {
        store = BlazeStore(restClient, dao, initCoroutineEngine())
        whenever(siteModel.siteId).thenReturn(SITE_ID)
    }

    @Test
    fun `given success, when fetch blaze status is triggered, then status are inserted`() = test {
        whenever(restClient.fetchBlazeStatus(any())).thenReturn(
            BlazeStatusFetchedPayload(
                SITE_ID,
                successResponse
            )
        )

        val response = store.fetchBlazeStatus(siteModel)

        verify(dao).insert(statusResult)
        assertThat(response.isEligible).isTrue
    }

    @Test
    fun `given error, when fetch blaze status is triggered, then error result is returned`() =
        test {
            whenever(restClient.fetchBlazeStatus(any())).thenReturn(
                BlazeStatusFetchedPayload(
                    SITE_ID,
                    errorResponse
                )
            )
            val response = store.fetchBlazeStatus(siteModel)

            verifyNoInteractions(dao)
            assertThat(response.isEligible).isFalse
            assertEquals(BlazeStatusResult(errorResult), response)
        }

    @Test
    fun `given site not in db, when is eligible for blaze is triggered, then false is returned`() {
        whenever(dao.getBlazeStatus(SITE_ID)).thenReturn(emptyList())

        val result = store.isSiteEligibleForBlaze(SITE_ID)

        assertThat(result).isFalse
    }

    @Test
    fun `when clear is requested, then the dao request is triggered`() {
        store.clear()

        verify(dao).clear()
    }

    companion object {
        private const val SITE_ID = 1234L
    }
}
