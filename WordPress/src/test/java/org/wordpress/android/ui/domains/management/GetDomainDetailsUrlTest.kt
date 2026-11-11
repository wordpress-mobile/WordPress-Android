package org.wordpress.android.ui.domains.management

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import uniffi.wp_api.DomainSubtypeId

@ExperimentalCoroutinesApi
class GetDomainDetailsUrlTest : BaseUnitTest() {
    @Test
    fun `WHEN a transfer domain is passed THEN a transfer detail url is generated`() {
        val domain = testDomainItem(
            domain = "transfer.domain",
            siteSlug = "transfer.slug",
            subtypeId = DomainSubtypeId.DomainTransfer,
        )
        val expected =
            "https://wordpress.com/domains/manage/all/transfer.domain/transfer/in/transfer.slug"
        assertThat(domain.getDomainDetailsUrl()).isEqualTo(expected)
    }

    @Test
    fun `WHEN a redirect domain is passed THEN a redirect detail url is generated`() {
        val domain = testDomainItem(
            domain = "redirect.domain",
            siteSlug = "redirect.domain.slug",
            subtypeId = DomainSubtypeId.SiteRedirect,
        )
        val expected =
            "https://wordpress.com/domains/manage/all/redirect.domain/redirect/redirect.domain.slug"
        assertThat(domain.getDomainDetailsUrl()).isEqualTo(expected)
    }

    @Test
    fun `WHEN a mapping domain is passed THEN the default detail url is generated`() {
        val domain = testDomainItem(
            domain = "some.domain",
            siteSlug = "domain.slug",
            subtypeId = DomainSubtypeId.DomainConnection,
        )
        val expected =
            "https://wordpress.com/domains/manage/all/some.domain/edit/domain.slug"
        assertThat(domain.getDomainDetailsUrl()).isEqualTo(expected)
    }

    @Test
    fun `WHEN the domain is empty THEN the detail url is null`() {
        val domain = testDomainItem(
            domain = "",
            siteSlug = "domain.slug",
        )
        assertThat(domain.getDomainDetailsUrl()).isNull()
    }

    @Test
    fun `WHEN the slug is empty THEN the detail url is null`() {
        val domain = testDomainItem(
            domain = "some.domain",
            siteSlug = "",
        )
        assertThat(domain.getDomainDetailsUrl()).isNull()
    }
}
