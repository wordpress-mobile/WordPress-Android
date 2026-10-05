package org.wordpress.android.ui.newstats.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.ui.newstats.StatsPeriod
import java.time.LocalDate

class StatsResultCacheTest {
    private lateinit var cache: StatsResultCache

    @Before
    fun setUp() {
        cache = StatsResultCache()
    }

    @Test
    fun `given nothing stored, when get, then null is returned`() {
        assertThat(cache.get<String>(key(StatsPeriod.Last7Days))).isNull()
        assertThat(cache.isCached(key(StatsPeriod.Last7Days))).isFalse()
    }

    @Test
    fun `given a stored value, when get, then the same instance is returned`() {
        val value = listOf("a", "b")

        cache.put(key(StatsPeriod.Last7Days), value)

        assertThat(cache.get<List<String>>(key(StatsPeriod.Last7Days))).isSameAs(value)
        assertThat(cache.isCached(key(StatsPeriod.Last7Days))).isTrue()
    }

    @Test
    fun `given a freshly stored value, when needsRevalidation, then it is false`() {
        cache.put(key(StatsPeriod.Last7Days), "value")

        assertThat(cache.needsRevalidation(key(StatsPeriod.Last7Days))).isFalse()
    }

    @Test
    fun `given markAllStale, then the value is still served but needs revalidation`() {
        cache.put(key(StatsPeriod.Last7Days), "value")

        cache.markAllStale()

        assertThat(cache.get<String>(key(StatsPeriod.Last7Days))).isEqualTo("value")
        assertThat(cache.needsRevalidation(key(StatsPeriod.Last7Days))).isTrue()
    }

    @Test
    fun `given a stale entry, when it is stored again, then it no longer needs revalidation`() {
        cache.put(key(StatsPeriod.Last7Days), "stale")
        cache.markAllStale()

        cache.put(key(StatsPeriod.Last7Days), "fresh")

        assertThat(cache.needsRevalidation(key(StatsPeriod.Last7Days))).isFalse()
        assertThat(cache.get<String>(key(StatsPeriod.Last7Days))).isEqualTo("fresh")
    }

    @Test
    fun `given nothing stored, when needsRevalidation, then it is false`() {
        assertThat(cache.needsRevalidation(key(StatsPeriod.Last7Days))).isFalse()
    }

    @Test
    fun `given a full bucket, when one more is stored, then the eldest is evicted`() {
        val periods = (1..StatsCacheBucket.CLICKS.capacity + 1).map { customPeriod(it) }

        periods.forEach { cache.put(key(it), "value") }

        assertThat(cache.isCached(key(periods.first()))).isFalse()
        assertThat(cache.isCached(key(periods.last()))).isTrue()
    }

    @Test
    fun `given a full bucket, when an entry is read, then a later eviction spares it`() {
        val periods = (1..StatsCacheBucket.CLICKS.capacity).map { customPeriod(it) }
        periods.forEach { cache.put(key(it), "value") }

        // Reading the eldest entry makes it the most recently used, so the next insertion must
        // evict the one after it instead.
        cache.get<String>(key(periods.first()))
        cache.put(key(customPeriod(99)), "value")

        assertThat(cache.isCached(key(periods.first()))).isTrue()
        assertThat(cache.isCached(key(periods[1]))).isFalse()
    }

    @Test
    fun `given one bucket overflows, then another bucket keeps its entries`() {
        cache.put(key(StatsPeriod.Last7Days, StatsCacheBucket.CHART), "chart")

        (1..StatsCacheBucket.CLICKS.capacity + 5).forEach {
            cache.put(key(customPeriod(it)), "value")
        }

        assertThat(cache.get<String>(key(StatsPeriod.Last7Days, StatsCacheBucket.CHART)))
            .isEqualTo("chart")
    }

    @Test
    fun `given stored values, when clear, then every bucket is emptied`() {
        cache.put(key(StatsPeriod.Last7Days), "clicks")
        cache.put(key(StatsPeriod.Last7Days, StatsCacheBucket.CHART), "chart")

        cache.clear()

        assertThat(cache.isCached(key(StatsPeriod.Last7Days))).isFalse()
        assertThat(cache.isCached(key(StatsPeriod.Last7Days, StatsCacheBucket.CHART))).isFalse()
    }

    @Test
    fun `given a stored value, then a different site, period, day or variant does not match it`() {
        cache.put(key(StatsPeriod.Last7Days), "value")

        assertThat(cache.isCached(key(StatsPeriod.Last7Days).copy(siteId = OTHER_SITE_ID))).isFalse()
        assertThat(cache.isCached(key(StatsPeriod.Last30Days))).isFalse()
        assertThat(cache.isCached(key(StatsPeriod.Last7Days).copy(today = TODAY.plusDays(1)))).isFalse()
        assertThat(cache.isCached(key(StatsPeriod.Last7Days).copy(variant = "other"))).isFalse()
    }

    private fun key(
        period: StatsPeriod,
        bucket: StatsCacheBucket = StatsCacheBucket.CLICKS
    ) = StatsCacheKey(bucket, SITE_ID, TODAY, period)

    private fun customPeriod(dayOfMonth: Int) = StatsPeriod.Custom(
        LocalDate.of(2026, 1, 1).plusDays(dayOfMonth.toLong()),
        LocalDate.of(2026, 2, 1).plusDays(dayOfMonth.toLong())
    )

    companion object {
        private const val SITE_ID = 123L
        private const val OTHER_SITE_ID = 456L
        private val TODAY = LocalDate.of(2026, 10, 5)
    }
}
