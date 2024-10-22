package org.wordpress.android.fluxc.persistance.stats.time

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.StatsUtils
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.VisitAndViewsRestClient.VisitsAndViewsResponse
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.fluxc.persistence.StatsRequestSqlUtils
import org.wordpress.android.fluxc.persistence.StatsSqlUtils
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.VISITS_AND_VIEWS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType.DAY
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType.MONTH
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType.WEEK
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType.YEAR
import org.wordpress.android.fluxc.persistence.TimeStatsSqlUtils.VisitsAndViewsSqlUtils
import org.wordpress.android.fluxc.store.stats.time.VISITS_AND_VIEWS_RESPONSE
import java.util.Date
import kotlin.test.assertEquals

private val DATE = Date(0)
private const val DATE_VALUE = "2018-10-10"

@RunWith(MockitoJUnitRunner::class)
class VisitAndViewsSqlUtilsTest {
    @Mock lateinit var statsSqlUtils: StatsSqlUtils
    @Mock lateinit var site: SiteModel
    @Mock lateinit var statsUtils: StatsUtils
    @Mock lateinit var statsRequestSqlUtils: StatsRequestSqlUtils
    private lateinit var timeStatsSqlUtils: VisitsAndViewsSqlUtils
    private val mappedTypes = mapOf(DAY to DAYS, WEEK to WEEKS, MONTH to MONTHS, YEAR to YEARS)

    @Before
    fun setUp() {
        timeStatsSqlUtils = VisitsAndViewsSqlUtils(statsSqlUtils, statsUtils, statsRequestSqlUtils)
        whenever(statsUtils.getFormattedDate(eq(DATE), isNull())).thenReturn(DATE_VALUE)
    }

    @Test
    fun `returns data from stats utils`() {
        mappedTypes.forEach { statsType, dbGranularity ->

            whenever(
                    statsSqlUtils.select(
                            site,
                            VISITS_AND_VIEWS,
                            statsType,
                            VisitsAndViewsResponse::class.java,
                            DATE_VALUE
                    )
            )
                    .thenReturn(
                            VISITS_AND_VIEWS_RESPONSE
                    )

            val result = timeStatsSqlUtils.select(site, dbGranularity, DATE)

            assertEquals(result, VISITS_AND_VIEWS_RESPONSE)
        }
    }

    @Test
    fun `inserts data to stats utils`() {
        mappedTypes.forEach { statsType, dbGranularity ->
            timeStatsSqlUtils.insert(site, VISITS_AND_VIEWS_RESPONSE, dbGranularity, DATE)

            verify(statsSqlUtils).insert(site, VISITS_AND_VIEWS, statsType, VISITS_AND_VIEWS_RESPONSE,
                    true, DATE_VALUE)
        }
    }
}
