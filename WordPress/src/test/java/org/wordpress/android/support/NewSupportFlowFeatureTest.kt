package org.wordpress.android.support

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures.Feature
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.NewMobileSupportFlowFeatureConfig

@RunWith(MockitoJUnitRunner::class)
class NewSupportFlowFeatureTest {
    @Mock
    private lateinit var experimentalFeatures: ExperimentalFeatures

    @Mock
    private lateinit var newMobileSupportFlowFeatureConfig: NewMobileSupportFlowFeatureConfig

    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var buildConfigWrapper: BuildConfigWrapper

    private lateinit var newSupportFlowFeature: NewSupportFlowFeature

    @Before
    fun setUp() {
        newSupportFlowFeature = NewSupportFlowFeature(
            experimentalFeatures,
            newMobileSupportFlowFeatureConfig,
            accountStore,
            buildConfigWrapper
        )
    }

    @Test
    fun `given both flags disabled, when isEnabled, then returns false`() {
        whenever(experimentalFeatures.isEnabled(Feature.MODERN_SUPPORT)).thenReturn(false)
        whenever(newMobileSupportFlowFeatureConfig.isEnabled()).thenReturn(false)

        assertThat(newSupportFlowFeature.isEnabled()).isFalse()
    }

    /**
     * The experimental toggle is not restricted by app or login state, so it stays enabled with the
     * remaining mocks returning false (WordPress app, no WordPress.com token).
     */
    @Test
    fun `given only the experimental flag is enabled, when isEnabled, then returns true`() {
        whenever(experimentalFeatures.isEnabled(Feature.MODERN_SUPPORT)).thenReturn(true)

        assertThat(newSupportFlowFeature.isEnabled()).isTrue()
    }

    @Test
    fun `given the remote flag is enabled on Jetpack for a logged in user, when isEnabled, then returns true`() {
        whenever(experimentalFeatures.isEnabled(Feature.MODERN_SUPPORT)).thenReturn(false)
        whenever(newMobileSupportFlowFeatureConfig.isEnabled()).thenReturn(true)
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(true)
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        assertThat(newSupportFlowFeature.isEnabled()).isTrue()
    }

    @Test
    fun `given the remote flag is enabled without a WordPress com token, when isEnabled, then returns false`() {
        whenever(experimentalFeatures.isEnabled(Feature.MODERN_SUPPORT)).thenReturn(false)
        whenever(newMobileSupportFlowFeatureConfig.isEnabled()).thenReturn(true)
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(true)
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        assertThat(newSupportFlowFeature.isEnabled()).isFalse()
    }

    @Test
    fun `given the remote flag is enabled on the WordPress app, when isEnabled, then returns false`() {
        whenever(experimentalFeatures.isEnabled(Feature.MODERN_SUPPORT)).thenReturn(false)
        whenever(newMobileSupportFlowFeatureConfig.isEnabled()).thenReturn(true)
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(false)

        assertThat(newSupportFlowFeature.isEnabled()).isFalse()
    }
}
