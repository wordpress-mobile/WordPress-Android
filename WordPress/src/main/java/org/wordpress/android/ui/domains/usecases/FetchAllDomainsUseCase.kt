package org.wordpress.android.ui.domains.usecases

import org.wordpress.android.fluxc.network.rest.wpcom.site.AllDomainsDomain
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

class FetchAllDomainsUseCase  @Inject constructor(
    private val siteStore: SiteStore,
) {
    suspend fun execute(): AllDomains {
        val result = siteStore.fetchAllDomains()
        return when {
            result.isError -> AllDomains.Error
            result.domains.isNullOrEmpty() -> return AllDomains.Empty
            else -> result.domains?.run {
                AllDomains.Success(this)
            } ?: AllDomains.Empty
        }
    }
}

sealed interface AllDomains {
    data class Success(
        val domains: List<AllDomainsDomain>,
    ) : AllDomains

    object Empty : AllDomains

    object Error : AllDomains
}
