package org.wordpress.android.ui.newstats

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.config.NewStatsFeatureConfig
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(MockitoJUnitRunner::class)
class NewStatsRoutingTest {
    @Mock
    lateinit var newStatsFeatureConfig: NewStatsFeatureConfig

    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    private lateinit var newStatsRouting: NewStatsRouting

    @Before
    fun setUp() {
        newStatsRouting = NewStatsRouting(newStatsFeatureConfig, appPrefsWrapper)
    }

    @Test
    fun `new stats is enabled when the remote flag is on`() {
        whenever(newStatsFeatureConfig.isEnabled()).thenReturn(true)

        assertTrue(newStatsRouting.isNewStatsEnabled())
    }

    @Test
    fun `new stats is enabled when the user opted in and the remote flag is off`() {
        whenever(newStatsFeatureConfig.isEnabled()).thenReturn(false)
        whenever(appPrefsWrapper.getNewStatsUserOptedIn()).thenReturn(true)

        assertTrue(newStatsRouting.isNewStatsEnabled())
    }

    @Test
    fun `new stats is disabled when the remote flag is off and the user has not opted in`() {
        whenever(newStatsFeatureConfig.isEnabled()).thenReturn(false)
        whenever(appPrefsWrapper.getNewStatsUserOptedIn()).thenReturn(false)

        assertFalse(newStatsRouting.isNewStatsEnabled())
    }

    @Test
    fun `opting out beats the remote flag`() {
        whenever(appPrefsWrapper.getNewStatsUserOptedOut()).thenReturn(true)

        assertFalse(newStatsRouting.isNewStatsEnabled())
    }

    /** Without this, a user who opted out could never get back into New Stats. */
    @Test
    fun `opting in clears a previous opt-out and re-arms the intro`() {
        newStatsRouting.optIn()

        verify(appPrefsWrapper).setNewStatsUserOptedOut(false)
        verify(appPrefsWrapper).setNewStatsIntroShown(false)
    }

    /** Re-arming it here would make the intro sheet reappear if New Stats is recreated. */
    @Test
    fun `opting out leaves the intro flag alone`() {
        newStatsRouting.optOut()

        verify(appPrefsWrapper, never()).setNewStatsIntroShown(any())
    }
}
