package org.wordpress.android.ui.publicize

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PublicizeServiceIconTest {
    @Test
    fun `given supported social service ID when calling fromServiceId then return expected value`() {
        // iterate and test all existing services supported by this enum
        PublicizeServiceIcon.values().forEach { expectedServiceIcon ->
            val icon = PublicizeServiceIcon.fromServiceId(expectedServiceIcon.serviceId)
            assertThat(icon).isEqualTo(expectedServiceIcon)
        }
    }

    @Test
    fun `given unsupported social service ID when calling fromServiceId then return null`() {
        val icon = PublicizeServiceIcon.fromServiceId("unsupported")
        assertThat(icon).isNull()
    }
}
