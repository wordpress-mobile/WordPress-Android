package org.wordpress.android.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ValidationUtilsTest {
    private val validIpV4Addresses = listOf("192.168.0.1", "128.91.123.166", "121.242.149.74", "129.123.18.52",
            "233.71.46.32", "232.179.102.235", "98.117.195.114", "42.142.221.219", "66.237.30.198", "182.40.247.46",
            "146.112.111.130", "224.0.0.251", "224.0.0.252", "239.255.255.250", "255.255.255.25", "127.0.0.1")
    private val invalidIpV4Addresses = listOf("", "invalid-ip", "256.255.255.255", "0.0.0.256")

    @Test
    fun testValidIPv4Addresses() {
        validIpV4Addresses.forEach { assertThat(validateIPv4(it)).isEqualTo(true) }
    }

    @Test
    fun testInvalidIPv4Addresses() {
        invalidIpV4Addresses.forEach { assertThat(validateIPv4(it)).isEqualTo(false) }
    }
}
