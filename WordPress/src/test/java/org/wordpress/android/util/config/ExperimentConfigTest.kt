package org.wordpress.android.util.config

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.util.config.ExperimentConfig.Variant

@RunWith(MockitoJUnitRunner::class)
class ExperimentConfigTest {
    @Mock lateinit var appConfig: AppConfig
    private lateinit var experimentConfig: ExperimentConfig

    @Before
    fun setUp() {
        experimentConfig = TestExperimentConfig(appConfig)
    }

    @Test
    fun `returns variant from app config`() {
        val variant = Variant("remote_variant")
        whenever(appConfig.getCurrentVariant(experimentConfig)).thenReturn(variant)

        assertThat(experimentConfig.getVariant()).isEqualTo(variant)
    }

    private class TestExperimentConfig(appConfig: AppConfig) : ExperimentConfig(appConfig, "remote_field") {
        private val variantA = Variant("test_variant_a")
        override val variants: List<Variant>
            get() = listOf(variantA)
    }
}
