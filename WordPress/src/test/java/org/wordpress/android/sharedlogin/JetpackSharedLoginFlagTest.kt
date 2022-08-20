package org.wordpress.android.sharedlogin

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert
import org.junit.Test
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.JetpackSharedLoginFeatureConfig

class JetpackSharedLoginFlagTest {
    private val jetpackSharedLoginFeatureConfig: JetpackSharedLoginFeatureConfig = mock()
    private val buildConfigWrapper: BuildConfigWrapper = mock()
    private val classToTest = JetpackSharedLoginFlag(jetpackSharedLoginFeatureConfig, buildConfigWrapper)

    @Test
    fun `Should return isEnabled TRUE if flag is ENABLED and IS Jetpack app`() {
        whenever(jetpackSharedLoginFeatureConfig.isEnabled()).thenReturn(true)
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(true)
        val expected = true
        val actual = classToTest.isEnabled()
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `Should return isEnabled FALSE if flag is ENABLED and IS NOT Jetpack app`() {
        whenever(jetpackSharedLoginFeatureConfig.isEnabled()).thenReturn(true)
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(false)
        val expected = false
        val actual = classToTest.isEnabled()
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `Should return isEnabled FALSE if flag is DISABLED and IS Jetpack app`() {
        whenever(jetpackSharedLoginFeatureConfig.isEnabled()).thenReturn(false)
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(true)
        val expected = false
        val actual = classToTest.isEnabled()
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `Should return isEnabled FALSE if flag is DISABLED and IS NOT Jetpack app`() {
        whenever(jetpackSharedLoginFeatureConfig.isEnabled()).thenReturn(false)
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(false)
        val expected = false
        val actual = classToTest.isEnabled()
        Assert.assertEquals(expected, actual)
    }
}
