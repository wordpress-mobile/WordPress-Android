package org.wordpress.android.fluxc.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.JsonLoaderUtils.jsonFileAs
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemotePriceType.FlatFee
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemoteRestrictionsType.AnyText
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemoteType.Checkbox
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductApiResponse
import kotlin.test.fail

class WCProductModelTest {
    @Test
    fun `Product addons should be serialized correctly`() {
        val productModelUnderTest =
                "wc/product-with-addons.json"
                .jsonFileAs(ProductApiResponse::class.java)
                ?.asProductModel()

        assertThat(productModelUnderTest).isNotNull
        assertThat(productModelUnderTest?.addons).isNotEmpty
        assertThat(productModelUnderTest?.addons?.size).isEqualTo(3)
    }

    @Test
    fun `Product addons should be serialized with enum values correctly`() {
        val productModelUnderTest =
                "wc/product-with-addons.json"
                        .jsonFileAs(ProductApiResponse::class.java)
                        ?.asProductModel()

        assertThat(productModelUnderTest).isNotNull
        assertThat(productModelUnderTest?.addons).isNotEmpty

        productModelUnderTest?.addons?.first()?.let {
            assertThat(it.priceType).isEqualTo(FlatFee)
            assertThat(it.restrictionsType).isEqualTo(AnyText)
            assertThat(it.type).isEqualTo(Checkbox)
        } ?: fail("Addons list shouldn't be empty")
    }

    @Test
    fun `Product addons should contain Addon options serialized correctly`() {
        val addonOptions = "wc/product-with-addons.json"
                .jsonFileAs(ProductApiResponse::class.java)
                ?.asProductModel()
                ?.addons
                ?.takeIf { it.isNotEmpty() }
                ?.first()
                ?.options

        assertThat(addonOptions).isNotNull
        assertThat(addonOptions).isNotEmpty
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
    fun `Product addons with incorrect key should be null`() {
        val productModelUnderTest =
                "wc/product-with-incorrect-addons-key.json"
                        .jsonFileAs(ProductApiResponse::class.java)
                        ?.asProductModel()

        assertThat(productModelUnderTest).isNotNull
        assertThat(productModelUnderTest?.metadata).isNotNull
        assertThat(productModelUnderTest?.addons).isNull()
    }
}
