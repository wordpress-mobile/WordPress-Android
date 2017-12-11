package org.wordpress.android.fluxc

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class WCWPAPIEndpointTest {
    @Test
    fun testAllEndpoints() {
        // Orders
        assertEquals("/orders/", WOOCOMMERCE.orders.endpoint)
    }

    @Test
    fun testAllUrls() {
        // Orders
        assertEquals("/wc/v2/orders/", WOOCOMMERCE.orders.pathV2)
    }
}
