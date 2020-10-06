package org.wordpress.android.fluxc.persistance.stats.time

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ReferrersRestClient.ReferrersResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.StatsUtils
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.fluxc.persistence.StatsRequestSqlUtils
import org.wordpress.android.fluxc.persistence.StatsSqlUtils
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.REFERRERS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType.DAY
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType.MONTH
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType.WEEK
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType.YEAR
import org.wordpress.android.fluxc.persistence.TimeStatsSqlUtils.ReferrersSqlUtils
import org.wordpress.android.fluxc.store.stats.time.REFERRERS_RESPONSE
import java.util.Date
import kotlin.test.assertEquals

private val DATE = Date(0)
private const val DATE_VALUE = "2018-10-10"

@RunWith(MockitoJUnitRunner::class)
class ReferrersSqlUtilsTest {
    @Mock lateinit var statsSqlUtils: StatsSqlUtils
    @Mock lateinit var statsUtils: StatsUtils
    @Mock lateinit var statsRequestSqlUtils: StatsRequestSqlUtils
    @Mock lateinit var site: SiteModel
    private lateinit var timeStatsSqlUtils: ReferrersSqlUtils
    private val mappedTypes = mapOf(DAY to DAYS, WEEK to WEEKS, MONTH to MONTHS, YEAR to YEARS)

    @Before
    fun setUp() {
        timeStatsSqlUtils = ReferrersSqlUtils(statsSqlUtils, statsUtils, statsRequestSqlUtils)
        whenever(statsUtils.getFormattedDate(eq(DATE), isNull())).thenReturn(DATE_VALUE)
    }

    @Test
    fun `returns referrers from stats utils`() {
        mappedTypes.forEach { statsType, dbGranularity ->

            whenever(statsSqlUtils.select(site, REFERRERS, statsType, ReferrersResponse::class.java, DATE_VALUE))
                    .thenReturn(
                            REFERRERS_RESPONSE
                    )

            val result = timeStatsSqlUtils.select(site, dbGranularity, DATE)

            assertEquals(result, REFERRERS_RESPONSE)
        }
    }

    @Test
    fun `inserts referrers to stats utils`() {
        mappedTypes.forEach { statsType, dbGranularity ->
            timeStatsSqlUtils.insert(site, REFERRERS_RESPONSE, dbGranularity, DATE)

            verify(statsSqlUtils).insert(site, REFERRERS, statsType, REFERRERS_RESPONSE,
                    true, DATE_VALUE)
        }
    }
}
