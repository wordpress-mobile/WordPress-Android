package org.wordpress.android.ui.domains.management

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.network.rest.wpcom.site.AllDomainsDomain
import org.wordpress.android.ui.domains.management.DomainManagementViewModel.UiState.PopulatedList

@ExperimentalCoroutinesApi
class PopulatedListTest : BaseUnitTest() {
    private lateinit var uiState: PopulatedList

    private val fakeDomainFoo = AllDomainsDomain(domain = "foo.com", siteSlug = "Foo Warehouse")
    private val fakeDomainBar = AllDomainsDomain(domain = "bar.com", siteSlug = "Chocolate Bar")
    private val fakeDomainBah = AllDomainsDomain(domain = "bah.com", siteSlug = "Black sheep")
    private val allDomains = listOf(fakeDomainFoo, fakeDomainBar, fakeDomainBah)


    @Before
    fun setUp() {
        uiState = PopulatedList.Loaded(domains = allDomains)
    }

    // PopulatedList

    @Test
    fun `PopulatedList filter matches correctly by domain`() {
        // Given
        val query = "foo"

        // When
        val result = uiState.filter(query)

        // Then
        assertThat(result).isEqualTo(PopulatedList.Loaded(listOf(fakeDomainFoo)))
    }

    @Test
    fun `PopulatedList filter matches correctly by site slug`() {
        // Given
        val query = "chocolate"

        // When
        val result = uiState.filter(query)

        // Then
        assertThat(result).isEqualTo(PopulatedList.Loaded(listOf(fakeDomainBar)))
    }

    @Test
    fun `PopulatedList filter matches all for the empty string`() {
        // Given
        val query = ""

        // When
        val result = uiState.filter(query)

        // Then
        assertThat(result).isEqualTo(PopulatedList.Loaded(domains = allDomains))
    }
}
