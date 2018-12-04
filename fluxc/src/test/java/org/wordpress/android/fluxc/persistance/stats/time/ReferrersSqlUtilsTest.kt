package org.wordpress.android.fluxc.persistance.stats.time

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ReferrersRestClient.ReferrersResponse
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.REFERRERS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType.DAY
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType.MONTH
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType.WEEK
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType.YEAR
import org.wordpress.android.fluxc.persistence.TimeStatsSqlUtils
import org.wordpress.android.fluxc.store.stats.time.REFERRERS_RESPONSE
import kotlin.test.assertEquals

@RunWith(MockitoJUnitRunner::class)
class ReferrersSqlUtilsTest {
    @Mock lateinit var statsSqlUtils: StatsSqlUtils
    @Mock lateinit var site: SiteModel
    private lateinit var timeStatsSqlUtils: TimeStatsSqlUtils

    @Before
    fun setUp() {
        timeStatsSqlUtils = TimeStatsSqlUtils(statsSqlUtils)
    }

    @Test
    fun `returns referrers by day from stats utils`() {
        whenever(statsSqlUtils.select(site, REFERRERS, DAY, ReferrersResponse::class.java))
                .thenReturn(
                        REFERRERS_RESPONSE
                )

        val result = timeStatsSqlUtils.selectReferrers(site, DAYS)

        assertEquals(result, REFERRERS_RESPONSE)
    }

    @Test
    fun `returns referrers by week from stats utils`() {
        whenever(statsSqlUtils.select(site, REFERRERS, WEEK, ReferrersResponse::class.java))
                .thenReturn(
                        REFERRERS_RESPONSE
                )

        val result = timeStatsSqlUtils.selectReferrers(site, WEEKS)

        assertEquals(result, REFERRERS_RESPONSE)
    }

    @Test
    fun `returns referrers by month from stats utils`() {
        whenever(statsSqlUtils.select(site, REFERRERS, MONTH, ReferrersResponse::class.java))
                .thenReturn(
                        REFERRERS_RESPONSE
                )

        val result = timeStatsSqlUtils.selectReferrers(site, MONTHS)

        assertEquals(result, REFERRERS_RESPONSE)
    }

    @Test
    fun `returns referrers by year from stats utils`() {
        whenever(statsSqlUtils.select(site, REFERRERS, YEAR, ReferrersResponse::class.java))
                .thenReturn(
                        REFERRERS_RESPONSE
                )

        val result = timeStatsSqlUtils.selectReferrers(site, YEARS)

        assertEquals(result, REFERRERS_RESPONSE)
    }

    @Test
    fun `inserts referrers by day to stats utils`() {
        timeStatsSqlUtils.insert(site,
                REFERRERS_RESPONSE, DAYS)

        verify(statsSqlUtils).insert(site, REFERRERS, DAY,
                REFERRERS_RESPONSE
        )
    }

    @Test
    fun `inserts referrers by week to stats utils`() {
        timeStatsSqlUtils.insert(site,
                REFERRERS_RESPONSE, WEEKS)

        verify(statsSqlUtils).insert(site, REFERRERS, WEEK,
                REFERRERS_RESPONSE
        )
    }

    @Test
    fun `inserts referrers by month to stats utils`() {
        timeStatsSqlUtils.insert(site,
                REFERRERS_RESPONSE, MONTHS)

        verify(statsSqlUtils).insert(site, REFERRERS, MONTH,
                REFERRERS_RESPONSE
        )
    }

    @Test
    fun `inserts referrers by year to stats utils`() {
        timeStatsSqlUtils.insert(site,
                REFERRERS_RESPONSE, YEARS)

        verify(statsSqlUtils).insert(site, REFERRERS, YEAR,
                REFERRERS_RESPONSE
        )
    }
}
