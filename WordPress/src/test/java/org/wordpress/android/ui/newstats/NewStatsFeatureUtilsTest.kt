package org.wordpress.android.ui.newstats

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.config.NewStatsFeatureConfig

@RunWith(MockitoJUnitRunner::class)
class NewStatsFeatureUtilsTest {
    @Mock
    lateinit var newStatsFeatureConfig: NewStatsFeatureConfig

    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    private lateinit var newStatsFeatureUtils: NewStatsFeatureUtils

    @Before
    fun setUp() {
        newStatsFeatureUtils = NewStatsFeatureUtils(newStatsFeatureConfig, appPrefsWrapper)
    }

    @Test
    fun `given remote flag is on, then new stats is enabled`() {
        whenever(newStatsFeatureConfig.isEnabled()).thenReturn(true)

        assertThat(newStatsFeatureUtils.isNewStatsEnabled()).isTrue()
    }

    @Test
    fun `given remote flag is off and user opted in, then new stats is enabled`() {
        whenever(newStatsFeatureConfig.isEnabled()).thenReturn(false)
        whenever(appPrefsWrapper.getNewStatsUserOptedIn()).thenReturn(true)

        assertThat(newStatsFeatureUtils.isNewStatsEnabled()).isTrue()
    }

    @Test
    fun `given remote flag is off and user has not opted in, then new stats is disabled`() {
        whenever(newStatsFeatureConfig.isEnabled()).thenReturn(false)
        whenever(appPrefsWrapper.getNewStatsUserOptedIn()).thenReturn(false)

        assertThat(newStatsFeatureUtils.isNewStatsEnabled()).isFalse()
    }
}
