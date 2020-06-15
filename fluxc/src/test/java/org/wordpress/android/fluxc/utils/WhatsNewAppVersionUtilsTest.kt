package org.wordpress.android.fluxc.utils

import org.junit.Test
import kotlin.test.assertEquals

class WhatsNewAppVersionUtilsTest {
    @Test
    fun `normalizeVersion converts major dot minor dot patch into major dot minor`() {
        val inputVersion = "14.1.2"
        val expectedOutputVersion = "14.1"

        val result = WhatsNewAppVersionUtils.normalizeVersion(inputVersion)

        assertEquals(expectedOutputVersion, result)
    }

    @Test
    fun `normalizeVersion returns null from malformed version name`() {
        val inputVersion = "alpha-222"
        val expectedOutputVersion = null

        val result = WhatsNewAppVersionUtils.normalizeVersion(inputVersion)

        assertEquals(expectedOutputVersion, result)
    }

    @Test
    fun `versionNameToInt converts major dot minor dot patch into major minor int`() {
        val inputVersion = "14.1.2"
        val expectedOutputVersion = 141

        val result = WhatsNewAppVersionUtils.versionNameToInt(inputVersion)

        assertEquals(expectedOutputVersion, result)
    }

    @Test
    fun `versionNameToInt returns -1 from malformed version name`() {
        val inputVersion = "alpha-222"
        val expectedOutputVersion = -1

        val result = WhatsNewAppVersionUtils.versionNameToInt(inputVersion)

        assertEquals(expectedOutputVersion, result)
    }
}
