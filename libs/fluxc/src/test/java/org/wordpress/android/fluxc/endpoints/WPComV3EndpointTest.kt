package org.wordpress.android.fluxc.endpoints

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV3

class WPComV3EndpointTest {
    @Test
    fun testAllEndpoints() {
        assertThat("/sites/123/blogging-prompts/")
            .isEqualTo(WPCOMV3.sites.site(123).blogging_prompts.endpoint)
    }

    @Test
    fun testUrls() {
        assertThat("https://public-api.wordpress.com/wpcom/v3/sites/123/blogging-prompts/")
            .isEqualTo(WPCOMV3.sites.site(123).blogging_prompts.url)
    }
}
