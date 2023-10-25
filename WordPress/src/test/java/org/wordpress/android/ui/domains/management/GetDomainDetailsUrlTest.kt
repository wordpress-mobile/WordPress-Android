package org.wordpress.android.ui.domains.management

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.network.rest.wpcom.site.AllDomainsDomain

@ExperimentalCoroutinesApi
class GetDomainDetailsUrlTest : BaseUnitTest() {
    @Test
    fun `WHEN a transfer domain is passed THEN a transfer detail url is generated`() {
        val domain = AllDomainsDomain(
            domain = "transfer.domain",
            siteSlug = "transfer.domain.slug",
            type = "transfer"
        )
        val expectedDetailUrl = "https://wordpress.com/domains/manage/all/transfer.domain/transfer/in/transfer.domain.slug"
        val actualDetailUrl = domain.getDomainDetailsUrl()
        assertThat(actualDetailUrl).isEqualTo(expectedDetailUrl)
    }

    @Test
    fun `WHEN a redirect domain is passed THEN a redirect detail url is generated`() {
        val domain = AllDomainsDomain(
            domain = "redirect.domain",
            siteSlug = "redirect.domain.slug",
            type = "redirect"
        )
        val expectedDetailUrl = "https://wordpress.com/domains/manage/all/redirect.domain/redirect/redirect.domain.slug"
        val actualDetailUrl = domain.getDomainDetailsUrl()
        assertThat(actualDetailUrl).isEqualTo(expectedDetailUrl)
    }

    @Test
    fun `WHEN a mapping domain is passed THEN the default detail url is generated`() {
        val domain = AllDomainsDomain(
            domain = "some.domain",
            siteSlug = "domain.slug",
            type = "mapping"
        )
        val expectedDetailUrl = "https://wordpress.com/domains/manage/all/some.domain/edit/domain.slug"
        val actualDetailUrl = domain.getDomainDetailsUrl()
        assertThat(actualDetailUrl).isEqualTo(expectedDetailUrl)
    }

    @Test
    fun `WHEN the domain is null THEN the default detail url is null`() {
        val domain = AllDomainsDomain(
            domain = null,
            siteSlug = "domain.slug",
            type = "mapping"
        )
        val actualDetailUrl = domain.getDomainDetailsUrl()
        assertThat(actualDetailUrl).isNull()
    }

    @Test
    fun `WHEN the slug is null THEN the default detail url is null`() {
        val domain = AllDomainsDomain(
            domain = "some.domain",
            siteSlug = null,
            type = "mapping"
        )
        val actualDetailUrl = domain.getDomainDetailsUrl()
        assertThat(actualDetailUrl).isNull()
    }
}
