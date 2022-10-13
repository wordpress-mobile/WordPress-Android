package org.wordpress.android.userflags

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert
import org.junit.Test
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.JetpackLocalUserFlagsFeatureConfig

class JetpackLocalUserFlagsFlagTest {
    private val jetpackLocalUserFlagsFeatureConfig: JetpackLocalUserFlagsFeatureConfig = mock()
    private val buildConfigWrapper: BuildConfigWrapper = mock()
    private val classToTest = JetpackLocalUserFlagsFlag(jetpackLocalUserFlagsFeatureConfig, buildConfigWrapper)

    @Test
    fun `Should return isEnabled TRUE if flag is ENABLED and IS Jetpack app`() {
        whenever(jetpackLocalUserFlagsFeatureConfig.isEnabled()).thenReturn(true)
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(true)
        val expected = true
        val actual = classToTest.isEnabled()
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `Should return isEnabled FALSE if flag is ENABLED and IS NOT Jetpack app`() {
        whenever(jetpackLocalUserFlagsFeatureConfig.isEnabled()).thenReturn(true)
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(false)
        val expected = false
        val actual = classToTest.isEnabled()
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `Should return isEnabled FALSE if flag is DISABLED and IS Jetpack app`() {
        whenever(jetpackLocalUserFlagsFeatureConfig.isEnabled()).thenReturn(false)
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(true)
        val expected = false
        val actual = classToTest.isEnabled()
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `Should return isEnabled FALSE if flag is DISABLED and IS NOT Jetpack app`() {
        whenever(jetpackLocalUserFlagsFeatureConfig.isEnabled()).thenReturn(false)
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(false)
        val expected = false
        val actual = classToTest.isEnabled()
        Assert.assertEquals(expected, actual)
    }
}
