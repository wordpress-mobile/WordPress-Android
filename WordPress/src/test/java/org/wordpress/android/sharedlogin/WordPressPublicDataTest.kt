package org.wordpress.android.sharedlogin

import android.content.pm.PackageInfo
import org.assertj.core.api.Assertions
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.util.publicdata.PackageManagerWrapper
import org.wordpress.android.util.publicdata.WordPressPublicData

class WordPressPublicDataTest {
    private val packageManagerWrapper: PackageManagerWrapper = mock()

    private val classToTest = WordPressPublicData(packageManagerWrapper)

    @Test
    fun `Should return correct current package ID`() {
        val actual = classToTest.currentPackageId()
        val expected = "org.wordpress.android"
        Assertions.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should return correct current package version`() {
        mockVersion("21.2-rc-3")
        val actual = classToTest.currentPackageVersion()
        val expected = "21.2-rc-3"
        Assertions.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Versions without semantic information should be equal to the non semantic version`() {
        mockVersion("21.2")
        val actual = classToTest.nonSemanticPackageVersion()
        val expected = "21.2"
        Assertions.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Release candidate versions should be stripped from the non semantic version`() {
        mockVersion("21.2-rc-3")
        val actual = classToTest.nonSemanticPackageVersion()
        val expected = "21.2"
        Assertions.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Alpha versions should be stripped from the non semantic version`() {
        mockVersion("21.2-alpha-3")
        val actual = classToTest.nonSemanticPackageVersion()
        val expected = "21.2"
        Assertions.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Invalid versions should return a null non semantic version`() {
        mockVersion("21.2...-rc2")
        val actual = classToTest.nonSemanticPackageVersion()
        val expected = null
        Assertions.assertThat(actual).isEqualTo(expected)
    }

    private fun mockVersion(version: String) {
        val packageInfo = PackageInfo().apply { versionName = version }
        whenever(packageManagerWrapper.getPackageInfo(any(), any())).thenReturn(packageInfo)
    }
}
