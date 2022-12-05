package org.wordpress.android.sharedlogin

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.util.publicdata.JetpackPublicData

class JetpackPublicDataTest {
    private val classToTest = JetpackPublicData()

    @Test
    fun `Should return correct release public hash key`() {
        val actual = classToTest.currentPublicKeyHash()
        val expected = "f2d7acc12614750009514a0932bf0b0aa9c11829a66e862ce4572bced344e76e"
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should return correct current package ID`() {
        val actual = classToTest.currentPackageId()
        val expected = "com.jetpack.android"
        assertThat(actual).isEqualTo(expected)
    }
}
