package org.wordpress.android.ui.reader

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.ReaderInWordPressAppFeatureConfig

@RunWith(MockitoJUnitRunner::class)
class ReaderAvailabilityTest {
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
    fun `flag is read once, so a value fetched mid-session doesn't change availability`() {
        setUpWordPressApp(isFlagEnabled = false, hasAccessToken = true)
        readerAvailability.isReaderAvailable()

        whenever(featureConfig.isEnabled()).thenReturn(true)

        assertThat(readerAvailability.isReaderAvailable()).isFalse
        verify(featureConfig, times(1)).isEnabled()
    }

    @Test
    fun `flag is re-read after a sign-in`() {
        setUpWordPressApp(isFlagEnabled = false, hasAccessToken = true)
        readerAvailability.isReaderAvailable()

        whenever(featureConfig.isEnabled()).thenReturn(true)
        readerAvailability.refreshFlagAfterSignIn()

        assertThat(readerAvailability.isReaderAvailable()).isTrue
    }

    private fun setUpWordPressApp(isFlagEnabled: Boolean, hasAccessToken: Boolean) {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(false)
        whenever(featureConfig.isEnabled()).thenReturn(isFlagEnabled)
        // Not reached when the flag is off
        lenient().whenever(accountStore.hasAccessToken()).thenReturn(hasAccessToken)
    }
}
