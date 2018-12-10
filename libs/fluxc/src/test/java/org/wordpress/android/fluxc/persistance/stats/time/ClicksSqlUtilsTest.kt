package org.wordpress.android.fluxc.persistance.stats.time

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ClicksRestClient.ClicksResponse
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.CLICKS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType.DAY
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType.MONTH
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType.WEEK
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType.YEAR
import org.wordpress.android.fluxc.persistence.TimeStatsSqlUtils
import org.wordpress.android.fluxc.store.stats.time.CLICKS_RESPONSE
import kotlin.test.assertEquals

@RunWith(MockitoJUnitRunner::class)
class ClicksSqlUtilsTest {
    @Mock lateinit var statsSqlUtils: StatsSqlUtils
    @Mock lateinit var site: SiteModel
    private lateinit var timeStatsSqlUtils: TimeStatsSqlUtils
    private val mappedTypes = mapOf(DAY to DAYS, WEEK to WEEKS, MONTH to MONTHS, YEAR to YEARS)

    @Before
    fun setUp() {
        timeStatsSqlUtils = TimeStatsSqlUtils(statsSqlUtils)
    }

    @Test
    fun `returns referrers from stats utils`() {
        mappedTypes.forEach { statsType, dbGranularity ->

            whenever(statsSqlUtils.select(site, CLICKS, statsType, ClicksResponse::class.java))
                    .thenReturn(
                            CLICKS_RESPONSE
                    )

            val result = timeStatsSqlUtils.selectClicks(site, dbGranularity)

            assertEquals(result, CLICKS_RESPONSE)
        }
    }

    @Test
    fun `inserts referrers to stats utils`() {
        mappedTypes.forEach { statsType, dbGranularity ->
            timeStatsSqlUtils.insert(site, CLICKS_RESPONSE, dbGranularity)

            verify(statsSqlUtils).insert(site, CLICKS, statsType, CLICKS_RESPONSE)
        }
    }
}
