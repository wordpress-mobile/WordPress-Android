package org.wordpress.android.ui.prefs.taxonomies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.TaxonomyListParams
import javax.inject.Inject

class TaxonomiesNavMenuViewModel @Inject constructor(
    private val wpApiClientProvider: WpApiClientProvider,
    private val appLogWrapper: AppLogWrapper,
) : ViewModel() {

    fun fetchTaxonomies(site: SiteModel) {
        viewModelScope.launch {
            val client = wpApiClientProvider.getWpApiClient(site)
            val response = client.request { requestBuilder ->
                requestBuilder.taxonomies().listWithEditContext(TaxonomyListParams())
            }
            when (response) {
                is WpRequestResult.Success -> {
                    val list = response.response.data
                    appLogWrapper.d(AppLog.T.API, "Fetched taxonomies ${list.taxonomyTypes.size}")
                    list.taxonomyTypes.forEach { type ->
                        appLogWrapper.d(AppLog.T.POSTS, "Taxonomy ${type.value
                            .name}")
                    }
                }

                else -> {
                    appLogWrapper.e(AppLog.T.API, "Erro fetcing taxonomies")
                }
            }
        }
    }
}
