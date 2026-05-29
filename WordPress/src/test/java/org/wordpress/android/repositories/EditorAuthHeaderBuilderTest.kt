package org.wordpress.android.repositories

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel

@RunWith(MockitoJUnitRunner::class)
class EditorAuthHeaderBuilderTest {
    private val builder = EditorAuthHeaderBuilder()

    @Test
    fun `WPCom site returns Bearer token header`() {
        val header = builder.build(
            shouldUseWPComRestApi = true,
            accessToken = "my_token",
            username = null,
            password = null
        )

        assertThat(header).isEqualTo("Bearer my_token")
    }

    @Test
    fun `WPCom site with null token returns null`() {
        val header = builder.build(
            shouldUseWPComRestApi = true,
            accessToken = null,
            username = null,
            password = null
        )

        assertThat(header).isNull()
    }

    @Test
    fun `WPCom site with empty token returns null`() {
        val header = builder.build(
            shouldUseWPComRestApi = true,
            accessToken = "",
            username = null,
            password = null
        )

        assertThat(header).isNull()
    }

    @Test
    fun `self-hosted site returns Basic auth header`() {
        val header = builder.build(
            shouldUseWPComRestApi = false,
            accessToken = null,
            username = "testuser",
            password = "testpass"
        )

        assertThat(header).isNotNull()
        assertThat(header).startsWith("Basic ")
    }

    @Test
    fun `Basic auth with null username returns null`() {
        val header = builder.build(
            shouldUseWPComRestApi = false,
            accessToken = null,
            username = null,
            password = "password123"
        )

        assertThat(header).isNull()
    }

    @Test
    fun `Basic auth with empty username returns null`() {
        val header = builder.build(
            shouldUseWPComRestApi = false,
            accessToken = null,
            username = "",
            password = "password123"
        )

        assertThat(header).isNull()
    }

    @Test
    fun `Basic auth with null password returns null`() {
        val header = builder.build(
            shouldUseWPComRestApi = false,
            accessToken = null,
            username = "username",
            password = null
        )

        assertThat(header).isNull()
    }

    @Test
    fun `Basic auth with empty password returns null`() {
        val header = builder.build(
            shouldUseWPComRestApi = false,
            accessToken = null,
            username = "username",
            password = ""
        )

        assertThat(header).isNull()
    }

    @Test
    fun `Basic auth with both empty returns null`() {
        val header = builder.build(
            shouldUseWPComRestApi = false,
            accessToken = null,
            username = "",
            password = ""
        )

        assertThat(header).isNull()
    }

    @Test
    fun `special characters in Basic auth are encoded`() {
        val header = builder.build(
            shouldUseWPComRestApi = false,
            accessToken = null,
            username = "user@example.com",
            password = "p@ss:word!123"
        )

        assertThat(header).isNotNull()
        assertThat(header).startsWith("Basic ")
    }

    // ===== Site-convenience overload =====

    @Test
    fun `site overload uses Bearer for WPCom-routed sites without app password`() {
        val site = SiteModel().apply {
            setIsWPCom(true)
            setIsJetpackConnected(false)
            origin = SiteModel.ORIGIN_WPCOM_REST
        }

        val header = builder.build(site, accessToken = "wpcom_token")

        assertThat(header).isEqualTo("Bearer wpcom_token")
    }

    @Test
    fun `site overload uses Basic when application password is set`() {
        val site = SiteModel().apply {
            setIsWPCom(false)
            setIsJetpackConnected(true)
            origin = SiteModel.ORIGIN_WPCOM_REST
            apiRestUsernamePlain = "admin"
            apiRestPasswordPlain = "app_pass"
        }

        val header = builder.build(site, accessToken = "ignored")

        assertThat(header).startsWith("Basic ")
    }

    @Test
    fun `site overload prefers Basic over Bearer when app password is set on a WPCom-routed site`() {
        val site = SiteModel().apply {
            setIsWPCom(false)
            setIsJetpackConnected(true)
            origin = SiteModel.ORIGIN_WPCOM_REST
            apiRestUsernamePlain = "admin"
            apiRestPasswordPlain = "app_pass"
        }

        val header = builder.build(site, accessToken = "wpcom_token")

        assertThat(header).startsWith("Basic ")
    }
}
