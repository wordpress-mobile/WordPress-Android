package org.wordpress.android.sharedlogin

import org.assertj.core.api.Assertions
import org.junit.Test

class WordPressPublicDataTest {
    private val classToTest = WordPressPublicData()

    @Test
    fun `Should return correct current package ID`() {
        val actual = classToTest.currentPackageId()
        val expected = "org.wordpress.android.prealpha"
        Assertions.assertThat(actual).isEqualTo(expected)
    }
}
