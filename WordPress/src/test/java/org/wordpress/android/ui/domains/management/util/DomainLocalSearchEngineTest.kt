package org.wordpress.android.ui.domains.management.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.network.rest.wpcom.site.AllDomainsDomain
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainStatus

class DomainLocalSearchEngineTest {
    private val fakeDomainFoo = AllDomainsDomain(
        domain = "foo.com",
        siteSlug = "Foo Warehouse",
        blogName = "Awesome blog",
        domainStatus = DomainStatus(status = "Active")
    )
    private val fakeDomainBar = AllDomainsDomain(
        domain = "bar.com",
        siteSlug = "Chocolate Bar",
        blogName = "Sweet blog",
        domainStatus = DomainStatus(status = "Activating")
    )
    private val fakeDomainBah = AllDomainsDomain(
        domain = "bah.com",
        siteSlug = "Black sheep",
        blogName = "Unique blog",
        domainStatus = DomainStatus(status = "Expired")
    )
    private val allDomains = listOf(fakeDomainFoo, fakeDomainBar, fakeDomainBah)

    private val engine = DomainLocalSearchEngine()

    @Test
    fun `PopulatedList filter matches correctly by domain`() {
        // Given
        val query = "foo"

        // When
        val result = engine.filter(allDomains, query)

        // Then
        assertThat(result).isEqualTo(listOf(fakeDomainFoo))
    }

    @Test
    fun `PopulatedList filter matches correctly by site slug`() {
        // Given
        val query = "chocolate"

        // When
        val result = engine.filter(allDomains, query)

        // Then
        assertThat(result).isEqualTo(listOf(fakeDomainBar))
    }

    @Test
    fun `PopulatedList filter matches correctly by domain status`() {
        // Given
        val query = "active"

        // When
        val result = engine.filter(allDomains, query)

        // Then
        assertThat(result).isEqualTo(listOf(fakeDomainFoo))
    }

    @Test
    fun `PopulatedList filter matches correctly by blog name`() {
        // Given
        val query = "unique"

        // When
        val result = engine.filter(allDomains, query)

        // Then
        assertThat(result).isEqualTo(listOf(fakeDomainBah))
    }

    @Test
    fun `PopulatedList filter matches all for the empty string`() {
        // Given
        val query = ""

        // When
        val result = engine.filter(allDomains, query)

        // Then
        assertThat(result).isEqualTo(allDomains)
    }
}
