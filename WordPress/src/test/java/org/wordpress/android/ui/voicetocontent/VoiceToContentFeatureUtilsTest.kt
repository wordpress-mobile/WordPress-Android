package org.wordpress.android.ui.voicetocontent

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.VoiceToContentFeatureConfig

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class VoiceToContentFeatureUtilsTest {
    @Mock
    lateinit var buildConfigWrapper: BuildConfigWrapper

    @Mock
    lateinit var voiceToContentFeatureConfig: VoiceToContentFeatureConfig

    private lateinit var utils: VoiceToContentFeatureUtils

    @Before
    fun setup() {
        utils = VoiceToContentFeatureUtils(buildConfigWrapper, voiceToContentFeatureConfig)
    }

    @Test
    fun `when buildConfigWrapper and featureConfig are enabled then returns true`() {
        // Arrange
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(true)
        whenever(voiceToContentFeatureConfig.isEnabled()).thenReturn(true)

        // Act
        val result = utils.isVoiceToContentEnabled()

        // Assert
        assertEquals(true, result)
    }

    @Test
    fun `when buildConfigWrapper is disabled then returns false `() {
        // Arrange
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(false)

        // Act
        val result = utils.isVoiceToContentEnabled()

        // Assert
        assertEquals(false, result)
    }

    @Test
    fun `when voiceToContentFeatureConfig is disabled then returns false `() {
        // Arrange
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(true)
        whenever(voiceToContentFeatureConfig.isEnabled()).thenReturn(false)

        // Act
        val result = utils.isVoiceToContentEnabled()

        // Assert
        assertEquals(false, result)
    }
}
