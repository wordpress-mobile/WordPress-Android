package org.wordpress.android.util.extensions

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class MapExtensionsKtTest {
    @Test
    fun `Should filter null keys and values in filterNull`() {
        mapOf(0 to "0", null to "1", 2 to null, 3 to "3", null to null)
                .filterNull().forEach { (key, value) ->
                    assertThat(key).isNotNull
                    assertThat(value).isNotNull
                }
    }
}
