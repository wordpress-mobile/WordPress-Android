package org.wordpress.android.fluxc.persistence

import org.junit.Test
import org.wordpress.android.fluxc.persistence.FeatureFlagConfigDao.FeatureFlagValueSource
import org.wordpress.android.fluxc.persistence.FeatureFlagConfigDao.FeatureFlagValueSourceConverter
import kotlin.test.assertEquals

class FeatureFlagValueSourceConverterTest {
    private val converter = FeatureFlagValueSourceConverter()

    @Test
    fun `toFeatureFlagValueSource returns BUILD_CONFIG for ordinal 0`() {
        assertEquals(FeatureFlagValueSource.BUILD_CONFIG, converter.toFeatureFlagValueSource(0))
    }

    @Test
    fun `toFeatureFlagValueSource returns REMOTE for ordinal 1`() {
        assertEquals(FeatureFlagValueSource.REMOTE, converter.toFeatureFlagValueSource(1))
    }

    @Test
    fun `toFeatureFlagValueSource falls back to BUILD_CONFIG for out-of-range ordinal`() {
        assertEquals(FeatureFlagValueSource.BUILD_CONFIG, converter.toFeatureFlagValueSource(99))
    }

    @Test
    fun `toFeatureFlagValueSource falls back to BUILD_CONFIG for negative ordinal`() {
        assertEquals(FeatureFlagValueSource.BUILD_CONFIG, converter.toFeatureFlagValueSource(-1))
    }
}
