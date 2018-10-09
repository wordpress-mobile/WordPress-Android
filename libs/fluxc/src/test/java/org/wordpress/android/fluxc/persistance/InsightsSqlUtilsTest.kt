package org.wordpress.android.fluxc.persistance

import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils
import org.wordpress.android.fluxc.persistence.StatsSqlUtils

@RunWith(MockitoJUnitRunner::class)
class InsightsSqlUtilsTest {
    @Mock lateinit var statsSqlUtils: StatsSqlUtils
    private lateinit var insightsSqlUtils: InsightsSqlUtils

    @Before
    fun setUp() {
        insightsSqlUtils = InsightsSqlUtils(statsSqlUtils)
    }

    fun `returns all time response from stats utils`() {
        insightsSqlUtils.
    }
}
