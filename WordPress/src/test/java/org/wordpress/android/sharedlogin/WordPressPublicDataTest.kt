package org.wordpress.android.sharedlogin

import android.content.pm.PackageInfo
import org.assertj.core.api.Assertions
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BuildConfig
import org.wordpress.android.util.publicdata.PackageManagerWrapper
import org.wordpress.android.util.publicdata.WordPressPublicData

class WordPressPublicDataTest {
    private val packageManagerWrapper: PackageManagerWrapper = mock()
    private val packageInfo: PackageInfo = mock()

    private val classToTest = WordPressPublicData(packageManagerWrapper)

    @Test
    fun `Should return correct current package ID`() {
        val actual = classToTest.currentPackageId()
        val expected = "org.wordpress.android"
        Assertions.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should return correct current package version`() {
        val actual = classToTest.currentPackageVersion()
        val expected = BuildConfig.VERSION_NAME
        Assertions.assertThat(actual).isEqualTo(expected)
    }
}
