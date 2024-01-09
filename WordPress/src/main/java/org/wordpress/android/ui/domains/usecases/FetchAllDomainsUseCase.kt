package org.wordpress.android.ui.domains.usecases

import org.wordpress.android.fluxc.network.rest.wpcom.site.AllDomainsDomain
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.util.AppLog
import javax.inject.Inject

class FetchAllDomainsUseCase  @Inject constructor(
    private val siteStore: SiteStore,
) {
    suspend fun execute(): AllDomains {
        val result = siteStore.fetchAllDomains()
        return when {
            result.isError -> {
                AppLog.e(AppLog.T.API, "An error occurred while fetching all domains: ${result.error.message}")
                AllDomains.Error
            }

            result.domains.isNullOrEmpty() -> AllDomains.Empty
            else -> AllDomains.Success(requireNotNull(result.domains))
        }
    }
}

sealed interface AllDomains {
    data class Success(
        val domains: List<AllDomainsDomain>,
    ) : AllDomains

    data object Empty : AllDomains

    data object Error : AllDomains
}
