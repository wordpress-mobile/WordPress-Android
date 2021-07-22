package org.wordpress.android.fluxc.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.JsonLoader.Companion.jsonFileAs
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductApiResponse

class WCProductModelTest {
    @Test
    fun `Product addons should be serialized correctly`() {
        val productModelUnderTest =
                "wc/product-with-addons.json"
                .jsonFileAs(ProductApiResponse::class.java)
                ?.asProductModel()

        assertThat(productModelUnderTest).isNotNull
        assertThat(productModelUnderTest?.addons).isNotEmpty
    }

    @Test
    fun `Product metadata is serialized correctly`() {
        val productModelUnderTest =
                "wc/product-with-addons.json"
                        .jsonFileAs(ProductApiResponse::class.java)
                        ?.asProductModel()

        assertThat(productModelUnderTest).isNotNull
        assertThat(productModelUnderTest?.metadata).isNotNull
    }

    @Test
    fun `Product addons with incorrect key should return empty array`() {
        val productModelUnderTest =
                "wc/product-with-incorrect-addons-key.json"
                        .jsonFileAs(ProductApiResponse::class.java)
                        ?.asProductModel()

        assertThat(productModelUnderTest).isNotNull
        assertThat(productModelUnderTest?.addons).isNotNull
        assertThat(productModelUnderTest?.addons).isEmpty()
    }
}

