package org.wordpress.android.support

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures.Feature
import org.wordpress.android.util.config.NewOdieSupportFlowFeatureConfig

@RunWith(MockitoJUnitRunner::class)
class NewSupportFlowFeatureTest {
    @Mock
    private lateinit var experimentalFeatures: ExperimentalFeatures

    @Mock
    private lateinit var newOdieSupportFlowFeatureConfig: NewOdieSupportFlowFeatureConfig

    private lateinit var newSupportFlowFeature: NewSupportFlowFeature

    @Before
    fun setUp() {
        newSupportFlowFeature = NewSupportFlowFeature(experimentalFeatures, newOdieSupportFlowFeatureConfig)
    }

    @Test
    fun `given both flags disabled, when isEnabled, then returns false`() {
        whenever(experimentalFeatures.isEnabled(Feature.MODERN_SUPPORT)).thenReturn(false)
        whenever(newOdieSupportFlowFeatureConfig.isEnabled()).thenReturn(false)

        assertThat(newSupportFlowFeature.isEnabled()).isFalse()
    }

    @Test
    fun `given only the experimental flag is enabled, when isEnabled, then returns true`() {
        whenever(experimentalFeatures.isEnabled(Feature.MODERN_SUPPORT)).thenReturn(true)

        assertThat(newSupportFlowFeature.isEnabled()).isTrue()
    }

    @Test
    fun `given only the remote flag is enabled, when isEnabled, then returns true`() {
        whenever(experimentalFeatures.isEnabled(Feature.MODERN_SUPPORT)).thenReturn(false)
        whenever(newOdieSupportFlowFeatureConfig.isEnabled()).thenReturn(true)

        assertThat(newSupportFlowFeature.isEnabled()).isTrue()
    }
}
