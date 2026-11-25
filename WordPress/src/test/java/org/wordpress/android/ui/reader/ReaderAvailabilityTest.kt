package org.wordpress.android.ui.reader

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.wordpress.android.BaseUnitTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.AppConfig.FeatureState
import org.wordpress.android.util.config.ReaderInWordPressAppFeatureConfig

@ExperimentalCoroutinesApi
class ReaderAvailabilityTest : BaseUnitTest() {
    @Mock
    lateinit var buildConfigWrapper: BuildConfigWrapper

    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var featureConfig: ReaderInWordPressAppFeatureConfig

    private lateinit var readerAvailability: ReaderAvailability

    @Before
    fun setUp() {
        readerAvailability = ReaderAvailability(buildConfigWrapper, accountStore, featureConfig)
    }

    @Test
    fun `available in the Jetpack app regardless of account and flag`() {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(true)

        assertThat(readerAvailability.isReaderAvailable()).isTrue
    }

    @Test
    fun `available in the WordPress app for a WordPress_com user with the flag on`() {
        setUpWordPressApp(isFlagEnabled = true, hasAccessToken = true)

        assertThat(readerAvailability.isReaderAvailable()).isTrue
    }

    @Test
    fun `unavailable in the WordPress app without a WordPress_com account`() {
        setUpWordPressApp(isFlagEnabled = true, hasAccessToken = false)

        assertThat(readerAvailability.isReaderAvailable()).isFalse
    }

    @Test
    fun `unavailable in the WordPress app when the flag is off`() {
        setUpWordPressApp(isFlagEnabled = false, hasAccessToken = true)

        assertThat(readerAvailability.isReaderAvailable()).isFalse
    }

    @Test
    fun `flag is read once while signed in, so a value fetched mid-session doesn't change availability`() {
        setUpWordPressApp(isFlagEnabled = false, hasAccessToken = true)
        readerAvailability.isReaderAvailable()

        assertThat(readerAvailability.isReaderAvailable()).isFalse
        verify(featureConfig, times(1)).featureState()
    }

    @Test
    fun `default value returned before the stored flags load is not kept`() {
        setUpWordPressApp(isFlagEnabled = false, hasAccessToken = true)
        whenever(featureConfig.featureState()).thenReturn(FeatureState.DefaultValue(false))
        readerAvailability.isReaderAvailable()

        whenever(featureConfig.featureState()).thenReturn(FeatureState.RemoteValue(true))

        assertThat(readerAvailability.isReaderAvailable()).isTrue
    }

    @Test
    fun `flag is re-read on the next sign-in after signing out`() {
        setUpWordPressApp(isFlagEnabled = false, hasAccessToken = true)
        readerAvailability.isReaderAvailable()

        whenever(accountStore.hasAccessToken()).thenReturn(false)
        readerAvailability.isReaderAvailable()
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(featureConfig.featureState()).thenReturn(FeatureState.RemoteValue(true))

        assertThat(readerAvailability.isReaderAvailable()).isTrue
    }

    @Test
    fun `refresh after sign-in replaces the kept value and reports the change`() {
        setUpWordPressApp(isFlagEnabled = false, hasAccessToken = true)
        readerAvailability.isReaderAvailable()
        whenever(featureConfig.featureState()).thenReturn(FeatureState.RemoteValue(true))

        readerAvailability.refreshFlagAfterSignIn()

        assertThat(readerAvailability.isReaderAvailable()).isTrue
        assertThat(readerAvailability.availabilityChanged.value?.getContentIfNotHandled()).isNotNull
    }

    @Test
    fun `refresh after sign-in that doesn't change the value reports nothing`() {
        setUpWordPressApp(isFlagEnabled = true, hasAccessToken = true)
        readerAvailability.isReaderAvailable()

        readerAvailability.refreshFlagAfterSignIn()

        assertThat(readerAvailability.availabilityChanged.value).isNull()
    }

    @Test
    fun `stored value loading after an uncached default reports the change`() {
        setUpWordPressApp(isFlagEnabled = false, hasAccessToken = true)
        whenever(featureConfig.featureState()).thenReturn(FeatureState.DefaultValue(false))
        readerAvailability.isReaderAvailable()

        whenever(featureConfig.featureState()).thenReturn(FeatureState.RemoteValue(true))

        assertThat(readerAvailability.isReaderAvailable()).isTrue
        assertThat(readerAvailability.availabilityChanged.value?.getContentIfNotHandled()).isNotNull
    }

    @Test
    fun `sign-in with the flag off reports nothing`() {
        setUpWordPressApp(isFlagEnabled = false, hasAccessToken = false)
        readerAvailability.isReaderAvailable()

        whenever(accountStore.hasAccessToken()).thenReturn(true)
        readerAvailability.refreshFlagAfterSignIn()

        assertThat(readerAvailability.isReaderAvailable()).isFalse
        assertThat(readerAvailability.availabilityChanged.value).isNull()
    }

    private fun setUpWordPressApp(isFlagEnabled: Boolean, hasAccessToken: Boolean) {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(false)
        // Not reached without a WordPress.com account
        lenient().whenever(featureConfig.featureState()).thenReturn(FeatureState.RemoteValue(isFlagEnabled))
        whenever(accountStore.hasAccessToken()).thenReturn(hasAccessToken)
    }
}
