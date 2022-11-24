package org.wordpress.android.util.publicdata

import android.content.pm.PackageInfo
import android.content.pm.PackageManager.NameNotFoundException
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AppStatusTest {
    private val packageManagerWrapper: PackageManagerWrapper = mock()
    private val classToTest = AppStatus(packageManagerWrapper)

    @Test
    fun `Should get info from PackageManagerWrapper when isAppInstalled is called`() {
        val packageName = "package"
        classToTest.isAppInstalled(packageName)
        verify(packageManagerWrapper).getPackageInfo(packageName)
    }

    @Test
    fun `Should return TRUE if installed app package info IS found`() {
        val packageName = "package"
        whenever(packageManagerWrapper.getPackageInfo(packageName)).thenReturn(PackageInfo())
        val expected = true
        val actual = classToTest.isAppInstalled(packageName)
        assertEquals(expected, actual)
    }

    @Test
    fun `Should return FALSE if installed app package info IS NOT found`() {
        val packageName = "package"
        whenever(packageManagerWrapper.getPackageInfo(packageName))
                .doAnswer { throw NameNotFoundException() }
        val expected = false
        val actual = classToTest.isAppInstalled(packageName)
        assertEquals(expected, actual)
    }
}
