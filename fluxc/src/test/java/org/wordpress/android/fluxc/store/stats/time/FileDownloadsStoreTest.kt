package org.wordpress.android.fluxc.store.stats.time

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.FileDownloadsModel
import org.wordpress.android.fluxc.model.stats.time.TimeStatsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.FileDownloadsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.FileDownloadsRestClient.FileDownloadsResponse
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.persistence.TimeStatsSqlUtils.FileDownloadsSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.FetchStatsPayload
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.API_ERROR
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import org.wordpress.android.fluxc.test
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private const val ITEMS_TO_LOAD = 8
private val LIMIT_MODE = LimitMode.Top(ITEMS_TO_LOAD)
private val DATE = Date(0)

@RunWith(MockitoJUnitRunner::class)
class FileDownloadsStoreTest {
    @Mock lateinit var site: SiteModel
    @Mock lateinit var restClient: FileDownloadsRestClient
    @Mock lateinit var sqlUtils: FileDownloadsSqlUtils
    @Mock lateinit var mapper: TimeStatsMapper
    private lateinit var store: FileDownloadsStore
    @Before
    fun setUp() {
        store = FileDownloadsStore(
                restClient,
                sqlUtils,
                mapper,
                initCoroutineEngine()
        )
    }

    @Test
    fun `returns file downloads per site`() = test {
        val fetchInsightsPayload = FetchStatsPayload(
                FILE_DOWNLOADS_RESPONSE
        )
        val forced = true
        whenever(restClient.fetchFileDownloads(site, DAYS, DATE, ITEMS_TO_LOAD + 1, forced))
                .thenReturn(fetchInsightsPayload)
        val model = mock<FileDownloadsModel>()
        whenever(mapper.map(FILE_DOWNLOADS_RESPONSE, LIMIT_MODE)).thenReturn(model)

        val responseModel = store.fetchFileDownloads(site, DAYS, LIMIT_MODE, DATE, forced)

        assertThat(responseModel.model).isEqualTo(model)
        verify(sqlUtils).insert(site, FILE_DOWNLOADS_RESPONSE, DAYS, DATE, ITEMS_TO_LOAD)
    }

    @Test
    fun `returns cached data per site`() = test {
        whenever(sqlUtils.hasFreshRequest(site, DAYS, DATE, ITEMS_TO_LOAD)).thenReturn(true)
        whenever(sqlUtils.select(site, DAYS, DATE)).thenReturn(FILE_DOWNLOADS_RESPONSE)
        val model = mock<FileDownloadsModel>()
        whenever(mapper.map(FILE_DOWNLOADS_RESPONSE, LIMIT_MODE)).thenReturn(model)

        val forced = false
        val responseModel = store.fetchFileDownloads(site, DAYS, LIMIT_MODE, DATE, forced)

        assertThat(responseModel.model).isEqualTo(model)
        assertThat(responseModel.cached).isTrue()
        verify(sqlUtils, never()).insert(any(), any(), any(), any<Date>(), isNull())
    }

    @Test
    fun `returns error when file downloads call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchStatsPayload<FileDownloadsResponse>(StatsError(type, message))
        val forced = true
        whenever(restClient.fetchFileDownloads(site, DAYS, DATE, ITEMS_TO_LOAD + 1, forced)).thenReturn(errorPayload)

        val responseModel = store.fetchFileDownloads(site, DAYS, LIMIT_MODE, DATE, forced)

        assertNotNull(responseModel.error)
        val error = responseModel.error!!
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }

    @Test
    fun `returns file downloads from db`() {
        whenever(sqlUtils.select(site, DAYS, DATE)).thenReturn(FILE_DOWNLOADS_RESPONSE)
        val model = mock<FileDownloadsModel>()
        whenever(mapper.map(FILE_DOWNLOADS_RESPONSE, LIMIT_MODE)).thenReturn(model)

        val result = store.getFileDownloads(site, DAYS, LIMIT_MODE, DATE)

        assertThat(result).isEqualTo(model)
    }
}
